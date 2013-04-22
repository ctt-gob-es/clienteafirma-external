package org.bouncycastle.cms;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.operator.ContentVerifier;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.SignatureAlgorithmIdentifierFinder;

public class SignerInformationVerifier
{
    private final ContentVerifierProvider verifierProvider;
    private final DigestCalculatorProvider digestProvider;
    private final SignatureAlgorithmIdentifierFinder sigAlgorithmFinder;
    private final CMSSignatureAlgorithmNameGenerator sigNameGenerator;

    public SignerInformationVerifier(final CMSSignatureAlgorithmNameGenerator sigNameGenerator,
    		                         final SignatureAlgorithmIdentifierFinder sigAlgorithmFinder,
    		                         final ContentVerifierProvider verifierProvider,
    		                         final DigestCalculatorProvider digestProvider)
    {
        this.sigNameGenerator = sigNameGenerator;
        this.sigAlgorithmFinder = sigAlgorithmFinder;
        this.verifierProvider = verifierProvider;
        this.digestProvider = digestProvider;
    }

    public boolean hasAssociatedCertificate()
    {
        return this.verifierProvider.hasAssociatedCertificate();
    }

    public X509CertificateHolder getAssociatedCertificate()
    {
        return this.verifierProvider.getAssociatedCertificate();
    }

    public ContentVerifier getContentVerifier(final AlgorithmIdentifier signingAlgorithm, final AlgorithmIdentifier digestAlgorithm)
        throws OperatorCreationException
    {
        final String          signatureName = this.sigNameGenerator.getSignatureName(digestAlgorithm, signingAlgorithm);

        return this.verifierProvider.get(this.sigAlgorithmFinder.find(signatureName));
    }

    public DigestCalculator getDigestCalculator(final AlgorithmIdentifier algorithmIdentifier)
        throws OperatorCreationException
    {
        return this.digestProvider.get(algorithmIdentifier);
    }
}
