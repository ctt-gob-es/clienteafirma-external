/*
 * $Id: PdfObject.java 3912 2009-04-26 08:38:15Z blowagie $
 *
 * Copyright 1999, 2000, 2001, 2002 Bruno Lowagie
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

package com.lowagie.text.pdf;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <CODE>PdfObject</CODE> is the abstract superclass of all PDF objects.
 * <P>
 * PDF supports seven basic types of objects: Booleans, numbers, strings, names,
 * arrays, dictionaries and streams. In addition, PDF provides a null object.
 * Objects may be labeled so that they can be referred to by other objects.<BR>
 * All these basic PDF objects are described in the 'Portable Document Format
 * Reference Manual version 1.3' Chapter 4 (pages 37-54).
 *
 * @see		PdfNull
 * @see		PdfBoolean
 * @see		PdfNumber
 * @see		PdfString
 * @see		PdfName
 * @see		PdfArray
 * @see		PdfDictionary
 * @see		PdfStream
 * @see		PdfIndirectReference
 */
public abstract class PdfObject {

    // CONSTANTS

    /** A possible type of <CODE>PdfObject</CODE> */
    static final int BOOLEAN = 1;

    /** A possible type of <CODE>PdfObject</CODE> */
    static final int NUMBER = 2;

    /** A possible type of <CODE>PdfObject</CODE> */
    static final int STRING = 3;

    /** A possible type of <CODE>PdfObject</CODE> */
    static final int NAME = 4;

    /** A possible type of <CODE>PdfObject</CODE> */
    public static final int ARRAY = 5;

    /** A possible type of <CODE>PdfObject</CODE> */
    static final int DICTIONARY = 6;

    /** A possible type of <CODE>PdfObject</CODE> */
    public static final int STREAM = 7;

    /** A possible type of <CODE>PdfObject</CODE> */
    static final int NULL = 8;

    /** A possible type of <CODE>PdfObject</CODE> */
    public static final int INDIRECT = 10;

    /** An empty string used for the <CODE>PdfNull</CODE>-object and for an empty <CODE>PdfString</CODE>-object. */
    static final String NOTHING = "";

    /**
     * This is the default encoding to be used for converting Strings into
     * bytes and vice versa. The default encoding is PdfDocEncoding.
     */
    static final String TEXT_PDFDOCENCODING = "PDF";

    /** This is the encoding to be used to output text in Unicode. */
    public static final String TEXT_UNICODE = "UnicodeBig";

    // CLASS VARIABLES

    /** The content of this <CODE>PdfObject</CODE> */
    protected byte[] bytes;

    /** The type of this <CODE>PdfObject</CODE> */
    protected int type;

    /** Holds the indirect reference. */
    private PRIndirectReference indRef;

    // CONSTRUCTORS

    /**
     * Constructs a <CODE>PdfObject</CODE> of a certain <VAR>type</VAR>
     * without any <VAR>content</VAR>.
     *
     * @param type    type of the new <CODE>PdfObject</CODE>
     */
    protected PdfObject(final int type) {
        this.type = type;
    }

    /**
     * Constructs a <CODE>PdfObject</CODE> of a certain <VAR>type</VAR>
     * with a certain <VAR>content</VAR>.
     *
     * @param type     type of the new <CODE>PdfObject</CODE>
     * @param content  content of the new <CODE>PdfObject</CODE> as a
     *   <CODE>String</CODE>.
     */
    protected PdfObject(final int type, final String content) {
        this.type = type;
        this.bytes = PdfEncodings.convertToBytes(content, null);
    }

    /**
     * Constructs a <CODE>PdfObject</CODE> of a certain <VAR>type</VAR>
     * with a certain <VAR>content</VAR>.
     *
     * @param type   type of the new <CODE>PdfObject</CODE>
     * @param bytes  content of the new <CODE>PdfObject</CODE> as an array of
     *   <CODE>byte</CODE>.
     */
    protected PdfObject(final int type, final byte[] bytes) {
        this.bytes = bytes;
        this.type = type;
    }

    // methods dealing with the content of this object

    /**
     * Writes the PDF representation of this <CODE>PdfObject</CODE> as an
     * array of <CODE>byte</CODE>s to the writer.
     *
     * @param writer for backwards compatibility
     * @param os     The <CODE>OutputStream</CODE> to write the bytes to.
     * @throws IOException
     */
    public void toPdf(final PdfWriter writer, final OutputStream os) throws IOException {
        if (this.bytes != null) {
			os.write(this.bytes);
		}
    }

    /**
     * Returns the <CODE>String</CODE>-representation of this
     * <CODE>PdfObject</CODE>.
     *
     * @return    a <CODE>String</CODE>
     */
    @Override
	public String toString() {
        if (this.bytes == null) {
			return super.toString();
		}
        return PdfEncodings.convertToString(this.bytes, null);
    }

    /**
     * Gets the presentation of this object in a byte array
     *
     * @return a byte array
     */
    public byte[] getBytes() {
        return this.bytes;
    }

    /**
     * Whether this object can be contained in an object stream.
     *
     * PdfObjects of type STREAM OR INDIRECT can not be contained in an
     * object stream.
     *
     * @return <CODE>true</CODE> if this object can be in an object stream.
     *   Otherwise <CODE>false</CODE>
     */
    boolean canBeInObjStm() {
        switch (this.type) {
            case NULL:
            case BOOLEAN:
            case NUMBER:
            case STRING:
            case NAME:
            case ARRAY:
            case DICTIONARY:
                return true;
            case STREAM:
            case INDIRECT:
            default:
                return false;
        }
    }

    /**
     * Changes the content of this <CODE>PdfObject</CODE>.
     *
     * @param content    the new content of this <CODE>PdfObject</CODE>
     */
    protected void setContent(final String content) {
        this.bytes = PdfEncodings.convertToBytes(content, null);
    }

    // methods dealing with the type of this object

    /**
     * Returns the type of this <CODE>PdfObject</CODE>.
     *
     * May be either of:
     * - <VAR>NULL</VAR>: A <CODE>PdfNull</CODE>
     * - <VAR>BOOLEAN</VAR>: A <CODE>PdfBoolean</CODE>
     * - <VAR>NUMBER</VAR>: A <CODE>PdfNumber</CODE>
     * - <VAR>STRING</VAR>: A <CODE>PdfString</CODE>
     * - <VAR>NAME</VAR>: A <CODE>PdfName</CODE>
     * - <VAR>ARRAY</VAR>: A <CODE>PdfArray</CODE>
     * - <VAR>DICTIONARY</VAR>: A <CODE>PdfDictionary</CODE>
     * - <VAR>STREAM</VAR>: A <CODE>PdfStream</CODE>
     * - <VAR>INDIRECT</VAR>: ><CODE>PdfIndirectObject</CODE>
     *
     * @return The type
     */
    public int type() {
        return this.type;
    }

    /**
     * Checks if this <CODE>PdfObject</CODE> is of the type
     * <CODE>PdfNull</CODE>.
     *
     * @return <CODE>true</CODE> or <CODE>false</CODE>
     */
    public boolean isNull() {
        return this.type == NULL;
    }

    /**
     * Checks if this <CODE>PdfObject</CODE> is of the type
     * <CODE>PdfBoolean</CODE>.
     *
     * @return <CODE>true</CODE> or <CODE>false</CODE>
     */
    public boolean isBoolean() {
        return this.type == BOOLEAN;
    }

    /**
     * Checks if this <CODE>PdfObject</CODE> is of the type
     * <CODE>PdfNumber</CODE>.
     *
     * @return <CODE>true</CODE> or <CODE>false</CODE>
     */
    public boolean isNumber() {
        return this.type == NUMBER;
    }

    /**
     * Checks if this <CODE>PdfObject</CODE> is of the type
     * <CODE>PdfString</CODE>.
     *
     * @return <CODE>true</CODE> or <CODE>false</CODE>
     */
    public boolean isString() {
        return this.type == STRING;
    }

    /**
     * Checks if this <CODE>PdfObject</CODE> is of the type
     * <CODE>PdfName</CODE>.
     *
     * @return <CODE>true</CODE> or <CODE>false</CODE>
     */
    public boolean isName() {
        return this.type == NAME;
    }

    /**
     * Checks if this <CODE>PdfObject</CODE> is of the type
     * <CODE>PdfArray</CODE>.
     *
     * @return <CODE>true</CODE> or <CODE>false</CODE>
     */
    public boolean isArray() {
        return this.type == ARRAY;
    }

    /**
     * Checks if this <CODE>PdfObject</CODE> is of the type
     * <CODE>PdfDictionary</CODE>.
     *
     * @return <CODE>true</CODE> or <CODE>false</CODE>
     */
    public boolean isDictionary() {
        return this.type == DICTIONARY;
    }

    /**
     * Checks if this <CODE>PdfObject</CODE> is of the type
     * <CODE>PdfStream</CODE>.
     *
     * @return <CODE>true</CODE> or <CODE>false</CODE>
     */
    public boolean isStream() {
        return this.type == STREAM;
    }

    /**
     * Checks if this <CODE>PdfObject</CODE> is of the type
     * <CODE>PdfIndirectObject</CODE>.
     *
     * @return <CODE>true</CODE> if this is an indirect object,
     *   otherwise <CODE>false</CODE>
     */
    public boolean isIndirect() {
        return this.type == INDIRECT;
    }

    /**
     * Get the indirect reference
     *
     * @return A <CODE>PdfIndirectReference</CODE>
     */
    public PRIndirectReference getIndRef() {
        return this.indRef;
    }

    /**
     * Set the indirect reference
     *
     * @param indRef New value as a <CODE>PdfIndirectReference</CODE>
     */
    public void setIndRef(final PRIndirectReference indRef) {
        this.indRef = indRef;
    }
}
