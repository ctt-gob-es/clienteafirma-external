package org.bouncycastle.operator.jcajce;

import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.jcajce.DefaultJcaJceHelper;
import org.bouncycastle.jcajce.NamedJcaJceHelper;
import org.bouncycastle.jcajce.ProviderJcaJceHelper;
import org.bouncycastle.operator.ContentVerifier;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OperatorStreamException;
import org.bouncycastle.operator.RawContentVerifier;
import org.bouncycastle.operator.RuntimeOperatorException;

public class JcaContentVerifierProviderBuilder
{
    private OperatorHelper helper = new OperatorHelper(new DefaultJcaJceHelper());

    public JcaContentVerifierProviderBuilder()
    {
    }

    public JcaContentVerifierProviderBuilder setProvider(final Provider provider)
    {
        this.helper = new OperatorHelper(new ProviderJcaJceHelper(provider));

        return this;
    }

    public JcaContentVerifierProviderBuilder setProvider(final String providerName)
    {
        this.helper = new OperatorHelper(new NamedJcaJceHelper(providerName));

        return this;
    }

    public ContentVerifierProvider build(final X509CertificateHolder certHolder)
        throws OperatorCreationException, CertificateException
    {
        return build(this.helper.convertCertificate(certHolder));
    }

    public ContentVerifierProvider build(final X509Certificate certificate)
        throws OperatorCreationException
    {
        final X509CertificateHolder certHolder;

        try
        {
            certHolder = new JcaX509CertificateHolder(certificate);
        }
        catch (final CertificateEncodingException e)
        {
            throw new OperatorCreationException("cannot process certificate: " + e.getMessage(), e); //$NON-NLS-1$
        }

        return new ContentVerifierProvider()
        {
            private SignatureOutputStream stream;

            @Override
			public boolean hasAssociatedCertificate()
            {
                return true;
            }

            @Override
			public X509CertificateHolder getAssociatedCertificate()
            {
                return certHolder;
            }

            @Override
			public ContentVerifier get(final AlgorithmIdentifier algorithm)
                throws OperatorCreationException
            {
                try
                {
                    final Signature sig = JcaContentVerifierProviderBuilder.this.helper.createSignature(algorithm);

                    sig.initVerify(certificate.getPublicKey());

                    this.stream = new SignatureOutputStream(sig);
                }
                catch (final GeneralSecurityException e)
                {
                    throw new OperatorCreationException("exception on setup: " + e, e); //$NON-NLS-1$
                }

                final Signature rawSig = createRawSig(algorithm, certificate.getPublicKey());

                if (rawSig != null)
                {
                    return new RawSigVerifier(algorithm, this.stream, rawSig);
                }
                else
                {
                    return new SigVerifier(algorithm, this.stream);
                }
            }
        };
    }

    public ContentVerifierProvider build(final PublicKey publicKey)
        throws OperatorCreationException
    {
        return new ContentVerifierProvider()
        {
            @Override
			public boolean hasAssociatedCertificate()
            {
                return false;
            }

            @Override
			public X509CertificateHolder getAssociatedCertificate()
            {
                return null;
            }

            @Override
			public ContentVerifier get(final AlgorithmIdentifier algorithm)
                throws OperatorCreationException
            {
                final SignatureOutputStream stream = createSignatureStream(algorithm, publicKey);

                final Signature rawSig = createRawSig(algorithm, publicKey);

                if (rawSig != null)
                {
                    return new RawSigVerifier(algorithm, stream, rawSig);
                }
                else
                {
                    return new SigVerifier(algorithm, stream);
                }
            }
        };
    }

    private SignatureOutputStream createSignatureStream(final AlgorithmIdentifier algorithm, final PublicKey publicKey)
        throws OperatorCreationException
    {
        try
        {
            final Signature sig = this.helper.createSignature(algorithm);

            sig.initVerify(publicKey);

            return new SignatureOutputStream(sig);
        }
        catch (final GeneralSecurityException e)
        {
            throw new OperatorCreationException("exception on setup: " + e, e); //$NON-NLS-1$
        }
    }

    private Signature createRawSig(final AlgorithmIdentifier algorithm, final PublicKey publicKey)
    {
        Signature rawSig;
        try
        {
            rawSig = this.helper.createRawSignature(algorithm);

            if (rawSig != null)
            {
                rawSig.initVerify(publicKey);
            }
        }
        catch (final Exception e)
        {
            rawSig = null;
        }
        return rawSig;
    }

    private class SigVerifier
        implements ContentVerifier
    {
        private final SignatureOutputStream stream;
        private final AlgorithmIdentifier algorithm;

        SigVerifier(final AlgorithmIdentifier algorithm, final SignatureOutputStream stream)
        {
            this.algorithm = algorithm;
            this.stream = stream;
        }

        @Override
		public AlgorithmIdentifier getAlgorithmIdentifier()
        {
            return this.algorithm;
        }

        @Override
		public OutputStream getOutputStream()
        {
            if (this.stream == null)
            {
                throw new IllegalStateException("verifier not initialised"); //$NON-NLS-1$
            }

            return this.stream;
        }

        @Override
		public boolean verify(final byte[] expected)
        {
            try
            {
                return this.stream.verify(expected);
            }
            catch (final SignatureException e)
            {
                throw new RuntimeOperatorException("exception obtaining signature: " + e.getMessage(), e); //$NON-NLS-1$
            }
        }
    }

    private class RawSigVerifier
        extends SigVerifier
        implements RawContentVerifier
    {
        private final Signature rawSignature;

        RawSigVerifier(final AlgorithmIdentifier algorithm, final SignatureOutputStream stream, final Signature rawSignature)
        {
            super(algorithm, stream);
            this.rawSignature = rawSignature;
        }

        @Override
		public boolean verify(final byte[] digest, final byte[] expected)
        {
            try
            {
                this.rawSignature.update(digest);

                return this.rawSignature.verify(expected);
            }
            catch (final SignatureException e)
            {
                throw new RuntimeOperatorException("exception obtaining raw signature: " + e.getMessage(), e); //$NON-NLS-1$
            }
        }
    }

    private class SignatureOutputStream
        extends OutputStream
    {
        private final Signature sig;

        SignatureOutputStream(final Signature sig)
        {
            this.sig = sig;
        }

        @Override
		public void write(final byte[] bytes, final int off, final int len)
            throws IOException
        {
            try
            {
                this.sig.update(bytes, off, len);
            }
            catch (final SignatureException e)
            {
                throw new OperatorStreamException("exception in content signer: " + e.getMessage(), e); //$NON-NLS-1$
            }
        }

        @Override
		public void write(final byte[] bytes)
            throws IOException
        {
            try
            {
                this.sig.update(bytes);
            }
            catch (final SignatureException e)
            {
                throw new OperatorStreamException("exception in content signer: " + e.getMessage(), e); //$NON-NLS-1$
            }
        }

        @Override
		public void write(final int b)
            throws IOException
        {
            try
            {
                this.sig.update((byte)b);
            }
            catch (final SignatureException e)
            {
                throw new OperatorStreamException("exception in content signer: " + e.getMessage(), e); //$NON-NLS-1$
            }
        }

        boolean verify(final byte[] expected)
            throws SignatureException
        {
            return this.sig.verify(expected);
        }
    }
}