/*
 * $Id: PdfRectangle.java 3694 2009-02-17 19:29:05Z mstorer $
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

package com.aowagie.text.pdf;

import com.aowagie.text.Rectangle;

/**
 * <CODE>PdfRectangle</CODE> is the PDF Rectangle object.
 * <P>
 * Rectangles are used to describe locations on the page and bounding boxes for several
 * objects in PDF, such as fonts. A rectangle is represented as an <CODE>array</CODE> of
 * four numbers, specifying the lower left <I>x</I>, lower left <I>y</I>, upper right <I>x</I>,
 * and upper right <I>y</I> coordinates of the rectangle, in that order.<BR>
 * This object is described in the 'Portable Document Format Reference Manual version 1.3'
 * section 7.1 (page 183).
 *
 * @see		com.aowagie.text.Rectangle
 * @see		PdfArray
 */

public class PdfRectangle extends PdfArray {

    // membervariables

/** lower left x */
    private float llx = 0;

/** lower left y */
    private float lly = 0;

/** upper right x */
    private float urx = 0;

/** upper right y */
    private float ury = 0;

    // constructors

/**
 * Constructs a <CODE>PdfRectangle</CODE>-object.
 *
 * @param		llx			lower left x
 * @param		lly			lower left y
 * @param		urx			upper right x
 * @param		ury			upper right y
 *
 * @since		rugPdf0.10
 */

    private PdfRectangle(final float llx, final float lly, final float urx, final float ury, final int rotation) {
        super();
        if (rotation == 90 || rotation == 270) {
            this.llx = lly;
            this.lly = llx;
            this.urx = ury;
            this.ury = urx;
        }
        else {
            this.llx = llx;
            this.lly = lly;
            this.urx = urx;
            this.ury = ury;
        }
        super.add(new PdfNumber(this.llx));
        super.add(new PdfNumber(this.lly));
        super.add(new PdfNumber(this.urx));
        super.add(new PdfNumber(this.ury));
    }

    public PdfRectangle(final float llx, final float lly, final float urx, final float ury) {
        this(llx, lly, urx, ury, 0);
    }





/**
 * Constructs a <CODE>PdfRectangle</CODE>-object with a <CODE>Rectangle</CODE>-object.
 *
 * @param	rectangle	a <CODE>Rectangle</CODE>
 */

    PdfRectangle(final Rectangle rectangle, final int rotation) {
        this(rectangle.getLeft(), rectangle.getBottom(), rectangle.getRight(), rectangle.getTop(), rotation);
    }

    PdfRectangle(final Rectangle rectangle) {
        this(rectangle.getLeft(), rectangle.getBottom(), rectangle.getRight(), rectangle.getTop(), 0);
    }

    // methods
    /**
     * Returns the high level version of this PdfRectangle
     * @return this PdfRectangle translated to class Rectangle
     */
    public Rectangle getRectangle() {
    	return new Rectangle(left(), bottom(), right(), top());
    }

/**
 * Overrides the <CODE>add</CODE>-method in <CODE>PdfArray</CODE> in order to prevent the adding of extra object to the array.
 *
 * @param		object			<CODE>PdfObject</CODE> to add (will not be added here)
 * @return		<CODE>false</CODE>
 */

    @Override
	public boolean add(final PdfObject object) {
        return false;
    }

    /**
     * Block changes to the underlying PdfArray
     * @param values stuff we'll ignore.  Ha!
     * @return false.  You can't add anything to a PdfRectangle
     * @since 2.1.5
     */

    @Override
	public boolean add( final float values[] ) {
        return false;
    }

    /**
     * Block changes to the underlying PdfArray
     * @param values stuff we'll ignore.  Ha!
     * @return false.  You can't add anything to a PdfRectangle
     * @since 2.1.5
     */

    @Override
	public boolean add( final int values[] ) {
        return false;
    }

    /**
     * Block changes to the underlying PdfArray
     * @param object Ignored.
     * @since 2.1.5
     */

    @Override
	public void addFirst( final PdfObject object ) {
    }
/**
 * Returns the lower left x-coordinate.
 *
 * @return		the lower left x-coordinate
 */

    public float left() {
        return this.llx;
    }

/**
 * Returns the upper right x-coordinate.
 *
 * @return		the upper right x-coordinate
 */

    public float right() {
        return this.urx;
    }

/**
 * Returns the upper right y-coordinate.
 *
 * @return		the upper right y-coordinate
 */

    public float top() {
        return this.ury;
    }

/**
 * Returns the lower left y-coordinate.
 *
 * @return		the lower left y-coordinate
 */

    public float bottom() {
        return this.lly;
    }


}