/*
 * $Id: HeaderFooter.java 3373 2008-05-12 16:21:24Z xlv $
 *
 * Copyright 1999, 2000, 2001, 2002 by Bruno Lowagie.
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

package com.lowagie.text;


/**
 * A <CODE>HeaderFooter</CODE>-object is a <CODE>Rectangle</CODe> with text
 * that can be put above and/or below every page.
 * <P>
 * Example:
 * <BLOCKQUOTE><PRE>
 * <STRONG>HeaderFooter header = new HeaderFooter(new Phrase("This is a header."), false);</STRONG>
 * <STRONG>HeaderFooter footer = new HeaderFooter(new Phrase("This is page "), new Phrase("."));</STRONG>
 * document.setHeader(header);
 * document.setFooter(footer);
 * </PRE></BLOCKQUOTE>
 */

public class HeaderFooter extends Rectangle {

    // membervariables

/** Does the page contain a pagenumber? */
    private final boolean numbered;

/** This is the <CODE>Phrase</CODE> that comes before the pagenumber. */
    private Phrase before = null;

/** This is number of the page. */
    private int pageN;

/** This is the <CODE>Phrase</CODE> that comes after the pagenumber. */
    private final Phrase after = null;

/** This is alignment of the header/footer. */
    private int alignment;

    // constructors


/**
 * Constructs a <CODE>Header</CODE>-object with a pagenumber at the end.
 *
 * @param	before		the <CODE>Phrase</CODE> before the pagenumber
 * @param	numbered	<CODE>true</CODE> if the page has to be numbered
 */

    public HeaderFooter(final Phrase before, final boolean numbered) {
        super(0, 0, 0, 0);
        setBorder(TOP + BOTTOM);
        setBorderWidth(1);

        this.numbered = numbered;
        this.before = before;
    }

    // methods

/**
 * Checks if the HeaderFooter contains a page number.
 *
 * @return  true if the page has to be numbered
 */

    public boolean isNumbered() {
        return this.numbered;
    }

/**
 * Gets the part that comes before the pageNumber.
 *
 * @return  a Phrase
 */

    public Phrase getBefore() {
        return this.before;
    }

/**
 * Gets the part that comes after the pageNumber.
 *
 * @return  a Phrase
 */

    public Phrase getAfter() {
        return this.after;
    }

/**
 * Sets the page number.
 *
 * @param		pageN		the new page number
 */

    public void setPageNumber(final int pageN) {
        this.pageN = pageN;
    }

/**
 * Sets the alignment.
 *
 * @param		alignment	the new alignment
 */

    public void setAlignment(final int alignment) {
        this.alignment = alignment;
    }

    // methods to retrieve the membervariables

/**
 * Gets the <CODE>Paragraph</CODE> that can be used as header or footer.
 *
 * @return		a <CODE>Paragraph</CODE>
 */

    public Paragraph paragraph() {
        final Paragraph paragraph = new Paragraph(this.before.getLeading());
        paragraph.add(this.before);
        if (this.numbered) {
            paragraph.addSpecial(new Chunk(String.valueOf(this.pageN), this.before.getFont()));
        }
        if (this.after != null) {
            paragraph.addSpecial(this.after);
        }
        paragraph.setAlignment(this.alignment);
        return paragraph;
    }

}