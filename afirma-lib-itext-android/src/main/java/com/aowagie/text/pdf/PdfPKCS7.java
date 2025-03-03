/*
 * Copyright 2004 by Paulo Soares.
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * The Original Code is 'iText, a free JAVA-PDF library'.
 *
 * The Initial Developer of the Original Code is Bruno Lowagie. Portions created by
 * the Initial Developer are Copyright (C) 1999, 2000, 2001, 2002 by Bruno Lowagie.
 * All Rights Reserved.
 * Co-Developer of the code is Paulo Soares. Portions created by the Co-Developer
 * are Copyright (C) 2000, 2001, 2002 by Paulo Soares. All Rights Reserved.
 *
 * Contributor(s): all the names of the contributors are added in the source code
 * where applicable.
 *
 * Alternatively, the contents of this file may be used under the terms of the
 * LGPL license (the "GNU LIBRARY GENERAL PUBLIC LICENSE"), in which case the
 * provisions of LGPL are applicable instead of those above.  If you wish to
 * allow use of your version of this file only under the terms of the LGPL
 * License and not to allow others to use your version of this file under
 * the MPL, indicate your decision by deleting the provisions above and
 * replace them with the notice and other provisions required by the LGPL.
 * If you do not delete the provisions above, a recipient may use your version
 * of this file under either the MPL or the GNU LIBRARY GENERAL PUBLIC LICENSE.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the MPL as stated above or under the terms of the GNU
 * Library General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library general Public License for more
 * details.
 *
 * If you didn't download this code from the following link, you should check if
 * you aren't using an obsolete version:
 * http://www.lowagie.com/iText/
 */
package com.aowagie.text.pdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CRL;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.ASN1Enumerated;
import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1OutputStream;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.ASN1Set;
import org.spongycastle.asn1.ASN1String;
import org.spongycastle.asn1.ASN1TaggedObject;
import org.spongycastle.asn1.BERTaggedObject;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.DEROctetString;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.asn1.DERSet;
import org.spongycastle.asn1.DERTaggedObject;
import org.spongycastle.asn1.DERUTCTime;
import org.spongycastle.asn1.cms.Attribute;
import org.spongycastle.asn1.cms.AttributeTable;
import org.spongycastle.asn1.cms.ContentInfo;
import org.spongycastle.asn1.ocsp.BasicOCSPResponse;
import org.spongycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.ocsp.BasicOCSPResp;
import org.spongycastle.cert.ocsp.CertificateID;
import org.spongycastle.cert.ocsp.SingleResp;
import org.spongycastle.jce.provider.X509CRLParser;
import org.spongycastle.jce.provider.X509CertParser;
import org.spongycastle.operator.bc.BcDigestCalculatorProvider;
import org.spongycastle.tsp.TimeStampToken;

import com.aowagie.text.ExceptionConverter;

/**
 * This class does all the processing related to signing and verifying a PKCS#7
 * signature.
 * <p>
 * It's based in code found at org.spongycastle.
 */
public class PdfPKCS7 {

    private byte sigAttr[];
    private byte digestAttr[];
    private int version, signerversion;
    private Set<String> digestalgos;
    private Collection<Certificate> certs;
    private Collection<CRL> crls;
    private Collection<Certificate> signCerts;
    private X509Certificate signCert;
    private byte[] digest;
    private MessageDigest messageDigest;
    private String digestAlgorithm, digestEncryptionAlgorithm;
    private Signature sig;
    private byte RSAdata[];
    private boolean verified;
    private boolean verifyResult;
    private byte externalDigest[];
    private byte externalRSAdata[];
    private String provider;

    private static final String ID_PKCS7_DATA = "1.2.840.113549.1.7.1"; //$NON-NLS-1$
    private static final String ID_PKCS7_SIGNED_DATA = "1.2.840.113549.1.7.2"; //$NON-NLS-1$
    private static final String ID_RSA = "1.2.840.113549.1.1.1"; //$NON-NLS-1$
    private static final String ID_DSA = "1.2.840.10040.4.1"; //$NON-NLS-1$
    private static final String ID_CONTENT_TYPE = "1.2.840.113549.1.9.3"; //$NON-NLS-1$
    private static final String ID_MESSAGE_DIGEST = "1.2.840.113549.1.9.4"; //$NON-NLS-1$
    private static final String ID_SIGNING_TIME = "1.2.840.113549.1.9.5"; //$NON-NLS-1$
    private static final String ID_ADBE_REVOCATION = "1.2.840.113583.1.1.8"; //$NON-NLS-1$
    /**
     * Holds value of property reason.
     */
    private String reason;

    /**
     * Holds value of property location.
     */
    private String location;

    /**
     * Holds value of property contactInfo.
     */
    private String contactInfo;

    /**
     * Holds value of property signDate.
     */
    private Calendar signDate;

    /**
     * Holds value of property signName.
     */
    private String signName;

    private TimeStampToken timeStampToken;

    private static final HashMap<String, String> strictDigestNames = new LinkedHashMap<String, String>();
    private static final HashMap<String, String> digestNames = new LinkedHashMap<String, String>();
    private static final HashMap<String, String> algorithmNames = new LinkedHashMap<String, String>();
    private static final HashMap<String, String> allowedDigests = new LinkedHashMap<String, String>();

    static {
    	strictDigestNames.put("1.2.840.113549.2.5", "MD5"); //$NON-NLS-1$ //$NON-NLS-2$
    	strictDigestNames.put("1.2.840.113549.2.2", "MD2"); //$NON-NLS-1$ //$NON-NLS-2$
        strictDigestNames.put("1.3.14.3.2.26", "SHA1"); //$NON-NLS-1$ //$NON-NLS-2$
        strictDigestNames.put("2.16.840.1.101.3.4.2.4", "SHA224"); //$NON-NLS-1$ //$NON-NLS-2$
        strictDigestNames.put("2.16.840.1.101.3.4.2.1", "SHA256"); //$NON-NLS-1$ //$NON-NLS-2$
        strictDigestNames.put("2.16.840.1.101.3.4.2.2", "SHA384"); //$NON-NLS-1$ //$NON-NLS-2$
        strictDigestNames.put("2.16.840.1.101.3.4.2.3", "SHA512"); //$NON-NLS-1$ //$NON-NLS-2$
        strictDigestNames.put("1.3.36.3.2.2", "RIPEMD128"); //$NON-NLS-1$ //$NON-NLS-2$
        strictDigestNames.put("1.3.36.3.2.1", "RIPEMD160"); //$NON-NLS-1$ //$NON-NLS-2$
        strictDigestNames.put("1.3.36.3.2.3", "RIPEMD256"); //$NON-NLS-1$ //$NON-NLS-2$

        digestNames.put("1.2.840.113549.2.5", "MD5"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("1.2.840.113549.2.2", "MD2"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("1.3.14.3.2.26", "SHA1"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("2.16.840.1.101.3.4.2.4", "SHA224"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("2.16.840.1.101.3.4.2.1", "SHA256"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("2.16.840.1.101.3.4.2.2", "SHA384"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("2.16.840.1.101.3.4.2.3", "SHA512"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("1.3.36.3.2.2", "RIPEMD128"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("1.3.36.3.2.1", "RIPEMD160"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("1.3.36.3.2.3", "RIPEMD256"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("1.2.840.113549.1.1.4", "MD5"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("1.2.840.113549.1.1.2", "MD2"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("1.2.840.113549.1.1.5", "SHA1"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("1.2.840.113549.1.1.14", "SHA224"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("1.2.840.113549.1.1.11", "SHA256"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("1.2.840.113549.1.1.12", "SHA384"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("1.2.840.113549.1.1.13", "SHA512"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("1.2.840.10040.4.3", "SHA1"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("2.16.840.1.101.3.4.3.1", "SHA224"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("2.16.840.1.101.3.4.3.2", "SHA256"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("2.16.840.1.101.3.4.3.3", "SHA384"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("2.16.840.1.101.3.4.3.4", "SHA512"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("1.3.36.3.3.1.3", "RIPEMD128"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("1.3.36.3.3.1.2", "RIPEMD160"); //$NON-NLS-1$ //$NON-NLS-2$
        digestNames.put("1.3.36.3.3.1.4", "RIPEMD256"); //$NON-NLS-1$ //$NON-NLS-2$

        algorithmNames.put("1.2.840.113549.1.1.1", "RSA"); //$NON-NLS-1$ //$NON-NLS-2$
        algorithmNames.put("1.2.840.10040.4.1", "DSA"); //$NON-NLS-1$ //$NON-NLS-2$
        algorithmNames.put("1.2.840.113549.1.1.2", "RSA"); //$NON-NLS-1$ //$NON-NLS-2$
        algorithmNames.put("1.2.840.113549.1.1.4", "RSA"); //$NON-NLS-1$ //$NON-NLS-2$
        algorithmNames.put("1.2.840.113549.1.1.5", "RSA"); //$NON-NLS-1$ //$NON-NLS-2$
        algorithmNames.put("1.2.840.113549.1.1.14", "RSA"); //$NON-NLS-1$ //$NON-NLS-2$
        algorithmNames.put("1.2.840.113549.1.1.11", "RSA"); //$NON-NLS-1$ //$NON-NLS-2$
        algorithmNames.put("1.2.840.113549.1.1.12", "RSA"); //$NON-NLS-1$ //$NON-NLS-2$
        algorithmNames.put("1.2.840.113549.1.1.13", "RSA"); //$NON-NLS-1$ //$NON-NLS-2$
        algorithmNames.put("1.2.840.10040.4.3", "DSA"); //$NON-NLS-1$ //$NON-NLS-2$
        algorithmNames.put("2.16.840.1.101.3.4.3.1", "DSA"); //$NON-NLS-1$ //$NON-NLS-2$
        algorithmNames.put("2.16.840.1.101.3.4.3.2", "DSA"); //$NON-NLS-1$ //$NON-NLS-2$
        algorithmNames.put("1.3.36.3.3.1.3", "RSA"); //$NON-NLS-1$ //$NON-NLS-2$
        algorithmNames.put("1.3.36.3.3.1.2", "RSA"); //$NON-NLS-1$ //$NON-NLS-2$
        algorithmNames.put("1.3.36.3.3.1.4", "RSA"); //$NON-NLS-1$ //$NON-NLS-2$

        allowedDigests.put("MD5", "1.2.840.113549.2.5"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("MD2", "1.2.840.113549.2.2"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("SHA1", "1.3.14.3.2.26"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("SHA224", "2.16.840.1.101.3.4.2.4"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("SHA256", "2.16.840.1.101.3.4.2.1"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("SHA384", "2.16.840.1.101.3.4.2.2"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("SHA512", "2.16.840.1.101.3.4.2.3"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("MD-5", "1.2.840.113549.2.5"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("MD-2", "1.2.840.113549.2.2"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("SHA-1", "1.3.14.3.2.26"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("SHA-224", "2.16.840.1.101.3.4.2.4"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("SHA-256", "2.16.840.1.101.3.4.2.1"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("SHA-384", "2.16.840.1.101.3.4.2.2"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("SHA-512", "2.16.840.1.101.3.4.2.3"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("RIPEMD128", "1.3.36.3.2.2"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("RIPEMD-128", "1.3.36.3.2.2"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("RIPEMD160", "1.3.36.3.2.1"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("RIPEMD-160", "1.3.36.3.2.1"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("RIPEMD256", "1.3.36.3.2.3"); //$NON-NLS-1$ //$NON-NLS-2$
        allowedDigests.put("RIPEMD-256", "1.3.36.3.2.3"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /** Obtiene el PKCS&#35;1 de la firma PKCS&#35;7 del PDF.
     * @return PKCS&#35;1 de la firma PKCS&#35;7 del PDF. */
    public byte[] getPkcs1() {
    	return this.digest != null ? this.digest.clone() : null;
    }

    /**
     * Gets the digest name for a certain id
     * @param oid	an id (for instance "1.2.840.113549.2.5")
     * @return	a digest name (for instance "MD5")
     * @since	2.1.6
     */
    private static String getDigest(final String oid) {
        final String ret = digestNames.get(oid);
        if (ret == null) {
			return oid;
		}
		return ret;
    }

    /**
     * Gets the algorithm name for a certain id.
     * @param oid	an id (for instance "1.2.840.113549.1.1.1")
     * @return	an algorithm name (for instance "RSA")
     * @since	2.1.6
     */
    private static String getAlgorithm(final String oid) {
        final String ret = algorithmNames.get(oid);
        if (ret == null) {
			return oid;
		}
		return ret;
    }

    /**
     * Gets the timestamp token if there is one.
     * @return the timestamp token or null
     * @since	2.1.6
     */
    public TimeStampToken getTimeStampToken() {
    	return this.timeStampToken;
    }

    /**
     * Gets the timestamp date
     * @return	a date
     * @since	2.1.6
     */
    public Calendar getTimeStampDate() {
        if (this.timeStampToken == null) {
			return null;
		}
        final Calendar cal = new GregorianCalendar();
        final Date date = this.timeStampToken.getTimeStampInfo().getGenTime();
        cal.setTime(date);
        return cal;
    }

    /**
     * Verifies a signature using the sub-filter adbe.x509.rsa_sha1.
     * @param contentsKey the /Contents key
     * @param certsKey the /Cert key
     * @param provider the provider or <code>null</code> for the default provider
     */
    PdfPKCS7(final byte[] contentsKey, final byte[] certsKey, final String provider) {
        try {
            this.provider = provider;

            final CertificateFactory cf = CertificateFactory.getInstance("X.509"); //$NON-NLS-1$
            this.certs = (Collection<Certificate>) cf.generateCertificates(new ByteArrayInputStream(certsKey));

            this.signCerts = this.certs;
            this.signCert = (X509Certificate)this.certs.iterator().next();
            this.crls = new ArrayList<CRL>();
            final ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(contentsKey));
            this.digest = ((DEROctetString)in.readObject()).getOctets();
            in.close();
            if (provider == null) {
				this.sig = Signature.getInstance("SHA1withRSA"); //$NON-NLS-1$
			}
            else {
				this.sig = Signature.getInstance("SHA1withRSA", provider); //$NON-NLS-1$
			}

            PublicKey publicKey = this.signCert.getPublicKey();
            if (publicKey == null) {
            	publicKey = CertificateFactory.getInstance("X.509").generateCertificate( //$NON-NLS-1$
            		new ByteArrayInputStream(this.signCert.getEncoded())
            	).getPublicKey();
            }
            if (publicKey != null) {
            	this.sig.initVerify(publicKey);
            }
            else {
            	throw new CertificateEncodingException(
        			"El certificado no contiene una clave publica adecuada" //$NON-NLS-1$
    			);
            }

        }
        catch (final Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    private BasicOCSPResp basicResp;

    /**
     * Gets the OCSP basic response if there is one.
     * @return the OCSP basic response or null
     * @since	2.1.6
     */
    public BasicOCSPResp getOcsp() {
        return this.basicResp;
    }

    private void findOcsp(ASN1Sequence seq) throws IOException {
        this.basicResp = null;
        boolean ret = false;
        while (true) {
            if (seq.getObjectAt(0) instanceof ASN1ObjectIdentifier
                && ((ASN1ObjectIdentifier)seq.getObjectAt(0)).getId().equals(OCSPObjectIdentifiers.id_pkix_ocsp_basic.getId())) {
                break;
            }
            ret = true;
            for (int k = 0; k < seq.size(); ++k) {
                if (seq.getObjectAt(k) instanceof ASN1Sequence) {
                    seq = (ASN1Sequence)seq.getObjectAt(0);
                    ret = false;
                    break;
                }
                if (seq.getObjectAt(k) instanceof ASN1TaggedObject) {
                    final ASN1TaggedObject tag = (ASN1TaggedObject)seq.getObjectAt(k);
                    if (tag.getObject() instanceof ASN1Sequence) {
                        seq = (ASN1Sequence)tag.getObject();
                        ret = false;
                        break;
                    }
					return;
                }
            }
            if (ret) {
				return;
			}
        }
        final DEROctetString os = (DEROctetString)seq.getObjectAt(1);
        final ASN1InputStream inp = new ASN1InputStream(os.getOctets());
        final BasicOCSPResponse resp = BasicOCSPResponse.getInstance(inp.readObject());
        this.basicResp = new BasicOCSPResp(resp);
    }

    /**
     * Verifies a signature using the sub-filter adbe.pkcs7.detached or
     * adbe.pkcs7.sha1.
     * @param contentsKey the /Contents key
     * @param provider the provider or <code>null</code> for the default provider
     */
    PdfPKCS7(final byte[] contentsKey, final String provider) {
        try {
            this.provider = provider;
            final ASN1InputStream din = new ASN1InputStream(new ByteArrayInputStream(contentsKey));

            //
            // Basic checks to make sure it's a PKCS#7 SignedData Object
            //
            ASN1Primitive pkcs;

            try {
                pkcs = din.readObject();
            }
            catch (final IOException e) {
                throw new IllegalArgumentException("can't decode PKCS7SignedData object", e); //$NON-NLS-1$
            }
            if (!(pkcs instanceof ASN1Sequence)) {
                throw new IllegalArgumentException("Not a valid PKCS#7 object - not a sequence"); //$NON-NLS-1$
            }
            final ASN1Sequence signedData = (ASN1Sequence)pkcs;
            final ASN1ObjectIdentifier objId = (ASN1ObjectIdentifier)signedData.getObjectAt(0);
            if (!objId.getId().equals(ID_PKCS7_SIGNED_DATA)) {
				throw new IllegalArgumentException("Not a valid PKCS#7 object - not signed data"); //$NON-NLS-1$
			}
            final ASN1Sequence content = (ASN1Sequence)((ASN1TaggedObject)signedData.getObjectAt(1)).getObject();
            // the positions that we care are:
            //     0 - version
            //     1 - digestAlgorithms
            //     2 - possible ID_PKCS7_DATA
            //     (the certificates and crls are taken out by other means)
            //     last - signerInfos

            // the version
            this.version = ((ASN1Integer)content.getObjectAt(0)).getValue().intValue();

            // the digestAlgorithms
            this.digestalgos = new HashSet();
            final Enumeration e = ((ASN1Set)content.getObjectAt(1)).getObjects();
            while (e.hasMoreElements())
            {
                final ASN1Sequence s = (ASN1Sequence)e.nextElement();
                final ASN1ObjectIdentifier o = (ASN1ObjectIdentifier)s.getObjectAt(0);
                this.digestalgos.add(o.getId());
            }

            // the certificates and crls
            final X509CertParser cr = new X509CertParser();
            cr.engineInit(new ByteArrayInputStream(contentsKey));
            this.certs = cr.engineReadAll();
            final X509CRLParser cl = new X509CRLParser();
            cl.engineInit(new ByteArrayInputStream(contentsKey));
            this.crls = cl.engineReadAll();

            // the possible ID_PKCS7_DATA
            final ASN1Sequence rsaData = (ASN1Sequence)content.getObjectAt(2);
            if (rsaData.size() > 1) {
            	final ASN1Encodable encodable = rsaData.getObjectAt(1);
            	final ASN1OctetString rsaDataContent;
            	if (encodable instanceof DERTaggedObject) {
            		rsaDataContent = (ASN1OctetString)((DERTaggedObject)encodable).getObject();
            	}
            	else if (encodable instanceof BERTaggedObject) {
            		rsaDataContent = (ASN1OctetString)((BERTaggedObject)encodable).getObject();
            	}
            	else {
            		throw new IllegalArgumentException("El objeto ASN.1 no es ni BER ni BER ni DER: " + encodable.getClass().getName()); //$NON-NLS-1$
            	}
                this.RSAdata = rsaDataContent.getOctets();
            }

            // The SignerInfos:
            // SignerInfos ::= SET OF SignerInfo
            int next = 3;
            while (content.getObjectAt(next) instanceof ASN1TaggedObject) {
				++next;
			}

            final ASN1Set signerInfos = (ASN1Set)content.getObjectAt(next);
            if (signerInfos.size() != 1) {
				throw new IllegalArgumentException("This PKCS#7 object has multiple SignerInfos - only one is supported at this time"); //$NON-NLS-1$
			}
            final ASN1Sequence signerInfo = (ASN1Sequence)signerInfos.getObjectAt(0);

            // the positions that we care are
            //     0 - version
            //     1 - the signing certificate serial number
            //     2 - the digest algorithm
            //     3 or 4 - digestEncryptionAlgorithm
            //     4 or 5 - encryptedDigest

            this.signerversion = ((ASN1Integer)signerInfo.getObjectAt(0)).getValue().intValue();

            // Get the signing certificate
            final ASN1Sequence issuerAndSerialNumber = (ASN1Sequence)signerInfo.getObjectAt(1);
            final BigInteger serialNumber = ((ASN1Integer)issuerAndSerialNumber.getObjectAt(1)).getValue();
            for (final Iterator i = this.certs.iterator(); i.hasNext();) {
                final X509Certificate cert = (X509Certificate)i.next();
                if (serialNumber.equals(cert.getSerialNumber())) {
                    this.signCert = cert;
                    break;
                }
            }
            if (this.signCert == null) {
                throw new IllegalArgumentException("Can't find signing certificate with serial " + serialNumber.toString(16)); //$NON-NLS-1$
            }
            signCertificateChain();
            this.digestAlgorithm = ((ASN1ObjectIdentifier)((ASN1Sequence)signerInfo.getObjectAt(2)).getObjectAt(0)).getId();
            next = 3;
            if (signerInfo.getObjectAt(next) instanceof ASN1TaggedObject) {
                final ASN1TaggedObject tagsig = (ASN1TaggedObject)signerInfo.getObjectAt(next);
                final ASN1Set sseq = ASN1Set.getInstance(tagsig, false);
                this.sigAttr = sseq.getEncoded(ASN1Encoding.DER);

                for (int k = 0; k < sseq.size(); ++k) {
                    final ASN1Sequence seq2 = (ASN1Sequence)sseq.getObjectAt(k);
                    if (((ASN1ObjectIdentifier)seq2.getObjectAt(0)).getId().equals(ID_MESSAGE_DIGEST)) {
                        final ASN1Set set = (ASN1Set)seq2.getObjectAt(1);
                        this.digestAttr = ((DEROctetString)set.getObjectAt(0)).getOctets();
                    }
                    else if (((ASN1ObjectIdentifier)seq2.getObjectAt(0)).getId().equals(ID_ADBE_REVOCATION)) {
                        final ASN1Set setout = (ASN1Set)seq2.getObjectAt(1);
                        final ASN1Sequence seqout = (ASN1Sequence)setout.getObjectAt(0);
                        for (int j = 0; j < seqout.size(); ++j) {
                            final ASN1TaggedObject tg = (ASN1TaggedObject)seqout.getObjectAt(j);
                            if (tg.getTagNo() != 1) {
								continue;
							}
                            final ASN1Sequence seqin = (ASN1Sequence)tg.getObject();
                            findOcsp(seqin);
                        }
                    }
                }
                if (this.digestAttr == null) {
					throw new IllegalArgumentException("Authenticated attribute is missing the digest."); //$NON-NLS-1$
				}
                ++next;
            }
            this.digestEncryptionAlgorithm = ((ASN1ObjectIdentifier)((ASN1Sequence)signerInfo.getObjectAt(next++)).getObjectAt(0)).getId();
            this.digest = ((DEROctetString)signerInfo.getObjectAt(next++)).getOctets();
            if (next < signerInfo.size() && signerInfo.getObjectAt(next) instanceof DERTaggedObject) {
                final DERTaggedObject taggedObject = (DERTaggedObject) signerInfo.getObjectAt(next);
                final ASN1Set unat = ASN1Set.getInstance(taggedObject, false);
                final AttributeTable attble = new AttributeTable(unat);
                final Attribute ts = attble.get(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
                if (ts != null) {
                    final ASN1Set attributeValues = ts.getAttrValues();
                    final ASN1Sequence tokenSequence = ASN1Sequence.getInstance(attributeValues.getObjectAt(0));
                    final ContentInfo contentInfo = ContentInfo.getInstance(tokenSequence);
                    this.timeStampToken = new TimeStampToken(contentInfo);
                }
            }
            if (this.RSAdata != null || this.digestAttr != null) {
                if (provider == null || provider.startsWith("SunPKCS11")) { //$NON-NLS-1$
                	this.messageDigest = MessageDigest.getInstance(getDigestAlgorithmName(getHashAlgorithm()));
				}
                else {
					this.messageDigest = MessageDigest.getInstance(getDigestAlgorithmName(getHashAlgorithm()), provider);
				}
            }
            if (provider == null) {
				this.sig = Signature.getInstance(getDigestAlgorithm());
			}
            else {
				this.sig = Signature.getInstance(getDigestAlgorithm(), provider);
			}

            PublicKey publicKey = this.signCert.getPublicKey();
            if (publicKey == null) {
            	publicKey = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(this.signCert.getEncoded())).getPublicKey(); //$NON-NLS-1$
            }
            if (publicKey != null) {
            	this.sig.initVerify(publicKey);
            }
            else {
            	throw new CertificateEncodingException(
        			"El certificado no contiene una clave publica adecuada" //$NON-NLS-1$
    			);
            }

        }
        catch (final Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    /**
     * Generates a signature.
     * @param privKey the private key
     * @param certChain the certificate chain
     * @param crlList the certificate revocation list
     * @param hashAlgorithm the hash algorithm
     * @param provider the provider or <code>null</code> for the default provider
     * @param hasRSAdata <CODE>true</CODE> if the sub-filter is adbe.pkcs7.sha1
     * @throws InvalidKeyException on error
     * @throws NoSuchProviderException on error
     * @throws NoSuchAlgorithmException on error
     */
    PdfPKCS7(final PrivateKey privKey,
    		 final Certificate[] certChain,
    		 final CRL[] crlList,
    		 final String hashAlgorithm,
    		 final String provider,
    		 final boolean hasRSAdata) throws InvalidKeyException,
    										  NoSuchProviderException,
    										  NoSuchAlgorithmException {
        this.provider = provider;

        this.digestAlgorithm = allowedDigests.get(hashAlgorithm.toUpperCase());
        if (this.digestAlgorithm == null) {
			throw new NoSuchAlgorithmException("Unknown Hash Algorithm "+hashAlgorithm); //$NON-NLS-1$
		}

        this.version = this.signerversion = 1;
        this.certs = new ArrayList();
        this.crls = new ArrayList();
        this.digestalgos = new HashSet();
        this.digestalgos.add(this.digestAlgorithm);

        //
        // Copy in the certificates and crls used to sign the private key.
        //
        this.signCert = (X509Certificate)certChain[0];
        for (final Certificate element : certChain) {
            this.certs.add(element);
        }

        if (crlList != null) {
            for (final CRL element : crlList) {
                this.crls.add(element);
            }
        }

        if (privKey != null) {
            //
            // Now we have private key, find out what the digestEncryptionAlgorithm is.
            //
            this.digestEncryptionAlgorithm = privKey.getAlgorithm();
            if (this.digestEncryptionAlgorithm.equals("RSA")) { //$NON-NLS-1$
                this.digestEncryptionAlgorithm = ID_RSA;
            }
            else if (this.digestEncryptionAlgorithm.equals("DSA")) { //$NON-NLS-1$
                this.digestEncryptionAlgorithm = ID_DSA;
            }
            else {
                throw new NoSuchAlgorithmException("Unknown Key Algorithm "+this.digestEncryptionAlgorithm); //$NON-NLS-1$
            }
        }
        if (hasRSAdata) {
            this.RSAdata = new byte[0];
            if (provider == null || provider.startsWith("SunPKCS11")) { //$NON-NLS-1$
				this.messageDigest = MessageDigest.getInstance(getHashAlgorithm());
			}
            else {
				this.messageDigest = MessageDigest.getInstance(getHashAlgorithm(), provider);
			}
        }

        if (privKey != null) {
            if (provider == null) {
				this.sig = Signature.getInstance(getDigestAlgorithm());
			}
            else {
				this.sig = Signature.getInstance(getDigestAlgorithm(), provider);
			}

            this.sig.initSign(privKey);
        }
    }

    /**
     * Update the digest with the specified bytes. This method is used both for signing and verifying
     * @param buf the data buffer
     * @param off the offset in the data buffer
     * @param len the data length
     * @throws SignatureException on error
     */
    void update(final byte[] buf, final int off, final int len) throws SignatureException {
        if (this.RSAdata != null || this.digestAttr != null) {
			this.messageDigest.update(buf, off, len);
		}
        else {
			this.sig.update(buf, off, len);
		}
    }

    /**
     * Get all the X.509 certificates associated with this PKCS#7 object in no particular order.
     * Other certificates, from OCSP for example, will also be included.
     * @return the X.509 certificates associated with this PKCS#7 object
     */
    public Certificate[] getCertificates() {
        return this.certs.toArray(new X509Certificate[this.certs.size()]);
    }

    /**
     * Get the X.509 sign certificate chain associated with this PKCS#7 object.
     * Only the certificates used for the main signature will be returned, with
     * the signing certificate first.
     * @return the X.509 certificates associated with this PKCS#7 object
     * @since	2.1.6
     */
    public Certificate[] getSignCertificateChain() {
        return this.signCerts.toArray(new X509Certificate[this.signCerts.size()]);
    }

    private void signCertificateChain() {
        final ArrayList<Certificate> cc = new ArrayList<Certificate>();
        cc.add(this.signCert);
        final ArrayList<Certificate> oc = new ArrayList<Certificate>(this.certs);
        for (int k = 0; k < oc.size(); ++k) {
            if (this.signCert.getSerialNumber().equals(((X509Certificate)oc.get(k)).getSerialNumber())) {
                oc.remove(k);
                --k;
                continue;
            }
        }
        boolean found = true;
        while (found) {
            final X509Certificate v = (X509Certificate) cc.get(cc.size() - 1);
            found = false;
            for (int k = 0; k < oc.size(); ++k) {
                try {
                    if (this.provider == null) {
						v.verify(((X509Certificate)oc.get(k)).getPublicKey());
					}
                    else {
						v.verify(((X509Certificate)oc.get(k)).getPublicKey(), this.provider);
					}
                    found = true;
                    cc.add(oc.get(k));
                    oc.remove(k);
                    break;
                }
                catch (final Exception e) {
                	// Vacio
                }
            }
        }
        this.signCerts = cc;
    }

    /**
     * Get the X.509 certificate revocation lists associated with this PKCS#7 object
     * @return the X.509 certificate revocation lists associated with this PKCS#7 object
     */
    public Collection<CRL> getCRLs() {
        return this.crls;
    }

    /**
     * Get the X.509 certificate actually used to sign the digest.
     * @return the X.509 certificate actually used to sign the digest
     */
    public X509Certificate getSigningCertificate() {
        return this.signCert;
    }

    /**
     * Get the version of the PKCS#7 object. Always 1
     * @return the version of the PKCS#7 object. Always 1
     */
    public int getVersion() {
        return this.version;
    }

    /**
     * Get the version of the PKCS#7 "SignerInfo" object. Always 1
     * @return the version of the PKCS#7 "SignerInfo" object. Always 1
     */
    public int getSigningInfoVersion() {
        return this.signerversion;
    }

    /**
     * Get the algorithm used to calculate the message digest
     * @return the algorithm used to calculate the message digest
     */
    public String getDigestAlgorithm() {
        String dea = getAlgorithm(this.digestEncryptionAlgorithm);
        if (dea == null) {
			dea = this.digestEncryptionAlgorithm;
		}

        final String algo = dea != null ? OIDS.get(dea) : null;
        return algo != null ? algo : getHashAlgorithm() + "with" + dea; //$NON-NLS-1$
    }

    /**
     * Returns the algorithm.
     * @return the digest algorithm
     */
    public String getHashAlgorithm() {
        return getDigest(this.digestAlgorithm);
    }

    /**
     * Returns the declared hash algorithm.
     * @return the digest algorithm or {@code null} is there isn't a valid
     * hash algorithm.
     */
    public String getStrictHashAlgorithm() {
        return strictDigestNames.get(this.digestAlgorithm);
    }

    /**
     * Checks if OCSP revocation refers to the document signing certificate.
     * @return true if it checks false otherwise
     * @since	2.1.6
     */
    public boolean isRevocationValid() {
        if (this.basicResp == null) {
			return false;
		}
        if (this.signCerts.size() < 2) {
			return false;
		}
        try {
            final X509Certificate[] cs = (X509Certificate[])getSignCertificateChain();
            final SingleResp sr = this.basicResp.getResponses()[0];
            final CertificateID cid = sr.getCertID();
            final X509Certificate sigcer = getSigningCertificate();
            final X509Certificate isscer = cs[1];
            final CertificateID tis = new CertificateID(
        		new BcDigestCalculatorProvider().get(CertificateID.HASH_SHA1),
        		new X509CertificateHolder(isscer.getEncoded()),
        		sigcer.getSerialNumber()
    		);
            return tis.equals(cid);
        }
        catch (final Exception ex) {
        	// Vacio
        }
        return false;
    }

    /**
     * Get the "issuer" from the TBSCertificate bytes that are passed in
     * @param enc a TBSCertificate in a byte array
     * @return a DERObject
     */
    private static ASN1Primitive getIssuer(final byte[] enc) {
        try {
            final ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(enc));
            final ASN1Sequence seq = (ASN1Sequence)in.readObject();
            in.close();
            return (ASN1Primitive)seq.getObjectAt(seq.getObjectAt(0) instanceof DERTaggedObject ? 3 : 2);
        }
        catch (final IOException e) {
            throw new ExceptionConverter(e);
        }
    }

    /**
     * Get the "subject" from the TBSCertificate bytes that are passed in
     * @param enc A TBSCertificate in a byte array
     * @return a DERObject
     */
    private static ASN1Primitive getSubject(final byte[] enc) {
        try {
            final ASN1InputStream in = new ASN1InputStream(new ByteArrayInputStream(enc));
            final ASN1Sequence seq = (ASN1Sequence)in.readObject();
            return (ASN1Primitive)seq.getObjectAt(seq.getObjectAt(0) instanceof DERTaggedObject ? 5 : 4);
        }
        catch (final IOException e) {
            throw new ExceptionConverter(e);
        }
    }



    /**
     * Get the subject fields from an X509 Certificate
     * @param cert an X509Certificate
     * @return an X509Name
     */
    public static X509Name getSubjectFields(final X509Certificate cert) {
        try {
            return new X509Name((ASN1Sequence)getSubject(cert.getTBSCertificate()));
        }
        catch (final Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    /**
     * Gets the bytes for the PKCS#1 object.
     * @return a byte array
     */
    public byte[] getEncodedPKCS1() {
        try {
            if (this.externalDigest != null) {
				this.digest = this.externalDigest;
			} else {
				this.digest = this.sig.sign();
			}
            final ByteArrayOutputStream   bOut = new ByteArrayOutputStream();

            final ASN1OutputStream dout = new ASN1OutputStream(bOut);
            dout.writeObject(new DEROctetString(this.digest));
            dout.close();

            return bOut.toByteArray();
        }
        catch (final Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    /**
     * Sets the digest/signature to an external calculated value.
     * @param digest the digest. This is the actual signature
     * @param RSAdata the extra data that goes into the data tag in PKCS#7
     * @param digestEncryptionAlgorithm the encryption algorithm. It may must be <CODE>null</CODE> if the <CODE>digest</CODE>
     * is also <CODE>null</CODE>. If the <CODE>digest</CODE> is not <CODE>null</CODE>
     * then it may be "RSA" or "DSA"
     */
    public void setExternalDigest(final byte digest[], final byte RSAdata[], final String digestEncryptionAlgorithm) {
        this.externalDigest = digest;
        this.externalRSAdata = RSAdata;
        if (digestEncryptionAlgorithm != null) {
            if (digestEncryptionAlgorithm.equals("RSA")) { //$NON-NLS-1$
                this.digestEncryptionAlgorithm = ID_RSA;
            }
            else if (digestEncryptionAlgorithm.equals("DSA")) { //$NON-NLS-1$
                this.digestEncryptionAlgorithm = ID_DSA;
            } else {
				throw new ExceptionConverter(new NoSuchAlgorithmException("Unknown Key Algorithm "+digestEncryptionAlgorithm)); //$NON-NLS-1$
			}
        }
    }

    /**
     * Gets the bytes for the PKCS7SignedData object.
     * @return the bytes for the PKCS7SignedData object
     */
    public byte[] getEncodedPKCS7() {
        return getEncodedPKCS7(null, null, null, null);
    }



    /**
     * Gets the bytes for the PKCS7SignedData object. Optionally the authenticatedAttributes
     * in the signerInfo can also be set, OR a time-stamp-authority client
     * may be provided.
     * @param secondDigest the digest in the authenticatedAttributes
     * @param signingTime the signing time in the authenticatedAttributes
     * @param tsaClient TSAClient - null or an optional time stamp authority client
     * @param ocsp OCSP
     * @return byte[] the bytes for the PKCS7SignedData object
     * @since	2.1.6
     */
    private byte[] getEncodedPKCS7(final byte secondDigest[], final Calendar signingTime, final TSAClient tsaClient, final byte[] ocsp) {
        try {
            if (this.externalDigest != null) {
                this.digest = this.externalDigest;
                if (this.RSAdata != null) {
					this.RSAdata = this.externalRSAdata;
				}
            }
            else if (this.externalRSAdata != null && this.RSAdata != null) {
                this.RSAdata = this.externalRSAdata;
                this.sig.update(this.RSAdata);
                try {
                	this.digest = this.sig.sign();
                }
                catch(final Exception e) {
                	// Vacio
                }
            }
            else {
                if (this.RSAdata != null) {
                    this.RSAdata = this.messageDigest.digest();
                    this.sig.update(this.RSAdata);
                }
                try {
                	this.digest = this.sig.sign();
                }
                catch(final Exception e) {
                	// Vacio
                }
            }

            // Create the set of Hash algorithms
            final ASN1EncodableVector digestAlgorithms = new ASN1EncodableVector();
            for(final Iterator it = this.digestalgos.iterator(); it.hasNext();) {
                final ASN1EncodableVector algos = new ASN1EncodableVector();
                algos.add(new ASN1ObjectIdentifier((String)it.next()));
                algos.add(DERNull.INSTANCE);
                digestAlgorithms.add(new DERSequence(algos));
            }

            // Create the contentInfo.
            ASN1EncodableVector v = new ASN1EncodableVector();
            v.add(new ASN1ObjectIdentifier(ID_PKCS7_DATA));
            if (this.RSAdata != null) {
				v.add(new DERTaggedObject(0, new DEROctetString(this.RSAdata)));
			}
            final DERSequence contentinfo = new DERSequence(v);

            // Get all the certificates
            //
            v = new ASN1EncodableVector();
            for (final Iterator i = this.certs.iterator(); i.hasNext();) {
                final ASN1InputStream tempstream = new ASN1InputStream(new ByteArrayInputStream(((X509Certificate)i.next()).getEncoded()));
                v.add(tempstream.readObject());
            }

            final DERSet dercertificates = new DERSet(v);

            // Create signerinfo structure.
            //
            final ASN1EncodableVector signerinfo = new ASN1EncodableVector();

            // Add the signerInfo version
            //
            signerinfo.add(new ASN1Integer(this.signerversion));

            v = new ASN1EncodableVector();
            v.add(getIssuer(this.signCert.getTBSCertificate()));
            v.add(new ASN1Integer(this.signCert.getSerialNumber()));
            signerinfo.add(new DERSequence(v));

            // Add the digestAlgorithm
            v = new ASN1EncodableVector();
            v.add(new ASN1ObjectIdentifier(this.digestAlgorithm));
            v.add(DERNull.INSTANCE);
            signerinfo.add(new DERSequence(v));

            // add the authenticated attribute if present
            if (secondDigest != null && signingTime != null) {
                signerinfo.add(new DERTaggedObject(false, 0, getAuthenticatedAttributeSet(secondDigest, signingTime, ocsp)));
            }
            // Add the digestEncryptionAlgorithm
            v = new ASN1EncodableVector();
            v.add(new ASN1ObjectIdentifier(this.digestEncryptionAlgorithm));
            v.add(DERNull.INSTANCE);
            signerinfo.add(new DERSequence(v));

            // Add the digest
            signerinfo.add(new DEROctetString(this.digest));

            // When requested, go get and add the timestamp. May throw an exception.
            // Added by Martin Brunecky, 07/12/2007 folowing Aiken Sam, 2006-11-15
            // Sam found Adobe expects time-stamped SHA1-1 of the encrypted digest
            if (tsaClient != null) {
                final byte[] tsImprint = MessageDigest.getInstance("SHA-1").digest(this.digest); //$NON-NLS-1$
                final byte[] tsToken = tsaClient.getTimeStampToken(this, tsImprint);
                if (tsToken != null) {
                    final ASN1EncodableVector unauthAttributes = buildUnauthenticatedAttributes(tsToken);
                    if (unauthAttributes != null) {
                        signerinfo.add(new DERTaggedObject(false, 1, new DERSet(unauthAttributes)));
                    }
                }
            }

            // Finally build the body out of all the components above
            final ASN1EncodableVector body = new ASN1EncodableVector();
            body.add(new ASN1Integer(this.version));
            body.add(new DERSet(digestAlgorithms));
            body.add(contentinfo);
            body.add(new DERTaggedObject(false, 0, dercertificates));

           if (!this.crls.isEmpty()) {
                v = new ASN1EncodableVector();
                for (final Iterator i = this.crls.iterator();i.hasNext();) {
                    final ASN1InputStream t = new ASN1InputStream(new ByteArrayInputStream(((X509CRL)i.next()).getEncoded()));
                    v.add(t.readObject());
                }
                final DERSet dercrls = new DERSet(v);
                body.add(new DERTaggedObject(false, 1, dercrls));
            }

            // Only allow one signerInfo
            body.add(new DERSet(new DERSequence(signerinfo)));

            // Now we have the body, wrap it in it's PKCS7Signed shell
            // and return it
            //
            final ASN1EncodableVector whole = new ASN1EncodableVector();
            whole.add(new ASN1ObjectIdentifier(ID_PKCS7_SIGNED_DATA));
            whole.add(new DERTaggedObject(0, new DERSequence(body)));

            final ByteArrayOutputStream   bOut = new ByteArrayOutputStream();

            final ASN1OutputStream dout = new ASN1OutputStream(bOut);
            dout.writeObject(new DERSequence(whole));
            dout.close();

            return bOut.toByteArray();
        }
        catch (final Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    /**
     * Added by Aiken Sam, 2006-11-15, modifed by Martin Brunecky 07/12/2007
     * to start with the timeStampToken (signedData 1.2.840.113549.1.7.2).
     * Token is the TSA response without response status, which is usually
     * handled by the (vendor supplied) TSA request/response interface).
     * @param timeStampToken byte[] - time stamp token, DER encoded signedData
     * @return ASN1EncodableVector
     * @throws IOException on error
     */
    private static ASN1EncodableVector buildUnauthenticatedAttributes(final byte[] timeStampToken)  throws IOException {
        if (timeStampToken == null) {
			return null;
		}

        // @todo: move this together with the rest of the defintions
        final String ID_TIME_STAMP_TOKEN = "1.2.840.113549.1.9.16.2.14"; // RFC 3161 id-aa-timeStampToken //$NON-NLS-1$

        final ASN1InputStream tempstream = new ASN1InputStream(new ByteArrayInputStream(timeStampToken));
        final ASN1EncodableVector unauthAttributes = new ASN1EncodableVector();

        final ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new ASN1ObjectIdentifier(ID_TIME_STAMP_TOKEN)); // id-aa-timeStampToken
        final ASN1Sequence seq = (ASN1Sequence) tempstream.readObject();
        v.add(new DERSet(seq));

        unauthAttributes.add(new DERSequence(v));
        return unauthAttributes;
     }




    private DERSet getAuthenticatedAttributeSet(final byte secondDigest[], final Calendar signingTime, final byte[] ocsp) {
        try {
            final ASN1EncodableVector attribute = new ASN1EncodableVector();
            ASN1EncodableVector v = new ASN1EncodableVector();
            v.add(new ASN1ObjectIdentifier(ID_CONTENT_TYPE));
            v.add(new DERSet(new ASN1ObjectIdentifier(ID_PKCS7_DATA)));
            attribute.add(new DERSequence(v));
            v = new ASN1EncodableVector();
            v.add(new ASN1ObjectIdentifier(ID_SIGNING_TIME));
            v.add(new DERSet(new DERUTCTime(signingTime.getTime())));
            attribute.add(new DERSequence(v));
            v = new ASN1EncodableVector();
            v.add(new ASN1ObjectIdentifier(ID_MESSAGE_DIGEST));
            v.add(new DERSet(new DEROctetString(secondDigest)));
            attribute.add(new DERSequence(v));
            if (ocsp != null) {
                v = new ASN1EncodableVector();
                v.add(new ASN1ObjectIdentifier(ID_ADBE_REVOCATION));
                final DEROctetString doctet = new DEROctetString(ocsp);
                final ASN1EncodableVector vo1 = new ASN1EncodableVector();
                final ASN1EncodableVector v2 = new ASN1EncodableVector();
                v2.add(OCSPObjectIdentifiers.id_pkix_ocsp_basic);
                v2.add(doctet);
                final ASN1Enumerated den = new ASN1Enumerated(0);
                final ASN1EncodableVector v3 = new ASN1EncodableVector();
                v3.add(den);
                v3.add(new DERTaggedObject(true, 0, new DERSequence(v2)));
                vo1.add(new DERSequence(v3));
                v.add(new DERSet(new DERSequence(new DERTaggedObject(true, 1, new DERSequence(vo1)))));
                attribute.add(new DERSequence(v));
            }
            else if (!this.crls.isEmpty()) {
                v = new ASN1EncodableVector();
                v.add(new ASN1ObjectIdentifier(ID_ADBE_REVOCATION));
                final ASN1EncodableVector v2 = new ASN1EncodableVector();
                for (final Iterator i = this.crls.iterator();i.hasNext();) {
                    final ASN1InputStream t = new ASN1InputStream(new ByteArrayInputStream(((X509CRL)i.next()).getEncoded()));
                    v2.add(t.readObject());
                }
                v.add(new DERSet(new DERSequence(new DERTaggedObject(true, 0, new DERSequence(v2)))));
                attribute.add(new DERSequence(v));
            }
            return new DERSet(attribute);
        }
        catch (final Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    /**
     * Getter for property reason.
     * @return Value of property reason.
     */
    public String getReason() {
        return this.reason;
    }

    /**
     * Setter for property reason.
     * @param reason New value of property reason.
     */
    public void setReason(final String reason) {
        this.reason = reason;
    }

    /**
     * Getter for property location.
     * @return Value of property location.
     */
    public String getLocation() {
        return this.location;
    }

    /**
     * Setter for property location.
     * @param location New value of property location.
     */
    public void setLocation(final String location) {
        this.location = location;
    }

    /**
     * Getter for property contactinfo.
     * @return Value of property contactinfo.
     */
    public String getContactInfo() {
        return this.contactInfo;
    }

    /**
     * Setter for property contactInfo.
     * @param contactInfo New value of property contactInfo.
     */
    public void setContactInfo(final String contactInfo) {
        this.contactInfo = contactInfo;
    }

    /**
     * Getter for property signDate.
     * @return Value of property signDate.
     */
    public Calendar getSignDate() {
        return this.signDate;
    }

    /**
     * Setter for property signDate.
     * @param signDate New value of property signDate.
     */
    public void setSignDate(final Calendar signDate) {
        this.signDate = signDate;
    }

    /**
     * Getter for property sigName.
     * @return Value of property sigName.
     */
    public String getSignName() {
        return this.signName;
    }

    /**
     * Setter for property sigName.
     * @param signName New value of property sigName.
     */
    public void setSignName(final String signName) {
        this.signName = signName;
    }

    /**
     * a class that holds an X509 name
     */
    public static class X509Name {
        /**
         * country code - StringType(SIZE(2))
         */
        private static final ASN1ObjectIdentifier C = new ASN1ObjectIdentifier("2.5.4.6"); //$NON-NLS-1$

        /**
         * organization - StringType(SIZE(1..64))
         */
        private static final ASN1ObjectIdentifier O = new ASN1ObjectIdentifier("2.5.4.10"); //$NON-NLS-1$

        /**
         * organizational unit name - StringType(SIZE(1..64))
         */
        private static final ASN1ObjectIdentifier OU = new ASN1ObjectIdentifier("2.5.4.11"); //$NON-NLS-1$

        /**
         * Title
         */
        private static final ASN1ObjectIdentifier T = new ASN1ObjectIdentifier("2.5.4.12"); //$NON-NLS-1$

        /**
         * common name - StringType(SIZE(1..64))
         */
        private static final ASN1ObjectIdentifier CN = new ASN1ObjectIdentifier("2.5.4.3"); //$NON-NLS-1$

        /**
         * device serial number name - StringType(SIZE(1..64))
         */
        private static final ASN1ObjectIdentifier SN = new ASN1ObjectIdentifier("2.5.4.5"); //$NON-NLS-1$

        /**
         * locality name - StringType(SIZE(1..64))
         */
        private static final ASN1ObjectIdentifier L = new ASN1ObjectIdentifier("2.5.4.7"); //$NON-NLS-1$

        /**
         * state, or province name - StringType(SIZE(1..64))
         */
        private static final ASN1ObjectIdentifier ST = new ASN1ObjectIdentifier("2.5.4.8"); //$NON-NLS-1$

        /** Naming attribute of type X520name */
        private static final ASN1ObjectIdentifier SURNAME = new ASN1ObjectIdentifier("2.5.4.4"); //$NON-NLS-1$
        /** Naming attribute of type X520name */
        private static final ASN1ObjectIdentifier GIVENNAME = new ASN1ObjectIdentifier("2.5.4.42"); //$NON-NLS-1$
        /** Naming attribute of type X520name */
        private static final ASN1ObjectIdentifier INITIALS = new ASN1ObjectIdentifier("2.5.4.43"); //$NON-NLS-1$
        /** Naming attribute of type X520name */
        private static final ASN1ObjectIdentifier GENERATION = new ASN1ObjectIdentifier("2.5.4.44"); //$NON-NLS-1$


        /**
         * Email address (RSA PKCS#9 extension) - IA5String.
         * <p>Note: if you're trying to be ultra orthodox, don't use this! It shouldn't be in here.
         */
        private static final ASN1ObjectIdentifier EmailAddress = new ASN1ObjectIdentifier("1.2.840.113549.1.9.1"); //$NON-NLS-1$



        /** object identifier */
        private static final ASN1ObjectIdentifier DC = new ASN1ObjectIdentifier("0.9.2342.19200300.100.1.25"); //$NON-NLS-1$

        /** LDAP User id. */
        private static final ASN1ObjectIdentifier UID = new ASN1ObjectIdentifier("0.9.2342.19200300.100.1.1"); //$NON-NLS-1$

        /** A HashMap with default symbols */
        private static HashMap<ASN1ObjectIdentifier, String> DefaultSymbols = new LinkedHashMap<ASN1ObjectIdentifier, String>();

        static {
            DefaultSymbols.put(C, "C"); //$NON-NLS-1$
            DefaultSymbols.put(O, "O"); //$NON-NLS-1$
            DefaultSymbols.put(T, "T"); //$NON-NLS-1$
            DefaultSymbols.put(OU, "OU"); //$NON-NLS-1$
            DefaultSymbols.put(CN, "CN"); //$NON-NLS-1$
            DefaultSymbols.put(L, "L"); //$NON-NLS-1$
            DefaultSymbols.put(ST, "ST"); //$NON-NLS-1$
            DefaultSymbols.put(SN, "SN"); //$NON-NLS-1$
            DefaultSymbols.put(EmailAddress, "E"); //$NON-NLS-1$
            DefaultSymbols.put(DC, "DC"); //$NON-NLS-1$
            DefaultSymbols.put(UID, "UID"); //$NON-NLS-1$
            DefaultSymbols.put(SURNAME, "SURNAME"); //$NON-NLS-1$
            DefaultSymbols.put(GIVENNAME, "GIVENNAME"); //$NON-NLS-1$
            DefaultSymbols.put(INITIALS, "INITIALS"); //$NON-NLS-1$
            DefaultSymbols.put(GENERATION, "GENERATION"); //$NON-NLS-1$
        }
        /** A HashMap with values */
        private final HashMap<String, ArrayList<String>> values = new LinkedHashMap<String, ArrayList<String>>();

        /**
         * Constructs an X509 name
         * @param seq an ASN1 Sequence
         */
        private X509Name(final ASN1Sequence seq) {
            final Enumeration e = seq.getObjects();

            while (e.hasMoreElements()) {
                final ASN1Set set = (ASN1Set)e.nextElement();

                for (int i = 0; i < set.size(); i++) {
                    final ASN1Sequence s = (ASN1Sequence)set.getObjectAt(i);
                    final String id = DefaultSymbols.get(s.getObjectAt(0));
                    if (id == null) {
						continue;
					}
                    ArrayList<String> vs = this.values.get(id);
                    if (vs == null) {
                        vs = new ArrayList<String>();
                        this.values.put(id, vs);
                    }
                    vs.add(((ASN1String)s.getObjectAt(1)).getString());
                }
            }
        }


        public String getField(final String name) {
            final ArrayList vs = this.values.get(name);
            return vs == null ? null : (String)vs.get(0);
        }



        /**
         * getter for values
         * @return a HashMap with the fields of the X509 name
         */
        public HashMap<String, ArrayList<String>> getFields() {
            return this.values;
        }

        /**
         * @see java.lang.Object#toString()
         */
        @Override
		public String toString() {
            return this.values.toString();
        }
    }

    /** Obtiene el nombre de un algoritmo de huella digital a partir de una de
	 * las variantes de este.
	 * @param pseudoName Nombre o variante del nombre del algoritmo de huella digital
	 * @return Nombre del algoritmo de huella digital */
	private static String getDigestAlgorithmName(final String pseudoName) {
		final String upperPseudoName = pseudoName.toUpperCase();
		if (upperPseudoName.equals("SHA")  //$NON-NLS-1$
				|| upperPseudoName.equals("http://www.w3.org/2000/09/xmldsig#sha1".toUpperCase()) //$NON-NLS-1$
				|| upperPseudoName.equals("1.3.14.3.2.26") //$NON-NLS-1$
				|| upperPseudoName.startsWith("SHA1") //$NON-NLS-1$
				|| upperPseudoName.startsWith("SHA-1")) //$NON-NLS-1$
		{
			return "SHA1"; //$NON-NLS-1$
		}

		if (upperPseudoName.equals("http://www.w3.org/2001/04/xmlenc#sha256".toUpperCase())  //$NON-NLS-1$
				|| upperPseudoName.equals("2.16.840.1.101.3.4.2.1") //$NON-NLS-1$
				|| upperPseudoName.startsWith("SHA256") //$NON-NLS-1$
				|| upperPseudoName.startsWith("SHA-256")) { //$NON-NLS-1$
			return "SHA-256"; //$NON-NLS-1$
		}

		if (upperPseudoName.startsWith("SHA384") //$NON-NLS-1$
				|| upperPseudoName.equals("2.16.840.1.101.3.4.2.2") //$NON-NLS-1$
				|| upperPseudoName.startsWith("SHA-384")) { //$NON-NLS-1$
			return "SHA-384"; //$NON-NLS-1$
		}

		if (upperPseudoName.equals("http://www.w3.org/2001/04/xmlenc#sha512".toUpperCase())  //$NON-NLS-1$
				|| upperPseudoName.equals("2.16.840.1.101.3.4.2.3") //$NON-NLS-1$
				|| upperPseudoName.startsWith("SHA512") //$NON-NLS-1$
				|| upperPseudoName.startsWith("SHA-512")) { //$NON-NLS-1$
			return "SHA-512"; //$NON-NLS-1$
		}

		if (upperPseudoName.equals("http://www.w3.org/2001/04/xmlenc#ripemd160".toUpperCase())  //$NON-NLS-1$
				|| upperPseudoName.startsWith("RIPEMD160") //$NON-NLS-1$
				|| upperPseudoName.startsWith("RIPEMD-160")) { //$NON-NLS-1$
			return "RIPEMD160"; //$NON-NLS-1$
		}

		if (upperPseudoName.equals("MD5") //$NON-NLS-1$
				|| upperPseudoName.equals("1.2.840.113549.2.5") //$NON-NLS-1$
				|| upperPseudoName.startsWith("MD5")) { //$NON-NLS-1$
			return "MD5"; //$NON-NLS-1$
		}

		if (upperPseudoName.equals("MD2")  //$NON-NLS-1$
				|| upperPseudoName.equals("1.2.840.113549.2.2") //$NON-NLS-1$
				|| upperPseudoName.startsWith("MD2")) { //$NON-NLS-1$
			return "MD2"; //$NON-NLS-1$
		}

		throw new IllegalArgumentException("Algoritmo de huella digital no soportado: " + pseudoName); //$NON-NLS-1$
	}

    /** Verify the digest.
     * @throws SignatureException on error
     * @return <CODE>true</CODE> if the signature checks out, <CODE>false</CODE> otherwise. */
    public boolean verify() throws SignatureException {
        if (this.verified) {
			return this.verifyResult;
		}
        if (this.sigAttr != null) {
            this.sig.update(this.sigAttr);
            if (this.RSAdata != null) {
                final byte msd[] = this.messageDigest.digest();
                this.messageDigest.update(msd);
            }
            this.verifyResult = Arrays.equals(this.messageDigest.digest(), this.digestAttr) && this.sig.verify(this.digest);
        }
        else {
            if (this.RSAdata != null) {
				this.sig.update(this.messageDigest.digest());
			}
            this.verifyResult = this.sig.verify(this.digest);
        }
        this.verified = true;
        return this.verifyResult;
    }

  //**********************************************************************************************
    //*********************** Modificaciones para el soporte de ECDSA ******************************

    private static final String OID_ECDSA_SHA1   = "1.2.840.10045.4.1"; //$NON-NLS-1$
    private static final String OID_ECDSA_SHA224 = "1.2.840.10045.4.3.1"; //$NON-NLS-1$
    private static final String OID_ECDSA_SHA256 = "1.2.840.10045.4.3.2"; //$NON-NLS-1$
    private static final String OID_ECDSA_SHA384 = "1.2.840.10045.4.3.3"; //$NON-NLS-1$
    private static final String OID_ECDSA_SHA512 = "1.2.840.10045.4.3.4"; //$NON-NLS-1$

    private static final String OID_RSA_SHA3_224 = "2.16.840.1.101.3.4.3.13"; //$NON-NLS-1$
    private static final String OID_RSA_SHA3_256 = "2.16.840.1.101.3.4.3.14"; //$NON-NLS-1$
    private static final String OID_RSA_SHA3_384 = "2.16.840.1.101.3.4.3.15"; //$NON-NLS-1$
    private static final String OID_RSA_SHA3_512 = "2.16.840.1.101.3.4.3.16"; //$NON-NLS-1$

    private static final String OID_ECDSA_SHA3_224 = "2.16.840.1.101.3.4.3.9"; //$NON-NLS-1$
    private static final String OID_ECDSA_SHA3_256 = "2.16.840.1.101.3.4.3.10"; //$NON-NLS-1$
    private static final String OID_ECDSA_SHA3_384 = "2.16.840.1.101.3.4.3.11"; //$NON-NLS-1$
    private static final String OID_ECDSA_SHA3_512 = "2.16.840.1.101.3.4.3.12"; //$NON-NLS-1$

    private static final Dictionary<String, String> OIDS = new Hashtable<String, String>();
    static {

        OIDS.put(OID_ECDSA_SHA1, "SHA1withECDSA"); //$NON-NLS-1$

        OIDS.put(OID_ECDSA_SHA224, "SHA224withECDSA"); //$NON-NLS-1$
        OIDS.put(OID_ECDSA_SHA256, "SHA256withECDSA"); //$NON-NLS-1$
        OIDS.put(OID_ECDSA_SHA384, "SHA384withECDSA"); //$NON-NLS-1$
        OIDS.put(OID_ECDSA_SHA512, "SHA512withECDSA"); //$NON-NLS-1$

        OIDS.put(OID_RSA_SHA3_224, "SHA3-224withRSA"); //$NON-NLS-1$
        OIDS.put(OID_RSA_SHA3_256, "SHA3-256withRSA"); //$NON-NLS-1$
        OIDS.put(OID_RSA_SHA3_384, "SHA3-384withRSA"); //$NON-NLS-1$
        OIDS.put(OID_RSA_SHA3_512, "SHA3-512withRSA"); //$NON-NLS-1$

        OIDS.put(OID_ECDSA_SHA3_224, "SHA3-224withECDSA"); //$NON-NLS-1$
        OIDS.put(OID_ECDSA_SHA3_256, "SHA3-256withECDSA"); //$NON-NLS-1$
        OIDS.put(OID_ECDSA_SHA3_384, "SHA3-384withECDSA"); //$NON-NLS-1$
        OIDS.put(OID_ECDSA_SHA3_512, "SHA3-512withECDSA"); //$NON-NLS-1$

    }

    //*********************** Fin modificaciones para el soporte de ECDSA **************************
    //**********************************************************************************************
}
