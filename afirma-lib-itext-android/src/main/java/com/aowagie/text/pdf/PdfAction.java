/*
 * $Id: PdfAction.java 3912 2009-04-26 08:38:15Z blowagie $
 *
 * Copyright 2000 by Bruno Lowagie.
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

import java.io.IOException;
import java.net.URL;

import com.aowagie.text.pdf.collection.PdfTargetDictionary;

/**
 * A <CODE>PdfAction</CODE> defines an action that can be triggered from a PDF file.
 *
 * @see		PdfDictionary
 */

public class PdfAction extends PdfDictionary {

    /** A named action to go to the first page.
     */
    private static final int FIRSTPAGE = 1;
    /** A named action to go to the previous page.
     */
    private static final int PREVPAGE = 2;
    /** A named action to go to the next page.
     */
    private static final int NEXTPAGE = 3;
    /** A named action to go to the last page.
     */
    private static final int LASTPAGE = 4;

    /** A named action to open a print dialog.
     */
    private static final int PRINTDIALOG = 5;
















    // constructors

    /** Create an empty action.
     */
    public PdfAction() {
    }

    /**
     * Constructs a new <CODE>PdfAction</CODE> of Subtype URI.
     *
     * @param url the Url to go to
     */

    public PdfAction(final URL url) {
        this(url.toExternalForm());
    }



    /**
     * Constructs a new <CODE>PdfAction</CODE> of Subtype URI.
     *
     * @param url the url to go to
     */

    public PdfAction(final String url) {
        this(url, false);
    }

    /**
     * Construct a new <CODE>PdfAction</CODE> of Subtype URI that accepts the x and y coordinate of the position that was clicked.
     * @param url URL
     * @param isMap Is map
     */

    private PdfAction(final String url, final boolean isMap) {
        put(PdfName.S, PdfName.URI);
        put(PdfName.URI, new PdfString(url));
        if (isMap) {
			put(PdfName.ISMAP, PdfBoolean.PDFTRUE);
		}
    }

    /**
     * Constructs a new <CODE>PdfAction</CODE> of Subtype GoTo.
     * @param destination the destination to go to
     */

    PdfAction(final PdfIndirectReference destination) {
        put(PdfName.S, PdfName.GOTO);
        put(PdfName.D, destination);
    }

    /**
     * Constructs a new <CODE>PdfAction</CODE> of Subtype GoToR.
     * @param filename the file name to go to
     * @param name the named destination to go to
     */

    public PdfAction(final String filename, final String name) {
        put(PdfName.S, PdfName.GOTOR);
        put(PdfName.F, new PdfString(filename));
        put(PdfName.D, new PdfString(name));
    }

    /**
     * Constructs a new <CODE>PdfAction</CODE> of Subtype GoToR.
     * @param filename the file name to go to
     * @param page the page destination to go to
     */

    public PdfAction(final String filename, final int page) {
        put(PdfName.S, PdfName.GOTOR);
        put(PdfName.F, new PdfString(filename));
        put(PdfName.D, new PdfLiteral("[" + (page - 1) + " /FitH 10000]"));
    }

    /** Implements name actions. The action can be FIRSTPAGE, LASTPAGE,
     * NEXTPAGE, PREVPAGE and PRINTDIALOG.
     * @param named the named action
     */
    public PdfAction(final int named) {
        put(PdfName.S, PdfName.NAMED);
        switch (named) {
            case FIRSTPAGE:
                put(PdfName.N, PdfName.FIRSTPAGE);
                break;
            case LASTPAGE:
                put(PdfName.N, PdfName.LASTPAGE);
                break;
            case NEXTPAGE:
                put(PdfName.N, PdfName.NEXTPAGE);
                break;
            case PREVPAGE:
                put(PdfName.N, PdfName.PREVPAGE);
                break;
            case PRINTDIALOG:
                put(PdfName.S, PdfName.JAVASCRIPT);
                put(PdfName.JS, new PdfString("this.print(true);\r"));
                break;
            default:
                throw new RuntimeException("Invalid named action.");
        }
    }

    /** Launches an application or a document.
     * @param application the application to be launched or the document to be opened or printed.
     * @param parameters (Windows-specific) A parameter string to be passed to the application.
     * It can be <CODE>null</CODE>.
     * @param operation (Windows-specific) the operation to perform: "open" - Open a document,
     * "print" - Print a document.
     * It can be <CODE>null</CODE>.
     * @param defaultDir (Windows-specific) the default directory in standard DOS syntax.
     * It can be <CODE>null</CODE>.
     */
    public PdfAction(final String application, final String parameters, final String operation, final String defaultDir) {
        put(PdfName.S, PdfName.LAUNCH);
        if (parameters == null && operation == null && defaultDir == null) {
			put(PdfName.F, new PdfString(application));
		} else {
            final PdfDictionary dic = new PdfDictionary();
            dic.put(PdfName.F, new PdfString(application));
            if (parameters != null) {
				dic.put(PdfName.P, new PdfString(parameters));
			}
            if (operation != null) {
				dic.put(PdfName.O, new PdfString(operation));
			}
            if (defaultDir != null) {
				dic.put(PdfName.D, new PdfString(defaultDir));
			}
            put(PdfName.WIN, dic);
        }
    }



     /**Creates a Rendition action
     * @param file File name
     * @param fs File specification
     * @param mimeType MimeType
     * @param ref Ref
     * @return a Media Clip action
     * @throws IOException on error
     */
    static PdfAction rendition(final String file, final PdfFileSpecification fs, final String mimeType, final PdfIndirectReference ref) throws IOException {
        final PdfAction js = new PdfAction();
        js.put(PdfName.S, PdfName.RENDITION);
        js.put(PdfName.R, new PdfRendition(file, fs, mimeType));
        js.put(new PdfName("OP"), new PdfNumber(0));
        js.put(new PdfName("AN"), ref);
        return js;
     }

    /** Creates a JavaScript action. If the JavaScript is smaller than
     * 50 characters it will be placed as a string, otherwise it will
     * be placed as a compressed stream.
     * @param code the JavaScript code
     * @param writer the writer for this action
     * @param unicode select JavaScript unicode. Note that the internal
     * Acrobat JavaScript engine does not support unicode,
     * so this may or may not work for you
     * @return the JavaScript action
     */
    static PdfAction javaScript(final String code, final PdfWriter writer, final boolean unicode) {
        final PdfAction js = new PdfAction();
        js.put(PdfName.S, PdfName.JAVASCRIPT);
        if (unicode && code.length() < 50) {
                js.put(PdfName.JS, new PdfString(code, PdfObject.TEXT_UNICODE));
        }
        else if (!unicode && code.length() < 100) {
                js.put(PdfName.JS, new PdfString(code));
        }
        else {
            try {
                final byte b[] = PdfEncodings.convertToBytes(code, unicode ? PdfObject.TEXT_UNICODE : PdfObject.TEXT_PDFDOCENCODING);
                final PdfStream stream = new PdfStream(b);
                stream.flateCompress(writer.getCompressionLevel());
                js.put(PdfName.JS, writer.addToBody(stream).getIndirectReference());
            }
            catch (final Exception e) {
                js.put(PdfName.JS, new PdfString(code));
            }
        }
        return js;
    }



    /**
     * A Hide action hides or shows an object.
     * @param obj object to hide or show
     * @param hide true is hide, false is show
     * @return a Hide Action
     */
    private static PdfAction createHide(final PdfObject obj, final boolean hide) {
        final PdfAction action = new PdfAction();
        action.put(PdfName.S, PdfName.HIDE);
        action.put(PdfName.T, obj);
        if (!hide) {
			action.put(PdfName.H, PdfBoolean.PDFFALSE);
		}
        return action;
    }





    private static PdfArray buildArray(final Object names[]) {
        final PdfArray array = new PdfArray();
        for (final Object obj : names) {
            if (obj instanceof String) {
				array.add(new PdfString((String)obj));
			} else if (obj instanceof PdfAnnotation) {
				array.add(((PdfAnnotation)obj).getIndirectReference());
			} else {
				throw new RuntimeException("The array must contain String or PdfAnnotation.");
			}
        }
        return array;
    }







    /**
     * Creates a GoToE action to an embedded file.
     * @param filename	the root document of the target (null if the target is in the same document)
     * @param target	a path to the target document of this action
     * @param dest		the destination inside the target document, can be of type PdfDestination, PdfName, or PdfString
     * @param newWindow	if true, the destination document should be opened in a new window
     * @return a GoToE action
     */
    private static PdfAction gotoEmbedded(final String filename, final PdfTargetDictionary target, final PdfObject dest, final boolean newWindow) {
    	final PdfAction action = new PdfAction();
    	action.put(PdfName.S, PdfName.GOTOE);
    	action.put(PdfName.T, target);
    	action.put(PdfName.D, dest);
    	action.put(PdfName.NEWWINDOW, new PdfBoolean(newWindow));
    	if (filename != null) {
    		action.put(PdfName.F, new PdfString(filename));
    	}
    	return action;
    }


}
