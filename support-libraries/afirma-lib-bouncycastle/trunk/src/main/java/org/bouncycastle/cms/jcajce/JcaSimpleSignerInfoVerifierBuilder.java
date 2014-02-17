package org.bouncycastle.cms.jcajce;

import java.security.Provider;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.DefaultCMSSignatureAlgorithmNameGenerator;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

public final class JcaSimpleSignerInfoVerifierBuilder
{
    private Helper helper = new Helper();

    public JcaSimpleSignerInfoVerifierBuilder setProvider(final Provider provider)
    {
        this.helper = new ProviderHelper(provider);

        return this;
    }

    public JcaSimpleSignerInfoVerifierBuilder setProvider(final String providerName)
    {
        this.helper = new NamedHelper(providerName);

        return this;
    }

    public SignerInformationVerifier build(final X509CertificateHolder certHolder)
        throws OperatorCreationException, CertificateException
    {
        return new SignerInformationVerifier(new DefaultCMSSignatureAlgorithmNameGenerator(), new DefaultSignatureAlgorithmIdentifierFinder(), this.helper.createContentVerifierProvider(certHolder), this.helper.createDigestCalculatorProvider());
    }

    public SignerInformationVerifier build(final X509Certificate certificate)
        throws OperatorCreationException
    {
        return new SignerInformationVerifier(new DefaultCMSSignatureAlgorithmNameGenerator(), new DefaultSignatureAlgorithmIdentifierFinder(), this.helper.createContentVerifierProvider(certificate), this.helper.createDigestCalculatorProvider());
    }

    public SignerInformationVerifier build(final PublicKey pubKey)
        throws OperatorCreationException
    {
        return new SignerInformationVerifier(new DefaultCMSSignatureAlgorithmNameGenerator(), new DefaultSignatureAlgorithmIdentifierFinder(), this.helper.createContentVerifierProvider(pubKey), this.helper.createDigestCalculatorProvider());
    }

    private class Helper
    {
        public Helper() {
			// Vacio
		}

		ContentVerifierProvider createContentVerifierProvider(final PublicKey publicKey)
            throws OperatorCreationException
        {
            return new JcaContentVerifierProviderBuilder().build(publicKey);
        }

        ContentVerifierProvider createContentVerifierProvider(final X509Certificate certificate)
            throws OperatorCreationException
        {
            return new JcaContentVerifierProviderBuilder().build(certificate);
        }

        ContentVerifierProvider createContentVerifierProvider(final X509CertificateHolder certHolder)
            throws OperatorCreationException, CertificateException
        {
            return new JcaContentVerifierProviderBuilder().build(certHolder);
        }

        DigestCalculatorProvider createDigestCalculatorProvider()
            throws OperatorCreationException
        {
            return new JcaDigestCalculatorProviderBuilder().build();
        }
    }

    private class NamedHelper
        extends Helper
    {
        private final String providerName;

        public NamedHelper(final String providerName)
        {
            this.providerName = providerName;
        }

        @Override
		ContentVerifierProvider createContentVerifierProvider(final PublicKey publicKey)
            throws OperatorCreationException
        {
            return new JcaContentVerifierProviderBuilder().setProvider(this.providerName).build(publicKey);
        }

        @Override
		ContentVerifierProvider createContentVerifierProvider(final X509Certificate certificate)
            throws OperatorCreationException
        {
            return new JcaContentVerifierProviderBuilder().setProvider(this.providerName).build(certificate);
        }

        @Override
		DigestCalculatorProvider createDigestCalculatorProvider()
            throws OperatorCreationException
        {
            return new JcaDigestCalculatorProviderBuilder().setProvider(this.providerName).build();
        }

        @Override
		ContentVerifierProvider createContentVerifierProvider(final X509CertificateHolder certHolder)
            throws OperatorCreationException, CertificateException
        {
            return new JcaContentVerifierProviderBuilder().setProvider(this.providerName).build(certHolder);
        }
    }

    private class ProviderHelper
        extends Helper
    {
        private final Provider provider;

        public ProviderHelper(final Provider provider)
        {
            this.provider = provider;
        }

        @Override
		ContentVerifierProvider createContentVerifierProvider(final PublicKey publicKey)
            throws OperatorCreationException
        {
            return new JcaContentVerifierProviderBuilder().setProvider(this.provider).build(publicKey);
        }

        @Override
		ContentVerifierProvider createContentVerifierProvider(final X509Certificate certificate)
            throws OperatorCreationException
        {
            return new JcaContentVerifierProviderBuilder().setProvider(this.provider).build(certificate);
        }

        @Override
		DigestCalculatorProvider createDigestCalculatorProvider()
            throws OperatorCreationException
        {
            return new JcaDigestCalculatorProviderBuilder().setProvider(this.provider).build();
        }

        @Override
		ContentVerifierProvider createContentVerifierProvider(final X509CertificateHolder certHolder)
            throws OperatorCreationException, CertificateException
        {
            return new JcaContentVerifierProviderBuilder().setProvider(this.provider).build(certHolder);
        }
    }
}
