package org.bouncycastle.jce.provider;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.SignedData;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.x509.X509StreamParserSpi;
import org.bouncycastle.x509.util.StreamParsingException;

public class X509CertParser
    extends X509StreamParserSpi
{
    private static final PEMUtil PEM_PARSER = new PEMUtil("CERTIFICATE");

    private ASN1Set     sData = null;
    private int         sDataObjectCount = 0;
    private InputStream currentStream = null;

    private Certificate readDERCertificate(
        final InputStream in)
        throws IOException, CertificateParsingException
    {
        final ASN1InputStream dIn = new ASN1InputStream(in);
        final ASN1Sequence seq = (ASN1Sequence)dIn.readObject();

        if (seq.size() > 1
                && seq.getObjectAt(0) instanceof DERObjectIdentifier)
        {
            if (seq.getObjectAt(0).equals(PKCSObjectIdentifiers.signedData))
            {
                this.sData = new SignedData(ASN1Sequence.getInstance(
                                (ASN1TaggedObject)seq.getObjectAt(1), true)).getCertificates();

                return getCertificate();
            }
        }

        return new X509CertificateObject(
                            X509CertificateStructure.getInstance(seq));
    }

    private Certificate getCertificate()
        throws CertificateParsingException
    {
        if (this.sData != null)
        {
            while (this.sDataObjectCount < this.sData.size())
            {
                final Object obj = this.sData.getObjectAt(this.sDataObjectCount++);

                if (obj instanceof ASN1Sequence)
                {
                   return new X509CertificateObject(
                                    X509CertificateStructure.getInstance(obj));
                }
            }
        }

        return null;
    }

    private Certificate readPEMCertificate(
        final InputStream  in)
        throws IOException, CertificateParsingException
    {
        final ASN1Sequence seq = PEM_PARSER.readPEMObject(in);

        if (seq != null)
        {
            return new X509CertificateObject(
                            X509CertificateStructure.getInstance(seq));
        }

        return null;
    }

    @Override
	public void engineInit(final InputStream in)
    {
        this.currentStream = in;
        this.sData = null;
        this.sDataObjectCount = 0;

        if (!this.currentStream.markSupported())
        {
            this.currentStream = new BufferedInputStream(this.currentStream);
        }
    }

    @Override
	public Object engineRead()
        throws StreamParsingException
    {
        try
        {
            if (this.sData != null)
            {
                if (this.sDataObjectCount != this.sData.size())
                {
                    return getCertificate();
                }
                else
                {
                    this.sData = null;
                    this.sDataObjectCount = 0;
                    return null;
                }
            }

            this.currentStream.mark(10);
            final int    tag = this.currentStream.read();

            if (tag == -1)
            {
                return null;
            }

            if (tag != 0x30)  // assume ascii PEM encoded.
            {
                this.currentStream.reset();
                return readPEMCertificate(this.currentStream);
            }
            else
            {
                this.currentStream.reset();
                return readDERCertificate(this.currentStream);
            }
        }
        catch (final Exception e)
        {
            throw new StreamParsingException(e.toString(), e);
        }
    }

    @Override
	public Collection engineReadAll()
        throws StreamParsingException
    {
        Certificate     cert;
        final List certs = new ArrayList();

        while ((cert = (Certificate)engineRead()) != null)
        {
            certs.add(cert);
        }

        return certs;
    }
}
