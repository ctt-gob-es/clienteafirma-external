/*
 * $Id: MultiColumnText.java 3936 2009-05-27 12:40:12Z blowagie $
 *
 * Copyright 2004 Steve Appling
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
 * the Initial Developer are Copyright (C) 1999-2005 by Bruno Lowagie.
 * All Rights Reserved.
 * Co-Developer of the code is Paulo Soares. Portions created by the Co-Developer
 * are Copyright (C) 2000-2005 by Paulo Soares. All Rights Reserved.
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

import java.util.ArrayList;

import com.lowagie.text.Chunk;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.ElementListener;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;

/**
 * Formats content into one or more columns bounded by a
 * rectangle.  The columns may be simple rectangles or
 * more complicated shapes. Add all of the columns before
 * adding content. Column continuation is supported. A MultiColumnText object may be added to
 * a document using <CODE>Document.add</CODE>.
 * @author Steve Appling
 */
public class MultiColumnText implements Element {

    /** special constant for automatic calculation of height */
    public static final float AUTOMATIC = -1f;

    /**
     * total desiredHeight of columns.  If <CODE>AUTOMATIC</CODE>, this means fill pages until done.
     * This may be larger than one page
     */
    private final float desiredHeight;

    /**
     * total height of element written out so far
     */
    private float totalHeight;

    /**
     * true if all the text could not be written out due to height restriction
     */
    private boolean overflow;

    /**
     * Top of the columns - y position on starting page.
     * If <CODE>AUTOMATIC</CODE>, it means current y position when added to document
     */
    private float top;

    /**
     * ColumnText object used to do all the real work.  This same object is used for all columns
     */
    private final ColumnText columnText;

    /**
     * Array of <CODE>ColumnDef</CODE> objects used to define the columns
     */
    private final ArrayList columnDefs;

    /**
     * true if all columns are simple (rectangular)
     */
    private boolean simple = true;

    private int currentColumn = 0;

    private float nextY = AUTOMATIC;

    private boolean columnsRightToLeft = false;

    private PdfDocument document;
    /**
     * Default constructor.  Sets height to <CODE>AUTOMATIC</CODE>.
     * Columns will repeat on each page as necessary to accommodate content length.
     */
    public MultiColumnText() {
        this(AUTOMATIC);
    }

    /**
     * Construct a MultiColumnText container of the specified height.
     * If height is <CODE>AUTOMATIC</CODE>, fill complete pages until done.
     * If a specific height is used, it may span one or more pages.
     *
     * @param height
     */
    public MultiColumnText(final float height) {
        this.columnDefs = new ArrayList();
        this.desiredHeight = height;
        this.top = AUTOMATIC;
        // canvas will be set later
        this.columnText = new ColumnText(null);
        this.totalHeight = 0f;
    }

    /**
     * Construct a MultiColumnText container of the specified height
     * starting at the specified Y position.
     *
     * @param height
     * @param top
     */
    public MultiColumnText(final float top, final float height) {
        this.columnDefs = new ArrayList();
        this.desiredHeight = height;
        this.top = top;
        this.nextY = top;
        // canvas will be set later
        this.columnText = new ColumnText(null);
        this.totalHeight = 0f;
    }

    /**
     * Indicates that all of the text did not fit in the
     * specified height. Note that isOverflow will return
     * false before the MultiColumnText object has been
     * added to the document.  It will always be false if
     * the height is AUTOMATIC.
     *
     * @return true if there is still space left in the column
     */
    public boolean isOverflow() {
        return this.overflow;
    }

    /**
     * Copy the parameters from the specified ColumnText to use
     * when rendering.  Parameters like <CODE>setArabicOptions</CODE>
     * must be set in this way.
     *
     * @param sourceColumn
     */
    public void useColumnParams(final ColumnText sourceColumn) {
        // note that canvas will be overwritten later
        this.columnText.setSimpleVars(sourceColumn);
    }

    /**
     * Add a new column.  The parameters are limits for each column
     * wall in the format of a sequence of points (x1,y1,x2,y2,...).
     *
     * @param left  limits for left column
     * @param right limits for right column
     */
    public void addColumn(final float[] left, final float[] right) {
        final ColumnDef nextDef = new ColumnDef(left, right);
        if (!nextDef.isSimple()) {
			this.simple = false;
		}
        this.columnDefs.add(nextDef);
    }

    /**
     * Add a simple rectangular column with specified left
     * and right x position boundaries.
     *
     * @param left  left boundary
     * @param right right boundary
     */
    public void addSimpleColumn(final float left, final float right) {
        final ColumnDef newCol = new ColumnDef(left, right);
        this.columnDefs.add(newCol);
    }

    /**
     * Add the specified number of evenly spaced rectangular columns.
     * Columns will be separated by the specified gutterWidth.
     *
     * @param left        left boundary of first column
     * @param right       right boundary of last column
     * @param gutterWidth width of gutter spacing between columns
     * @param numColumns  number of columns to add
     */
    public void addRegularColumns(final float left, final float right, final float gutterWidth, final int numColumns) {
        float currX = left;
        final float width = right - left;
        final float colWidth = (width - gutterWidth * (numColumns - 1)) / numColumns;
        for (int i = 0; i < numColumns; i++) {
            addSimpleColumn(currX, currX + colWidth);
            currX += colWidth + gutterWidth;
        }
    }

    /**
     * Adds a <CODE>Phrase</CODE> to the current text array.
     * Will not have any effect if addElement() was called before.
     * @param phrase the text
     * @since	2.1.5
     */
    public void addText(final Phrase phrase) {
    	this.columnText.addText(phrase);
    }

    /**
     * Adds a <CODE>Chunk</CODE> to the current text array.
     * Will not have any effect if addElement() was called before.
     * @param chunk the text
     * @since	2.1.5
     */
    public void addText(final Chunk chunk) {
    	this.columnText.addText(chunk);
    }

    /**
     * Add an element to be rendered in a column.
     * Note that you can only add a <CODE>Phrase</CODE>
     * or a <CODE>Chunk</CODE> if the columns are
     * not all simple.  This is an underlying restriction in
     * {@link com.lowagie.text.pdf.ColumnText}
     *
     * @param element element to add
     * @throws DocumentException if element can't be added
     */
    public void addElement(final Element element) throws DocumentException {
        if (this.simple) {
            this.columnText.addElement(element);
        } else if (element instanceof Phrase) {
            this.columnText.addText((Phrase) element);
        } else if (element instanceof Chunk) {
            this.columnText.addText((Chunk) element);
        } else {
            throw new DocumentException("Can't add " + element.getClass() + " to MultiColumnText with complex columns");
        }
    }


    /**
     * Write out the columns.  After writing, use
     * {@link #isOverflow()} to see if all text was written.
     * @param canvas PdfContentByte to write with
     * @param document document to write to (only used to get page limit info)
     * @param documentY starting y position to begin writing at
     * @return the current height (y position) after writing the columns
     * @throws DocumentException on error
     */
    public float write(final PdfContentByte canvas, final PdfDocument document, float documentY) throws DocumentException {
        this.document = document;
        this.columnText.setCanvas(canvas);
        if (this.columnDefs.isEmpty()) {
            throw new DocumentException("MultiColumnText has no columns");
        }
        this.overflow = false;
        float currentHeight = 0;
        boolean done = false;
        try {
            while (!done) {
                if (this.top == AUTOMATIC) {
                    this.top = document.getVerticalPosition(true); // RS - 07/07/2005 - Get current doc writing position for top of columns on new page.
                }
                else if (this.nextY == AUTOMATIC) {
                    this.nextY = document.getVerticalPosition(true); // RS - 07/07/2005 - - Get current doc writing position for top of columns on new page.
                }
                final ColumnDef currentDef = (ColumnDef) this.columnDefs.get(getCurrentColumn());
                this.columnText.setYLine(this.top);

                float[] left = currentDef.resolvePositions(Rectangle.LEFT);
                float[] right = currentDef.resolvePositions(Rectangle.RIGHT);
                if (document.isMarginMirroring() && document.getPageNumber() % 2 == 0){
                    final float delta = document.rightMargin() - document.left();
                    left = left.clone();
                    right = right.clone();
                    for (int i = 0; i < left.length; i += 2) {
                        left[i] -= delta;
                    }
                    for (int i = 0; i < right.length; i += 2) {
                        right[i] -= delta;
                    }
                }

                currentHeight = Math.max(currentHeight, getHeight(left, right));

                if (currentDef.isSimple()) {
                    this.columnText.setSimpleColumn(left[2], left[3], right[0], right[1]);
                } else {
                    this.columnText.setColumns(left, right);
                }

                final int result = this.columnText.go();
                if ((result & ColumnText.NO_MORE_TEXT) != 0) {
                    done = true;
                    this.top = this.columnText.getYLine();
                } else if (shiftCurrentColumn()) {
                    this.top = this.nextY;
                } else {  // check if we are done because of height
                    this.totalHeight += currentHeight;
                    if (this.desiredHeight != AUTOMATIC && this.totalHeight >= this.desiredHeight) {
                        this.overflow = true;
                        break;
                    } else {  // need to start new page and reset the columns
                        documentY = this.nextY;
                        newPage();
                        currentHeight = 0;
                    }
                }
            }
        } catch (final DocumentException ex) {
            ex.printStackTrace();
            throw ex;
        }
        if (this.desiredHeight == AUTOMATIC && this.columnDefs.size() == 1) {
        	currentHeight = documentY - this.columnText.getYLine();
        }
        return currentHeight;
    }

    private void newPage() throws DocumentException {
        resetCurrentColumn();
        if (this.desiredHeight == AUTOMATIC) {
        	this.top = this.nextY = AUTOMATIC;
        }
        else {
        	this.top = this.nextY;
        }
        this.totalHeight = 0;
        if (this.document != null) {
            this.document.newPage();
        }
    }

    /**
     * Figure out the height of a column from the border extents
     *
     * @param left  left border
     * @param right right border
     * @return height
     */
    private float getHeight(final float[] left, final float[] right) {
        float max = Float.MIN_VALUE;
        float min = Float.MAX_VALUE;
        for (int i = 0; i < left.length; i += 2) {
            min = Math.min(min, left[i + 1]);
            max = Math.max(max, left[i + 1]);
        }
        for (int i = 0; i < right.length; i += 2) {
            min = Math.min(min, right[i + 1]);
            max = Math.max(max, right[i + 1]);
        }
        return max - min;
    }


    /**
     * Processes the element by adding it to an
     * <CODE>ElementListener</CODE>.
     *
     * @param	listener	an <CODE>ElementListener</CODE>
     * @return	<CODE>true</CODE> if the element was processed successfully
     */
    @Override
	public boolean process(final ElementListener listener) {
        try {
            return listener.add(this);
        } catch (final DocumentException de) {
            return false;
        }
    }

    /**
     * Gets the type of the text element.
     *
     * @return	a type
     */

    @Override
	public int type() {
        return Element.MULTI_COLUMN_TEXT;
    }

    /**
     * Returns null - not used
     *
     * @return	null
     */

    @Override
	public ArrayList getChunks() {
        return null;
    }

	/**
	 * @see com.lowagie.text.Element#isContent()
	 * @since	iText 2.0.8
	 */
	@Override
	public boolean isContent() {
		return true;
	}

	/**
	 * @see com.lowagie.text.Element#isNestable()
	 * @since	iText 2.0.8
	 */
	@Override
	public boolean isNestable() {
		return false;
	}

    /**
     * Calculates the appropriate y position for the bottom
     * of the columns on this page.
     *
     * @return the y position of the bottom of the columns
     */
    private float getColumnBottom() {
        if (this.desiredHeight == AUTOMATIC) {
            return this.document.bottom();
        } else {
            return Math.max(this.top - (this.desiredHeight - this.totalHeight), this.document.bottom());
        }
    }

    /**
     * Moves the text insertion point to the beginning of the next column, issuing a page break if
     * needed.
     * @throws DocumentException on error
     */
    public void nextColumn() throws DocumentException {
        this.currentColumn = (this.currentColumn + 1) % this.columnDefs.size();
        this.top = this.nextY;
        if (this.currentColumn == 0) {
            newPage();
        }
    }

    /**
     * Gets the current column.
     * @return the current column
     */
    public int getCurrentColumn() {
    	if (this.columnsRightToLeft) {
    		return this.columnDefs.size() - this.currentColumn - 1;
    	}
        return this.currentColumn;
    }

    /**
     * Resets the current column.
     */
    public void resetCurrentColumn() {
    	this.currentColumn = 0;
    }

    /**
     * Shifts the current column.
     * @return true if the current column has changed
     */
    public boolean shiftCurrentColumn() {
    	if (this.currentColumn + 1 < this.columnDefs.size()) {
            this.currentColumn++;
            return true;
    	}
    	return false;
    }

    /**
     * Sets the direction of the columns.
     * @param direction true = right2left; false = left2right
     */
    public void setColumnsRightToLeft(final boolean direction) {
    	this.columnsRightToLeft = direction;
    }

    /** Sets the ratio between the extra word spacing and the extra character spacing
     * when the text is fully justified.
     * Extra word spacing will grow <CODE>spaceCharRatio</CODE> times more than extra character spacing.
     * If the ratio is <CODE>PdfWriter.NO_SPACE_CHAR_RATIO</CODE> then the extra character spacing
     * will be zero.
     * @param spaceCharRatio the ratio between the extra word spacing and the extra character spacing
     */
    public void setSpaceCharRatio(final float spaceCharRatio) {
        this.columnText.setSpaceCharRatio(spaceCharRatio);
    }

    /** Sets the run direction.
     * @param runDirection the run direction
     */
    public void setRunDirection(final int runDirection) {
        this.columnText.setRunDirection(runDirection);
    }

    /** Sets the arabic shaping options. The option can be AR_NOVOWEL,
     * AR_COMPOSEDTASHKEEL and AR_LIG.
     * @param arabicOptions the arabic shaping options
     */
    public void setArabicOptions(final int arabicOptions) {
        this.columnText.setArabicOptions(arabicOptions);
    }

    /** Sets the default alignment
     * @param alignment the default alignment
     */
    public void setAlignment(final int alignment) {
        this.columnText.setAlignment(alignment);
    }

    /**
     * Inner class used to define a column
     */
    private class ColumnDef {
        private final float[] left;
        private final float[] right;

        ColumnDef(final float[] newLeft, final float[] newRight) {
            this.left = newLeft;
            this.right = newRight;
        }

        ColumnDef(final float leftPosition, final float rightPosition) {
            this.left = new float[4];
            this.left[0] = leftPosition; // x1
            this.left[1] = MultiColumnText.this.top;          // y1
            this.left[2] = leftPosition; // x2
            if (MultiColumnText.this.desiredHeight == AUTOMATIC || MultiColumnText.this.top == AUTOMATIC) {
                this.left[3] = AUTOMATIC;
            } else {
                this.left[3] = MultiColumnText.this.top - MultiColumnText.this.desiredHeight;
            }

            this.right = new float[4];
            this.right[0] = rightPosition; // x1
            this.right[1] = MultiColumnText.this.top;           // y1
            this.right[2] = rightPosition; // x2
            if (MultiColumnText.this.desiredHeight == AUTOMATIC || MultiColumnText.this.top == AUTOMATIC) {
                this.right[3] = AUTOMATIC;
            } else {
                this.right[3] = MultiColumnText.this.top - MultiColumnText.this.desiredHeight;
            }
        }

        /**
         * Resolves the positions for the specified side of the column
         * into real numbers once the top of the column is known.
         *
         * @param side either <CODE>Rectangle.LEFT</CODE>
         *             or <CODE>Rectangle.RIGHT</CODE>
         * @return the array of floats for the side
         */
        float[] resolvePositions(final int side) {
            if (side == Rectangle.LEFT) {
                return resolvePositions(this.left);
            } else {
                return resolvePositions(this.right);
            }
        }

        private float[] resolvePositions(final float[] positions) {
            if (!isSimple()) {
                positions[1] = MultiColumnText.this.top;
                return positions;
            }
            if (MultiColumnText.this.top == AUTOMATIC) {
                // this is bad - must be programmer error
                throw new RuntimeException("resolvePositions called with top=AUTOMATIC (-1).  " +
                        "Top position must be set befure lines can be resolved");
            }
            positions[1] = MultiColumnText.this.top;
            positions[3] = getColumnBottom();
            return positions;
        }

        /**
         * Checks if column definition is a simple rectangle
         * @return true if it is a simple column
         */
        private boolean isSimple() {
            return this.left.length == 4 && this.right.length == 4 && this.left[0] == this.left[2] && this.right[0] == this.right[2];
        }

    }
}
