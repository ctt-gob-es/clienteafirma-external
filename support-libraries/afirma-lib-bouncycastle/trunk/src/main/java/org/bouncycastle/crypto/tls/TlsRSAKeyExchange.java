package org.bouncycastle.crypto.tls;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.util.io.Streams;

/**
 * TLS 1.0/1.1 and SSLv3 RSA key exchange.
 */
public class TlsRSAKeyExchange
    extends AbstractTlsKeyExchange
{
    protected AsymmetricKeyParameter serverPublicKey = null;

    protected RSAKeyParameters rsaServerPublicKey = null;

    protected TlsEncryptionCredentials serverCredentials = null;

    protected byte[] premasterSecret;

    public TlsRSAKeyExchange(Vector supportedSignatureAlgorithms)
    {
        super(KeyExchangeAlgorithm.RSA, supportedSignatureAlgorithms);
    }

    public void skipServerCredentials()
        throws IOException
    {
        throw new TlsFatalAlert(AlertDescription.unexpected_message);
    }

    public void processServerCredentials(TlsCredentials serverCredentials)
        throws IOException
    {

        if (!(serverCredentials instanceof TlsEncryptionCredentials))
        {
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }

        processServerCertificate(serverCredentials.getCertificate());

        this.serverCredentials = (TlsEncryptionCredentials)serverCredentials;
    }

    public void processServerCertificate(Certificate serverCertificate)
        throws IOException
    {

        if (serverCertificate.isEmpty())
        {
            throw new TlsFatalAlert(AlertDescription.bad_certificate);
        }

        org.bouncycastle.asn1.x509.Certificate x509Cert = serverCertificate.getCertificateAt(0);

        SubjectPublicKeyInfo keyInfo = x509Cert.getSubjectPublicKeyInfo();
        try
        {
            this.serverPublicKey = PublicKeyFactory.createKey(keyInfo);
        }
        catch (RuntimeException e)
        {
            throw new TlsFatalAlert(AlertDescription.unsupported_certificate);
        }

        // Sanity check the PublicKeyFactory
        if (this.serverPublicKey.isPrivate())
        {
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }

        this.rsaServerPublicKey = validateRSAPublicKey((RSAKeyParameters)this.serverPublicKey);

        TlsUtils.validateKeyUsage(x509Cert, KeyUsage.keyEncipherment);

        super.processServerCertificate(serverCertificate);
    }

    public void validateCertificateRequest(CertificateRequest certificateRequest)
        throws IOException
    {
        short[] types = certificateRequest.getCertificateTypes();
        for (int i = 0; i < types.length; ++i)
        {
            switch (types[i])
            {
            case ClientCertificateType.rsa_sign:
            case ClientCertificateType.dss_sign:
            case ClientCertificateType.ecdsa_sign:
                break;
            default:
                throw new TlsFatalAlert(AlertDescription.illegal_parameter);
            }
        }
    }

    public void processClientCredentials(TlsCredentials clientCredentials)
        throws IOException
    {
        if (!(clientCredentials instanceof TlsSignerCredentials))
        {
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }
    }

    public void generateClientKeyExchange(OutputStream output)
        throws IOException
    {
        this.premasterSecret = TlsRSAUtils.generateEncryptedPreMasterSecret(context, this.rsaServerPublicKey, output);
    }

    public void processClientKeyExchange(InputStream input)
        throws IOException
    {

        byte[] encryptedPreMasterSecret;
        if (context.getServerVersion().isSSL())
        {
            // TODO Do any SSLv3 clients actually include the length?
            encryptedPreMasterSecret = Streams.readAll(input);
        }
        else
        {
            encryptedPreMasterSecret = TlsUtils.readOpaque16(input);
        }

        ProtocolVersion clientVersion = context.getClientVersion();

        /*
         * RFC 5246 7.4.7.1.
         */
        {
            // TODO Provide as configuration option?
            boolean versionNumberCheckDisabled = false;

            /*
             * See notes regarding Bleichenbacher/Klima attack. The code here implements the first
             * construction proposed there, which is RECOMMENDED.
             */
            byte[] R = new byte[48];
            this.context.getSecureRandom().nextBytes(R);

            byte[] M = TlsUtils.EMPTY_BYTES;
            try
            {
                M = serverCredentials.decryptPreMasterSecret(encryptedPreMasterSecret);
            }
            catch (Exception e)
            {
                /*
                 * In any case, a TLS server MUST NOT generate an alert if processing an
                 * RSA-encrypted premaster secret message fails, or the version number is not as
                 * expected. Instead, it MUST continue the handshake with a randomly generated
                 * premaster secret.
                 */
            }

            if (M.length != 48)
            {
                TlsUtils.writeVersion(clientVersion, R, 0);
                this.premasterSecret = R;
            }
            else
            {
                /*
                 * If ClientHello.client_version is TLS 1.1 or higher, server implementations MUST
                 * check the version number [..].
                 */
                if (versionNumberCheckDisabled && clientVersion.isEqualOrEarlierVersionOf(ProtocolVersion.TLSv10))
                {
                    /*
                     * If the version number is TLS 1.0 or earlier, server implementations SHOULD
                     * check the version number, but MAY have a configuration option to disable the
                     * check.
                     */
                }
                else
                {
                    /*
                     * Note that explicitly constructing the pre_master_secret with the
                     * ClientHello.client_version produces an invalid master_secret if the client
                     * has sent the wrong version in the original pre_master_secret.
                     */
                    TlsUtils.writeVersion(clientVersion, M, 0);
                }
                this.premasterSecret = M;
            }
        }
    }

    public byte[] generatePremasterSecret()
        throws IOException
    {
        if (this.premasterSecret == null)
        {
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }

        byte[] tmp = this.premasterSecret;
        this.premasterSecret = null;
        return tmp;
    }

    // Would be needed to process RSA_EXPORT server key exchange
    // protected void processRSAServerKeyExchange(InputStream is, Signer signer) throws IOException
    // {
    // InputStream sigIn = is;
    // if (signer != null)
    // {
    // sigIn = new SignerInputStream(is, signer);
    // }
    //
    // byte[] modulusBytes = TlsUtils.readOpaque16(sigIn);
    // byte[] exponentBytes = TlsUtils.readOpaque16(sigIn);
    //
    // if (signer != null)
    // {
    // byte[] sigByte = TlsUtils.readOpaque16(is);
    //
    // if (!signer.verifySignature(sigByte))
    // {
    // handler.failWithError(AlertLevel.fatal, AlertDescription.bad_certificate);
    // }
    // }
    //
    // BigInteger modulus = new BigInteger(1, modulusBytes);
    // BigInteger exponent = new BigInteger(1, exponentBytes);
    //
    // this.rsaServerPublicKey = validateRSAPublicKey(new RSAKeyParameters(false, modulus,
    // exponent));
    // }

    protected RSAKeyParameters validateRSAPublicKey(RSAKeyParameters key)
        throws IOException
    {
        // TODO What is the minimum bit length required?
        // key.getModulus().bitLength();

        if (!key.getExponent().isProbablePrime(2))
        {
            throw new TlsFatalAlert(AlertDescription.illegal_parameter);
        }

        return key;
    }
}
