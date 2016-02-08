/*
 * $Id: VerticalPositionMark.java 3373 2008-05-12 16:21:24Z xlv $
 *
 * Copyright 2008 by Bruno Lowagie
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

package com.aowagie.text.pdf.draw;

import java.util.ArrayList;

import com.aowagie.text.Chunk;
import com.aowagie.text.DocumentException;
import com.aowagie.text.Element;
import com.aowagie.text.ElementListener;
import com.aowagie.text.pdf.PdfContentByte;

/**
 * Helper class implementing the DrawInterface. Can be used to add
 * horizontal or vertical separators. Won't draw anything unless
 * you implement the draw method.
 * @since	2.1.2
 */

public class VerticalPositionMark implements DrawInterface, Element {

    /** Another implementation of the DrawInterface; its draw method will overrule LineSeparator.draw(). */
    protected DrawInterface drawInterface = null;

    /** The offset for the line. */
    protected float offset = 0;

	/**
	 * Creates a vertical position mark that won't draw anything unless
	 * you define a DrawInterface.
	 */
	public VerticalPositionMark() {
	}

	/**
	 * Creates a vertical position mark that won't draw anything unless
	 * you define a DrawInterface.
	 * @param	drawInterface	the drawInterface for this vertical position mark.
	 * @param	offset			the offset for this vertical position mark.
	 */
	public VerticalPositionMark(final DrawInterface drawInterface, final float offset) {
		this.drawInterface = drawInterface;
		this.offset = offset;
	}

	/**
	 * @see com.aowagie.text.pdf.draw.DrawInterface#draw(com.aowagie.text.pdf.PdfContentByte, float, float, float, float, float)
	 */
	@Override
	public void draw(final PdfContentByte canvas, final float llx, final float lly, final float urx, final float ury, final float y) {
		if (this.drawInterface != null) {
			this.drawInterface.draw(canvas, llx, lly, urx, ury, y + this.offset);
		}
	}

    /**
     * @see com.aowagie.text.Element#process(com.aowagie.text.ElementListener)
     */
    @Override
	public boolean process(final ElementListener listener) {
		try {
			return listener.add(this);
		} catch (final DocumentException e) {
			return false;
		}
    }

    /**
     * @see com.aowagie.text.Element#type()
     */
    @Override
	public int type() {
        return Element.YMARK;
    }

    /**
     * @see com.aowagie.text.Element#isContent()
     */
    @Override
	public boolean isContent() {
        return true;
    }

    /**
     * @see com.aowagie.text.Element#isNestable()
     */
    @Override
	public boolean isNestable() {
        return false;
    }

    /**
     * @see com.aowagie.text.Element#getChunks()
     */
    @Override
	public ArrayList getChunks() {
    	final ArrayList list = new ArrayList();
    	list.add(new Chunk(this, true));
        return list;
    }

    /**
     * Getter for the interface with the overruling draw() method.
     * @return	a DrawInterface implementation
     */
    public DrawInterface getDrawInterface() {
        return this.drawInterface;
    }

    /**
     * Setter for the interface with the overruling draw() method.
     * @param drawInterface a DrawInterface implementation
     */
    public void setDrawInterface(final DrawInterface drawInterface) {
        this.drawInterface = drawInterface;
    }

    /**
     * Getter for the offset relative to the baseline of the current line.
     * @return	an offset
     */
    public float getOffset() {
        return this.offset;
    }

    /**
     * Setter for the offset. The offset is relative to the current
     * Y position. If you want to underline something, you have to
     * choose a negative offset.
     * @param offset	an offset
     */
    public void setOffset(final float offset) {
        this.offset = offset;
    }
}
