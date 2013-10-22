package org.bouncycastle.cms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CRLException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.BEROctetStringGenerator;
import org.bouncycastle.asn1.BERSet;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.cms.OtherRevocationInfoFormat;
import org.bouncycastle.asn1.ocsp.OCSPResponse;
import org.bouncycastle.asn1.ocsp.OCSPResponseStatus;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.CertificateList;
import org.bouncycastle.asn1.x509.TBSCertificate;
import org.bouncycastle.cert.X509AttributeCertificateHolder;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.io.Streams;
import org.bouncycastle.util.io.TeeInputStream;
import org.bouncycastle.util.io.TeeOutputStream;

class CMSUtils
{
    static ContentInfo readContentInfo(
        byte[] input)
        throws CMSException
    {
        // enforce limit checking as from a byte array
        return readContentInfo(new ASN1InputStream(input));
    }

    static ContentInfo readContentInfo(
        InputStream input)
        throws CMSException
    {
        // enforce some limit checking
        return readContentInfo(new ASN1InputStream(input));
    } 

    static List getCertificatesFromStore(CertStore certStore)
        throws CertStoreException, CMSException
    {
        List certs = new ArrayList();

        try
        {
            for (Iterator it = certStore.getCertificates(null).iterator(); it.hasNext();)
            {
                X509Certificate c = (X509Certificate)it.next();

                certs.add(Certificate.getInstance(ASN1Primitive.fromByteArray(c.getEncoded())));
            }

            return certs;
        }
        catch (IllegalArgumentException e)
        {
            throw new CMSException("error processing certs", e);
        }
        catch (IOException e)
        {
            throw new CMSException("error processing certs", e);
        }
        catch (CertificateEncodingException e)
        {
            throw new CMSException("error encoding certs", e);
        }
    }

    static List getCertificatesFromStore(Store certStore)
        throws CMSException
    {
        List certs = new ArrayList();

        try
        {
            for (Iterator it = certStore.getMatches(null).iterator(); it.hasNext();)
            {
                X509CertificateHolder c = (X509CertificateHolder)it.next();

                certs.add(c.toASN1Structure());
            }

            return certs;
        }
        catch (ClassCastException e)
        {
            throw new CMSException("error processing certs", e);
        }
    }

    static List getAttributeCertificatesFromStore(Store attrStore)
        throws CMSException
    {
        List certs = new ArrayList();

        try
        {
            for (Iterator it = attrStore.getMatches(null).iterator(); it.hasNext();)
            {
                X509AttributeCertificateHolder attrCert = (X509AttributeCertificateHolder)it.next();

                certs.add(new DERTaggedObject(false, 2, attrCert.toASN1Structure()));
            }

            return certs;
        }
        catch (ClassCastException e)
        {
            throw new CMSException("error processing certs", e);
        }
    }

    static List getCRLsFromStore(CertStore certStore)
        throws CertStoreException, CMSException
    {
        List crls = new ArrayList();

        try
        {
            for (Iterator it = certStore.getCRLs(null).iterator(); it.hasNext();)
            {
                X509CRL c = (X509CRL)it.next();

                crls.add(CertificateList.getInstance(ASN1Primitive.fromByteArray(c.getEncoded())));
            }

            return crls;
        }
        catch (IllegalArgumentException e)
        {
            throw new CMSException("error processing crls", e);
        }
        catch (IOException e)
        {
            throw new CMSException("error processing crls", e);
        }
        catch (CRLException e)
        {
            throw new CMSException("error encoding crls", e);
        }
    }

    static List getCRLsFromStore(Store crlStore)
        throws CMSException
    {
        List certs = new ArrayList();

        try
        {
            for (Iterator it = crlStore.getMatches(null).iterator(); it.hasNext();)
            {
                X509CRLHolder c = (X509CRLHolder)it.next();

                certs.add(c.toASN1Structure());
            }

            return certs;
        }
        catch (ClassCastException e)
        {
            throw new CMSException("error processing certs", e);
        }
    }

    static Collection getOthersFromStore(ASN1ObjectIdentifier otherRevocationInfoFormat, Store otherRevocationInfos)
    {
        List others = new ArrayList();

        for (Iterator it = otherRevocationInfos.getMatches(null).iterator(); it.hasNext();)
        {
            ASN1Encodable info = (ASN1Encodable)it.next();

            if (CMSObjectIdentifiers.id_ri_ocsp_response.equals(otherRevocationInfoFormat))
            {
                OCSPResponse resp = OCSPResponse.getInstance(info);

                if (resp.getResponseStatus().getValue().intValue() != OCSPResponseStatus.SUCCESSFUL)
                {
                    throw new IllegalArgumentException("cannot add unsuccessful OCSP response to CMS SignedData");
                }
            }

            others.add(new DERTaggedObject(false, 1, new OtherRevocationInfoFormat(otherRevocationInfoFormat, info)));
        }

        return others;
    }

    static ASN1Set createBerSetFromList(List derObjects)
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        for (Iterator it = derObjects.iterator(); it.hasNext();)
        {
            v.add((ASN1Encodable)it.next());
        }

        return new BERSet(v);
    }

    static ASN1Set createDerSetFromList(List derObjects)
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        for (Iterator it = derObjects.iterator(); it.hasNext();)
        {
            v.add((ASN1Encodable)it.next());
        }

        return new DERSet(v);
    }

    static OutputStream createBEROctetOutputStream(OutputStream s,
            int tagNo, boolean isExplicit, int bufferSize) throws IOException
    {
        BEROctetStringGenerator octGen = new BEROctetStringGenerator(s, tagNo, isExplicit);

        if (bufferSize != 0)
        {
            return octGen.getOctetOutputStream(new byte[bufferSize]);
        }

        return octGen.getOctetOutputStream();
    }

    static TBSCertificate getTBSCertificateStructure(
        X509Certificate cert)
    {
        try
        {
            return TBSCertificate.getInstance(
                ASN1Primitive.fromByteArray(cert.getTBSCertificate()));
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException(
                "can't extract TBS structure from this cert");
        }
    }

    static IssuerAndSerialNumber getIssuerAndSerialNumber(X509Certificate cert)
    {
        TBSCertificate tbsCert = getTBSCertificateStructure(cert);
        return new IssuerAndSerialNumber(tbsCert.getIssuer(), tbsCert.getSerialNumber().getValue());
    }

    private static ContentInfo readContentInfo(
        ASN1InputStream in)
        throws CMSException
    {
        try
        {
            return ContentInfo.getInstance(in.readObject());
        }
        catch (IOException e)
        {
            throw new CMSException("IOException reading content.", e);
        }
        catch (ClassCastException e)
        {
            throw new CMSException("Malformed content.", e);
        }
        catch (IllegalArgumentException e)
        {
            throw new CMSException("Malformed content.", e);
        }
    }
    
    public static byte[] streamToByteArray(
        InputStream in) 
        throws IOException
    {
        return Streams.readAll(in);
    }

    public static byte[] streamToByteArray(
        InputStream in,
        int         limit)
        throws IOException
    {
        return Streams.readAllLimited(in, limit);
    }

    public static Provider getProvider(String providerName)
        throws NoSuchProviderException
    {
        if (providerName != null)
        {
            Provider prov = Security.getProvider(providerName);

            if (prov != null)
            {
                return prov;
            }

            throw new NoSuchProviderException("provider " + providerName + " not found.");
        }

        return null; 
    }

    static InputStream attachDigestsToInputStream(Collection digests, InputStream s)
    {
        InputStream result = s;
        Iterator it = digests.iterator();
        while (it.hasNext())
        {
            DigestCalculator digest = (DigestCalculator)it.next();
            result = new TeeInputStream(result, digest.getOutputStream());
        }
        return result;
    }

    static OutputStream attachSignersToOutputStream(Collection signers, OutputStream s)
    {
        OutputStream result = s;
        Iterator it = signers.iterator();
        while (it.hasNext())
        {
            SignerInfoGenerator signerGen = (SignerInfoGenerator)it.next();
            result = getSafeTeeOutputStream(result, signerGen.getCalculatingOutputStream());
        }
        return result;
    }

    static OutputStream getSafeOutputStream(OutputStream s)
    {
        return s == null ? new NullOutputStream() : s;
    }

    static OutputStream getSafeTeeOutputStream(OutputStream s1,
            OutputStream s2)
    {
        return s1 == null ? getSafeOutputStream(s2)
                : s2 == null ? getSafeOutputStream(s1) : new TeeOutputStream(
                        s1, s2);
    }
}
