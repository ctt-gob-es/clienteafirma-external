package org.bouncycastle.cert.bc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.X509ExtensionUtils;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.operator.DigestCalculator;

public final class BcX509ExtensionUtils
    extends X509ExtensionUtils
{
    /**
     * Create a utility class pre-configured with a SHA-1 digest calculator based on the
     * BC implementation.
     */
    public BcX509ExtensionUtils()
    {
        super(new SHA1DigestCalculator());
    }

    public BcX509ExtensionUtils(final DigestCalculator calculator)
    {
        super(calculator);
    }

    public AuthorityKeyIdentifier createAuthorityKeyIdentifier(
        final AsymmetricKeyParameter publicKey)
        throws IOException
    {
        return super.createAuthorityKeyIdentifier(SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(publicKey));
    }

    /**
     * Return a RFC 3280 type 1 key identifier. As in:
     * <pre>
     * (1) The keyIdentifier is composed of the 160-bit SHA-1 hash of the
     * value of the BIT STRING subjectPublicKey (excluding the tag,
     * length, and number of unused bits).
     * </pre>
     * @param publicKey the key object containing the key identifier is to be based on.
     * @return the key identifier.
     * @throws IOException
     */
    public SubjectKeyIdentifier createSubjectKeyIdentifier(
        final AsymmetricKeyParameter publicKey)
        throws IOException
    {
        return super.createSubjectKeyIdentifier(SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(publicKey));
    }

    private static class SHA1DigestCalculator
        implements DigestCalculator
    {
        private final ByteArrayOutputStream bOut = new ByteArrayOutputStream();

        public SHA1DigestCalculator() {
			// TODO Auto-generated constructor stub
		}

		@Override
		public AlgorithmIdentifier getAlgorithmIdentifier()
        {
            return new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1);
        }

        @Override
		public OutputStream getOutputStream()
        {
            return this.bOut;
        }

        @Override
		public byte[] getDigest()
        {
            final byte[] bytes = this.bOut.toByteArray();

            this.bOut.reset();

            final Digest sha1 = new SHA1Digest();

            sha1.update(bytes, 0, bytes.length);

            final byte[] digest = new byte[sha1.getDigestSize()];

            sha1.doFinal(digest, 0);

            return digest;
        }
    }
}
