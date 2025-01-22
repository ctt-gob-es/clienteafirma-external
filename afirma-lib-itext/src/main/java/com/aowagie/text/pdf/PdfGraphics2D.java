/*
 * $Id: PdfGraphics2D.java 3611 2008-11-05 19:45:31Z blowagie $
 *
 * Copyright 2002 by Jim Moore <jim@scolamoore.com>.
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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.Transparency;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.RenderableImage;
import java.io.ByteArrayOutputStream;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

import com.aowagie.text.pdf.internal.PolylineShape;

public class PdfGraphics2D extends Graphics2D {

    private static final int FILL = 1;
    private static final int STROKE = 2;
    private static final int CLIP = 3;
    private BasicStroke strokeOne = new BasicStroke(1);

    private static final AffineTransform IDENTITY = new AffineTransform();

    private Font font;
    private BaseFont baseFont;
    private float fontSize;
    private AffineTransform transform;
    private Paint paint;
    private Color background;
    private float width;
    private float height;

    private Area clip;

    private final RenderingHints rhints = new RenderingHints(null);

    private Stroke stroke;
    private Stroke originalStroke;

    private PdfContentByte cb;

    /** Storage for BaseFont objects created. */
    private HashMap baseFonts;

    private boolean disposeCalled = false;

    private FontMapper fontMapper;

    private ArrayList kids;

    private boolean kid = false;

    private Graphics2D dg2 = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB).createGraphics();

    private boolean onlyShapes = false;

    private Stroke oldStroke;
    private Paint paintFill;
    private Paint paintStroke;

    private MediaTracker mediaTracker;

    // Added by Jurij Bilas
    protected boolean underline;          // indicates if the font style is underlined

    protected PdfGState fillGState[] = new PdfGState[256];
    protected PdfGState strokeGState[] = new PdfGState[256];
    protected int currentFillGState = 255;
    protected int currentStrokeGState = 255;

    public static final int AFM_DIVISOR = 1000; // used to calculate coordinates

    private boolean convertImagesToJPEG = false;
    private float jpegQuality = .95f;

	// Added by Alexej Suchov
	private float alpha;

	// Added by Alexej Suchov
	private Composite composite;

	// Added by Alexej Suchov
	private Paint realPaint;

    private PdfGraphics2D() {
        this.dg2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        setRenderingHint(HyperLinkKey.KEY_INSTANCE, HyperLinkKey.VALUE_HYPERLINKKEY_OFF);
    }

    /**
     * Constructor for PDFGraphics2D.
     *
     */
    PdfGraphics2D(final PdfContentByte cb, final float width, final float height, final FontMapper fontMapper, final boolean onlyShapes, final boolean convertImagesToJPEG, final float quality) {
        super();
        this.dg2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        setRenderingHint(HyperLinkKey.KEY_INSTANCE, HyperLinkKey.VALUE_HYPERLINKKEY_OFF);
        this.convertImagesToJPEG = convertImagesToJPEG;
        this.jpegQuality = quality;
        this.onlyShapes = onlyShapes;
        this.transform = new AffineTransform();
        this.baseFonts = new HashMap();
        if (!onlyShapes) {
            this.fontMapper = fontMapper;
            if (this.fontMapper == null) {
				this.fontMapper = new DefaultFontMapper();
			}
        }
        this.paint = Color.black;
        this.background = Color.white;
        setFont(new Font("sanserif", Font.PLAIN, 12));
        this.cb = cb;
        cb.saveState();
        this.width = width;
        this.height = height;
        this.clip = new Area(new Rectangle2D.Float(0, 0, width, height));
        clip(this.clip);
        this.originalStroke = this.stroke = this.oldStroke = this.strokeOne;
        setStrokeDiff(this.stroke, null);
        cb.saveState();
    }

    /**
     * @see Graphics2D#draw(Shape)
     */
    @Override
	public void draw(final Shape s) {
        followPath(s, STROKE);
    }

    /**
     * @see Graphics2D#drawImage(Image, AffineTransform, ImageObserver)
     */
    @Override
	public boolean drawImage(final Image img, final AffineTransform xform, final ImageObserver obs) {
        return drawImage(img, null, xform, null, obs);
    }

    /**
     * @see Graphics2D#drawImage(BufferedImage, BufferedImageOp, int, int)
     */
    @Override
	public void drawImage(final BufferedImage img, final BufferedImageOp op, final int x, final int y) {
        BufferedImage result = img;
        if (op != null) {
            result = op.createCompatibleDestImage(img, img.getColorModel());
            result = op.filter(img, result);
        }
        drawImage(result, x, y, null);
    }

    /**
     * @see Graphics2D#drawRenderedImage(RenderedImage, AffineTransform)
     */
    @Override
	public void drawRenderedImage(final RenderedImage img, final AffineTransform xform) {
        BufferedImage image = null;
        if (img instanceof BufferedImage) {
            image = (BufferedImage)img;
        } else {
            final ColorModel cm = img.getColorModel();
            final int width = img.getWidth();
            final int height = img.getHeight();
            final WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
            final boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
            final Hashtable properties = new Hashtable();
            final String[] keys = img.getPropertyNames();
            if (keys!=null) {
                for (int i = 0; i < keys.length; i++) {
                    properties.put(keys[i], img.getProperty(keys[i]));
                }
            }
            final BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
            img.copyData(raster);
            image=result;
        }
        drawImage(image, xform, null);
    }

    /**
     * @see Graphics2D#drawRenderableImage(RenderableImage, AffineTransform)
     */
    @Override
	public void drawRenderableImage(final RenderableImage img, final AffineTransform xform) {
        drawRenderedImage(img.createDefaultRendering(), xform);
    }

    /**
     * @see Graphics#drawString(String, int, int)
     */
    @Override
	public void drawString(final String s, final int x, final int y) {
        drawString(s, (float)x, (float)y);
    }

    /**
     * Calculates position and/or stroke thickness depending on the font size
     * @param d value to be converted
     * @param i font size
     * @return position and/or stroke thickness depending on the font size
     */
    public static double asPoints(final double d, final int i) {
        return d * i / AFM_DIVISOR;
    }
    /**
     * This routine goes through the attributes and sets the font
     * before calling the actual string drawing routine
     * @param iter
     */
    protected void doAttributes(final AttributedCharacterIterator iter) {
        this.underline = false;
        final Set set = iter.getAttributes().keySet();
        for(final Iterator iterator = set.iterator(); iterator.hasNext();) {
            final AttributedCharacterIterator.Attribute attribute = (AttributedCharacterIterator.Attribute)iterator.next();
            if (!(attribute instanceof TextAttribute)) {
				continue;
			}
            final TextAttribute textattribute = (TextAttribute)attribute;
            if(textattribute.equals(TextAttribute.FONT)) {
                final Font font = (Font)iter.getAttributes().get(textattribute);
                setFont(font);
            }
            else if(textattribute.equals(TextAttribute.UNDERLINE)) {
                if(iter.getAttributes().get(textattribute) == TextAttribute.UNDERLINE_ON) {
					this.underline = true;
				}
            }
            else if(textattribute.equals(TextAttribute.SIZE)) {
                final Object obj = iter.getAttributes().get(textattribute);
                if(obj instanceof Integer) {
                    final int i = ((Integer)obj).intValue();
                    setFont(getFont().deriveFont(getFont().getStyle(), i));
                }
                else if(obj instanceof Float) {
                    final float f = ((Float)obj).floatValue();
                    setFont(getFont().deriveFont(getFont().getStyle(), f));
                }
            }
            else if(textattribute.equals(TextAttribute.FOREGROUND)) {
                setColor((Color) iter.getAttributes().get(textattribute));
            }
            else if(textattribute.equals(TextAttribute.FAMILY)) {
              final Font font = getFont();
              final Map fontAttributes = font.getAttributes();
              fontAttributes.put(TextAttribute.FAMILY, iter.getAttributes().get(textattribute));
              setFont(font.deriveFont(fontAttributes));
            }
            else if(textattribute.equals(TextAttribute.POSTURE)) {
              final Font font = getFont();
              final Map fontAttributes = font.getAttributes();
              fontAttributes.put(TextAttribute.POSTURE, iter.getAttributes().get(textattribute));
              setFont(font.deriveFont(fontAttributes));
            }
            else if(textattribute.equals(TextAttribute.WEIGHT)) {
              final Font font = getFont();
              final Map fontAttributes = font.getAttributes();
              fontAttributes.put(TextAttribute.WEIGHT, iter.getAttributes().get(textattribute));
              setFont(font.deriveFont(fontAttributes));
            }
        }
    }

    /**
     * @see Graphics2D#drawString(String, float, float)
     */
    @Override
	public void drawString(final String s, final float x, float y) {
        if (s.length() == 0) {
			return;
		}
        setFillPaint();
        if (this.onlyShapes) {
            drawGlyphVector(this.font.layoutGlyphVector(getFontRenderContext(), s.toCharArray(), 0, s.length(), java.awt.Font.LAYOUT_LEFT_TO_RIGHT), x, y);
//            Use the following line to compile in JDK 1.3
//            drawGlyphVector(this.font.createGlyphVector(getFontRenderContext(), s), x, y);
        }
        else {
        	boolean restoreTextRenderingMode = false;
            final AffineTransform at = getTransform();
            final AffineTransform at2 = getTransform();
            at2.translate(x, y);
            at2.concatenate(this.font.getTransform());
            setTransform(at2);
            final AffineTransform inverse = normalizeMatrix();
            final AffineTransform flipper = AffineTransform.getScaleInstance(1,-1);
            inverse.concatenate(flipper);
            final double[] mx = new double[6];
            inverse.getMatrix(mx);
            this.cb.beginText();
            this.cb.setFontAndSize(this.baseFont, this.fontSize);
            // Check if we need to simulate an italic font.
            // When there are different fonts for italic, bold, italic bold
            // the font.getName() will be different from the font.getFontName()
            // value. When they are the same value then we are normally dealing
            // with a single font that has been made into an italic or bold
            // font.
            if (this.font.isItalic() && this.font.getFontName().equals(this.font.getName())) {
                final float angle = this.baseFont.getFontDescriptor(BaseFont.ITALICANGLE, 1000);
                float angle2 = this.font.getItalicAngle();
                // We don't have an italic version of this font so we need
                // to set the font angle ourselves to produce an italic font.
                if (angle2 == 0) {
                    // The JavaVM didn't have an angle setting for making
                    // the font an italic font so use a default of
                    // italic angle of 15 degrees.
                    angle2 = 15.0f;
                } else {
                    // This sign of the angle for Java and PDF seams
                    // seams to be reversed.
                    angle2 = -angle2;
                }
                if (angle == 0) {
                    mx[2] = angle2 / 100.0f;
                }
            }
            this.cb.setTextMatrix((float)mx[0], (float)mx[1], (float)mx[2], (float)mx[3], (float)mx[4], (float)mx[5]);
            Float fontTextAttributeWidth = (Float)this.font.getAttributes().get(TextAttribute.WIDTH);
            fontTextAttributeWidth = fontTextAttributeWidth == null
                                     ? TextAttribute.WIDTH_REGULAR
                                     : fontTextAttributeWidth;
            if (!TextAttribute.WIDTH_REGULAR.equals(fontTextAttributeWidth)) {
				this.cb.setHorizontalScaling(100.0f / fontTextAttributeWidth.floatValue());
			}

            // Check if we need to simulate a bold font.
            // Do nothing if the BaseFont is already bold. This test is not foolproof but it will work most of the times.
            if (this.baseFont.getPostscriptFontName().toLowerCase().indexOf("bold") < 0) {
                // Get the weight of the font so we can detect fonts with a weight
                // that makes them bold, but the Font.isBold() value is false.
                Float weight = (Float) this.font.getAttributes().get(TextAttribute.WEIGHT);
                if (weight == null) {
                    weight = this.font.isBold() ? TextAttribute.WEIGHT_BOLD
                                             : TextAttribute.WEIGHT_REGULAR;
                }
                if ((this.font.isBold() || weight.floatValue() >= TextAttribute.WEIGHT_SEMIBOLD.floatValue())
                    && this.font.getFontName().equals(this.font.getName())) {
                    // Simulate a bold font.
                    final float strokeWidth = this.font.getSize2D() * (weight.floatValue() - TextAttribute.WEIGHT_REGULAR.floatValue()) / 30f;
                    if (strokeWidth != 1) {
                        if(this.realPaint instanceof Color){
                            this.cb.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_FILL_STROKE);
                            this.cb.setLineWidth(strokeWidth);
                            final Color color = (Color)this.realPaint;
                            final int alpha = color.getAlpha();
                            if (alpha != this.currentStrokeGState) {
                                this.currentStrokeGState = alpha;
                                PdfGState gs = this.strokeGState[alpha];
                                if (gs == null) {
                                    gs = new PdfGState();
                                    gs.setStrokeOpacity(alpha / 255f);
                                    this.strokeGState[alpha] = gs;
                                }
                                this.cb.setGState(gs);
                            }
                            this.cb.setColorStroke(color);
                            restoreTextRenderingMode = true;
                        }
                    }
                }
            }

            double width = 0;
            if (this.font.getSize2D() > 0) {
                final float scale = 1000 / this.font.getSize2D();
                final Font derivedFont = this.font.deriveFont(AffineTransform.getScaleInstance(scale, scale));
                width = derivedFont.getStringBounds(s, getFontRenderContext()).getWidth();
                if (derivedFont.isTransformed()) {
					width /= scale;
				}
            }
            // if the hyperlink flag is set add an action to the text
            final Object url = getRenderingHint(HyperLinkKey.KEY_INSTANCE);
            if (url != null && !url.equals(HyperLinkKey.VALUE_HYPERLINKKEY_OFF))
            {
                final float scale = 1000 / this.font.getSize2D();
                final Font derivedFont = this.font.deriveFont(AffineTransform.getScaleInstance(scale, scale));
                double height = derivedFont.getStringBounds(s, getFontRenderContext()).getHeight();
                if (derivedFont.isTransformed()) {
					height /= scale;
				}
                final double leftX = this.cb.getXTLM();
                final double leftY = this.cb.getYTLM();
                final PdfAction action = new  PdfAction(url.toString());
                this.cb.setAction(action, (float)leftX, (float)leftY, (float)(leftX+width), (float)(leftY+height));
            }
            if (s.length() > 1) {
                final float adv = ((float)width - this.baseFont.getWidthPoint(s, this.fontSize)) / (s.length() - 1);
                this.cb.setCharacterSpacing(adv);
            }
            this.cb.showText(s);
            if (s.length() > 1) {
                this.cb.setCharacterSpacing(0);
            }
            if (!TextAttribute.WIDTH_REGULAR.equals(fontTextAttributeWidth)) {
				this.cb.setHorizontalScaling(100);
			}

            // Restore the original TextRenderingMode if needed.
            if (restoreTextRenderingMode) {
                this.cb.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_FILL);
            }

            this.cb.endText();
            setTransform(at);
            if(this.underline)
            {
                // These two are supposed to be taken from the .AFM file
                //int UnderlinePosition = -100;
                final int UnderlineThickness = 50;
                //
                final double d = asPoints(UnderlineThickness, (int)this.fontSize);
                final Stroke savedStroke = this.originalStroke;
                setStroke(new BasicStroke((float)d));
                y = (float)(y + asPoints(UnderlineThickness, (int)this.fontSize));
                final Line2D line = new Line2D.Double(x, y, width+x, y);
                draw(line);
                setStroke(savedStroke);
            }
        }
    }

    /**
     * @see Graphics#drawString(AttributedCharacterIterator, int, int)
     */
    @Override
	public void drawString(final AttributedCharacterIterator iterator, final int x, final int y) {
        drawString(iterator, (float)x, (float)y);
    }

    /**
     * @see Graphics2D#drawString(AttributedCharacterIterator, float, float)
     */
    @Override
	public void drawString(final AttributedCharacterIterator iter, float x, final float y) {
/*
        StringBuffer sb = new StringBuffer();
        for(char c = iter.first(); c != AttributedCharacterIterator.DONE; c = iter.next()) {
            sb.append(c);
        }
        drawString(sb.toString(),x,y);
*/
        final StringBuffer stringbuffer = new StringBuffer(iter.getEndIndex());
        for(char c = iter.first(); c != '\uFFFF'; c = iter.next())
        {
            if(iter.getIndex() == iter.getRunStart())
            {
                if(stringbuffer.length() > 0)
                {
                    drawString(stringbuffer.toString(), x, y);
                    final FontMetrics fontmetrics = getFontMetrics();
                    x = (float)(x + fontmetrics.getStringBounds(stringbuffer.toString(), this).getWidth());
                    stringbuffer.delete(0, stringbuffer.length());
                }
                doAttributes(iter);
            }
            stringbuffer.append(c);
        }

        drawString(stringbuffer.toString(), x, y);
        this.underline = false;
    }

    /**
     * @see Graphics2D#drawGlyphVector(GlyphVector, float, float)
     */
    @Override
	public void drawGlyphVector(final GlyphVector g, final float x, final float y) {
        final Shape s = g.getOutline(x, y);
        fill(s);
    }

    /**
     * @see Graphics2D#fill(Shape)
     */
    @Override
	public void fill(final Shape s) {
        followPath(s, FILL);
    }

    /**
     * @see Graphics2D#hit(Rectangle, Shape, boolean)
     */
    @Override
	public boolean hit(final Rectangle rect, Shape s, final boolean onStroke) {
        if (onStroke) {
            s = this.stroke.createStrokedShape(s);
        }
        s = this.transform.createTransformedShape(s);
        final Area area = new Area(s);
        if (this.clip != null) {
			area.intersect(this.clip);
		}
        return area.intersects(rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * @see Graphics2D#getDeviceConfiguration()
     */
    @Override
	public GraphicsConfiguration getDeviceConfiguration() {
        return this.dg2.getDeviceConfiguration();
    }

    /**
	 * Method contributed by Alexej Suchov
     * @see Graphics2D#setComposite(Composite)
     */
    @Override
	public void setComposite(final Composite comp) {

		if (comp instanceof AlphaComposite) {

			final AlphaComposite composite = (AlphaComposite) comp;

			if (composite.getRule() == 3) {

				this.alpha = composite.getAlpha();
				this.composite = composite;

				if (this.realPaint != null && this.realPaint instanceof Color) {

					final Color c = (Color) this.realPaint;
					this.paint = new Color(c.getRed(), c.getGreen(), c.getBlue(),
							(int) (c.getAlpha() * this.alpha));
				}
				return;
			}
		}

		this.composite = comp;
		this.alpha = 1.0F;

    }

    /**
	 * Method contributed by Alexej Suchov
     * @see Graphics2D#setPaint(Paint)
     */
    @Override
	public void setPaint(final Paint paint) {
        if (paint == null) {
			return;
		}
        this.paint = paint;
		this.realPaint = paint;

		if (this.composite instanceof AlphaComposite && paint instanceof Color) {

			final AlphaComposite co = (AlphaComposite) this.composite;

			if (co.getRule() == 3) {
				final Color c = (Color) paint;
				this.paint = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (c.getAlpha() * this.alpha));
				this.realPaint = paint;
			}
		}

    }

    private Stroke transformStroke(final Stroke stroke) {
        if (!(stroke instanceof BasicStroke)) {
			return stroke;
		}
        final BasicStroke st = (BasicStroke)stroke;
        final float scale = (float)Math.sqrt(Math.abs(this.transform.getDeterminant()));
        final float dash[] = st.getDashArray();
        if (dash != null) {
            for (int k = 0; k < dash.length; ++k) {
				dash[k] *= scale;
			}
        }
        return new BasicStroke(st.getLineWidth() * scale, st.getEndCap(), st.getLineJoin(), st.getMiterLimit(), dash, st.getDashPhase() * scale);
    }

    private void setStrokeDiff(final Stroke newStroke, final Stroke oldStroke) {
        if (newStroke == oldStroke) {
			return;
		}
        if (!(newStroke instanceof BasicStroke)) {
			return;
		}
        final BasicStroke nStroke = (BasicStroke)newStroke;
        final boolean oldOk = oldStroke instanceof BasicStroke;
        BasicStroke oStroke = null;
        if (oldOk) {
			oStroke = (BasicStroke)oldStroke;
		}
        if (!oldOk || nStroke.getLineWidth() != oStroke.getLineWidth()) {
			this.cb.setLineWidth(nStroke.getLineWidth());
		}
        if (!oldOk || nStroke.getEndCap() != oStroke.getEndCap()) {
            switch (nStroke.getEndCap()) {
            case BasicStroke.CAP_BUTT:
                this.cb.setLineCap(0);
                break;
            case BasicStroke.CAP_SQUARE:
                this.cb.setLineCap(2);
                break;
            default:
                this.cb.setLineCap(1);
            }
        }
        if (!oldOk || nStroke.getLineJoin() != oStroke.getLineJoin()) {
            switch (nStroke.getLineJoin()) {
            case BasicStroke.JOIN_MITER:
                this.cb.setLineJoin(0);
                break;
            case BasicStroke.JOIN_BEVEL:
                this.cb.setLineJoin(2);
                break;
            default:
                this.cb.setLineJoin(1);
            }
        }
        if (!oldOk || nStroke.getMiterLimit() != oStroke.getMiterLimit()) {
			this.cb.setMiterLimit(nStroke.getMiterLimit());
		}
        boolean makeDash;
        if (oldOk) {
            if (nStroke.getDashArray() != null) {
                if (nStroke.getDashPhase() != oStroke.getDashPhase()) {
                    makeDash = true;
                }
                else if (!java.util.Arrays.equals(nStroke.getDashArray(), oStroke.getDashArray())) {
                    makeDash = true;
                } else {
					makeDash = false;
				}
            }
            else if (oStroke.getDashArray() != null) {
                makeDash = true;
            } else {
				makeDash = false;
			}
        }
        else {
            makeDash = true;
        }
        if (makeDash) {
            final float dash[] = nStroke.getDashArray();
            if (dash == null) {
				this.cb.setLiteral("[]0 d\n");
			} else {
                this.cb.setLiteral('[');
                final int lim = dash.length;
                for (int k = 0; k < lim; ++k) {
                    this.cb.setLiteral(dash[k]);
                    this.cb.setLiteral(' ');
                }
                this.cb.setLiteral(']');
                this.cb.setLiteral(nStroke.getDashPhase());
                this.cb.setLiteral(" d\n");
            }
        }
    }

    /**
     * @see Graphics2D#setStroke(Stroke)
     */
    @Override
	public void setStroke(final Stroke s) {
        this.originalStroke = s;
        this.stroke = transformStroke(s);
    }


    /**
     * Sets a rendering hint
     * @param key the rendering hint key.
     * @param value the rendering hint value.
     */
    @Override
	public void setRenderingHint(final Key key, final Object value) {
    	 if (value != null) {
         	this.rhints.put(key, value);
         } else {
        	 if (key instanceof HyperLinkKey)
        	 {
        		 this.rhints.put(key, HyperLinkKey.VALUE_HYPERLINKKEY_OFF);
        	 }
        	 else
        	 {
        		 this.rhints.remove(key);
        	 }
         }
    }

    /**
     * @param arg0 a key
     * @return the rendering hint
     */
    @Override
	public Object getRenderingHint(final Key arg0) {
        return this.rhints.get(arg0);
    }

    /**
     * @see Graphics2D#setRenderingHints(Map)
     */
    @Override
	public void setRenderingHints(final Map hints) {
        this.rhints.clear();
        this.rhints.putAll(hints);
    }

    /**
     * @see Graphics2D#addRenderingHints(Map)
     */
    @Override
	public void addRenderingHints(final Map hints) {
        this.rhints.putAll(hints);
    }

    /**
     * @see Graphics2D#getRenderingHints()
     */
    @Override
	public RenderingHints getRenderingHints() {
        return this.rhints;
    }

    /**
     * @see Graphics#translate(int, int)
     */
    @Override
	public void translate(final int x, final int y) {
        translate((double)x, (double)y);
    }

    /**
     * @see Graphics2D#translate(double, double)
     */
    @Override
	public void translate(final double tx, final double ty) {
        this.transform.translate(tx,ty);
    }

    /**
     * @see Graphics2D#rotate(double)
     */
    @Override
	public void rotate(final double theta) {
        this.transform.rotate(theta);
    }

    /**
     * @see Graphics2D#rotate(double, double, double)
     */
    @Override
	public void rotate(final double theta, final double x, final double y) {
        this.transform.rotate(theta, x, y);
    }

    /**
     * @see Graphics2D#scale(double, double)
     */
    @Override
	public void scale(final double sx, final double sy) {
        this.transform.scale(sx, sy);
        this.stroke = transformStroke(this.originalStroke);
    }

    /**
     * @see Graphics2D#shear(double, double)
     */
    @Override
	public void shear(final double shx, final double shy) {
        this.transform.shear(shx, shy);
    }

    /**
     * @see Graphics2D#transform(AffineTransform)
     */
    @Override
	public void transform(final AffineTransform tx) {
        this.transform.concatenate(tx);
        this.stroke = transformStroke(this.originalStroke);
    }

    /**
     * @see Graphics2D#setTransform(AffineTransform)
     */
    @Override
	public void setTransform(final AffineTransform t) {
        this.transform = new AffineTransform(t);
        this.stroke = transformStroke(this.originalStroke);
    }

    /**
     * @see Graphics2D#getTransform()
     */
    @Override
	public AffineTransform getTransform() {
        return new AffineTransform(this.transform);
    }

    /**
	 * Method contributed by Alexej Suchov
     * @see Graphics2D#getPaint()
     */
    @Override
	public Paint getPaint() {
        if (this.realPaint != null) {
            return this.realPaint;
        } else {
            return this.paint;
        }
	}

    /**
     * @see Graphics2D#getComposite()
     */
    @Override
	public Composite getComposite() {
        return this.composite;
    }

    /**
     * @see Graphics2D#setBackground(Color)
     */
    @Override
	public void setBackground(final Color color) {
        this.background = color;
    }

    /**
     * @see Graphics2D#getBackground()
     */
    @Override
	public Color getBackground() {
        return this.background;
    }

    /**
     * @see Graphics2D#getStroke()
     */
    @Override
	public Stroke getStroke() {
        return this.originalStroke;
    }


    /**
     * @see Graphics2D#getFontRenderContext()
     */
    @Override
	public FontRenderContext getFontRenderContext() {
        final boolean antialias = RenderingHints.VALUE_TEXT_ANTIALIAS_ON.equals(getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING));
        final boolean fractions = RenderingHints.VALUE_FRACTIONALMETRICS_ON.equals(getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS));
        return new FontRenderContext(new AffineTransform(), antialias, fractions);
    }

    /**
     * @see Graphics#create()
     */
    @Override
	public Graphics create() {
        final PdfGraphics2D g2 = new PdfGraphics2D();
        g2.rhints.putAll( this.rhints );
        g2.onlyShapes = this.onlyShapes;
        g2.transform = new AffineTransform(this.transform);
        g2.baseFonts = this.baseFonts;
        g2.fontMapper = this.fontMapper;
        g2.paint = this.paint;
        g2.fillGState = this.fillGState;
        g2.currentFillGState = this.currentFillGState;
        g2.strokeGState = this.strokeGState;
        g2.background = this.background;
        g2.mediaTracker = this.mediaTracker;
        g2.convertImagesToJPEG = this.convertImagesToJPEG;
        g2.jpegQuality = this.jpegQuality;
        g2.setFont(this.font);
        g2.cb = this.cb.getDuplicate();
        g2.cb.saveState();
        g2.width = this.width;
        g2.height = this.height;
        g2.followPath(new Area(new Rectangle2D.Float(0, 0, this.width, this.height)), CLIP);
        if (this.clip != null) {
			g2.clip = new Area(this.clip);
		}
        g2.composite = this.composite;
        g2.stroke = this.stroke;
        g2.originalStroke = this.originalStroke;
        g2.strokeOne = (BasicStroke)g2.transformStroke(g2.strokeOne);
        g2.oldStroke = g2.strokeOne;
        g2.setStrokeDiff(g2.oldStroke, null);
        g2.cb.saveState();
        if (g2.clip != null) {
			g2.followPath(g2.clip, CLIP);
		}
        g2.kid = true;
        if (this.kids == null) {
			this.kids = new ArrayList();
		}
        this.kids.add(new Integer(this.cb.getInternalBuffer().size()));
        this.kids.add(g2);
        return g2;
    }

    public PdfContentByte getContent() {
        return this.cb;
    }
    /**
     * @see Graphics#getColor()
     */
    @Override
	public Color getColor() {
        if (this.paint instanceof Color) {
            return (Color)this.paint;
        } else {
            return Color.black;
        }
    }

    /**
     * @see Graphics#setColor(Color)
     */
    @Override
	public void setColor(final Color color) {
        setPaint(color);
    }

    /**
     * @see Graphics#setPaintMode()
     */
    @Override
	public void setPaintMode() {}

    /**
     * @see Graphics#setXORMode(Color)
     */
    @Override
	public void setXORMode(final Color c1) {

    }

    /**
     * @see Graphics#getFont()
     */
    @Override
	public Font getFont() {
        return this.font;
    }

    /**
     * @see Graphics#setFont(Font)
     */
    /**
     * Sets the current font.
     */
    @Override
	public void setFont(final Font f) {
        if (f == null) {
			return;
		}
        if (this.onlyShapes) {
            this.font = f;
            return;
        }
        if (f == this.font) {
			return;
		}
        this.font = f;
        this.fontSize = f.getSize2D();
        this.baseFont = getCachedBaseFont(f);
    }

    private BaseFont getCachedBaseFont(final Font f) {
        synchronized (this.baseFonts) {
            BaseFont bf = (BaseFont)this.baseFonts.get(f.getFontName());
            if (bf == null) {
                bf = this.fontMapper.awtToPdf(f);
                this.baseFonts.put(f.getFontName(), bf);
            }
            return bf;
        }
    }

    /**
     * @see Graphics#getFontMetrics(Font)
     */
    @Override
	public FontMetrics getFontMetrics(final Font f) {
        return this.dg2.getFontMetrics(f);
    }

    /**
     * @see Graphics#getClipBounds()
     */
    @Override
	public Rectangle getClipBounds() {
        if (this.clip == null) {
			return null;
		}
        return getClip().getBounds();
    }

    /**
     * @see Graphics#clipRect(int, int, int, int)
     */
    @Override
	public void clipRect(final int x, final int y, final int width, final int height) {
        final Rectangle2D rect = new Rectangle2D.Double(x,y,width,height);
        clip(rect);
    }

    /**
     * @see Graphics#setClip(int, int, int, int)
     */
    @Override
	public void setClip(final int x, final int y, final int width, final int height) {
        final Rectangle2D rect = new Rectangle2D.Double(x,y,width,height);
        setClip(rect);
    }

    /**
     * @see Graphics2D#clip(Shape)
     */
    @Override
	public void clip(Shape s) {
        if (s == null) {
            setClip(null);
            return;
        }
        s = this.transform.createTransformedShape(s);
        if (this.clip == null) {
			this.clip = new Area(s);
		} else {
			this.clip.intersect(new Area(s));
		}
        followPath(s, CLIP);
    }

    /**
     * @see Graphics#getClip()
     */
    @Override
	public Shape getClip() {
        try {
            return this.transform.createInverse().createTransformedShape(this.clip);
        }
        catch (final NoninvertibleTransformException e) {
            return null;
        }
    }

    /**
     * @see Graphics#setClip(Shape)
     */
    @Override
	public void setClip(Shape s) {
        this.cb.restoreState();
        this.cb.saveState();
        if (s != null) {
			s = this.transform.createTransformedShape(s);
		}
        if (s == null) {
            this.clip = null;
        }
        else {
            this.clip = new Area(s);
            followPath(s, CLIP);
        }
        this.paintFill = this.paintStroke = null;
        this.currentFillGState = this.currentStrokeGState = 255;
        this.oldStroke = this.strokeOne;
    }

    /**
     * @see Graphics#copyArea(int, int, int, int, int, int)
     */
    @Override
	public void copyArea(final int x, final int y, final int width, final int height, final int dx, final int dy) {

    }

    /**
     * @see Graphics#drawLine(int, int, int, int)
     */
    @Override
	public void drawLine(final int x1, final int y1, final int x2, final int y2) {
        final Line2D line = new Line2D.Double(x1, y1, x2, y2);
        draw(line);
    }

    /**
     * @see Graphics#fillRect(int, int, int, int)
     */
    @Override
	public void drawRect(final int x, final int y, final int width, final int height) {
        draw(new Rectangle(x, y, width, height));
    }

    /**
     * @see Graphics#fillRect(int, int, int, int)
     */
    @Override
	public void fillRect(final int x, final int y, final int width, final int height) {
        fill(new Rectangle(x,y,width,height));
    }

    /**
     * @see Graphics#clearRect(int, int, int, int)
     */
    @Override
	public void clearRect(final int x, final int y, final int width, final int height) {
        final Paint temp = this.paint;
        setPaint(this.background);
        fillRect(x,y,width,height);
        setPaint(temp);
    }

    /**
     * @see Graphics#drawRoundRect(int, int, int, int, int, int)
     */
    @Override
	public void drawRoundRect(final int x, final int y, final int width, final int height, final int arcWidth, final int arcHeight) {
        final RoundRectangle2D rect = new RoundRectangle2D.Double(x,y,width,height,arcWidth, arcHeight);
        draw(rect);
    }

    /**
     * @see Graphics#fillRoundRect(int, int, int, int, int, int)
     */
    @Override
	public void fillRoundRect(final int x, final int y, final int width, final int height, final int arcWidth, final int arcHeight) {
        final RoundRectangle2D rect = new RoundRectangle2D.Double(x,y,width,height,arcWidth, arcHeight);
        fill(rect);
    }

    /**
     * @see Graphics#drawOval(int, int, int, int)
     */
    @Override
	public void drawOval(final int x, final int y, final int width, final int height) {
        final Ellipse2D oval = new Ellipse2D.Float(x, y, width, height);
        draw(oval);
    }

    /**
     * @see Graphics#fillOval(int, int, int, int)
     */
    @Override
	public void fillOval(final int x, final int y, final int width, final int height) {
        final Ellipse2D oval = new Ellipse2D.Float(x, y, width, height);
        fill(oval);
    }

    /**
     * @see Graphics#drawArc(int, int, int, int, int, int)
     */
    @Override
	public void drawArc(final int x, final int y, final int width, final int height, final int startAngle, final int arcAngle) {
        final Arc2D arc = new Arc2D.Double(x,y,width,height,startAngle, arcAngle, Arc2D.OPEN);
        draw(arc);

    }

    /**
     * @see Graphics#fillArc(int, int, int, int, int, int)
     */
    @Override
	public void fillArc(final int x, final int y, final int width, final int height, final int startAngle, final int arcAngle) {
        final Arc2D arc = new Arc2D.Double(x,y,width,height,startAngle, arcAngle, Arc2D.PIE);
        fill(arc);
    }

    /**
     * @see Graphics#drawPolyline(int[], int[], int)
     */
    @Override
	public void drawPolyline(final int[] x, final int[] y, final int nPoints) {
        final PolylineShape polyline = new PolylineShape(x, y, nPoints);
        draw(polyline);
    }

    /**
     * @see Graphics#drawPolygon(int[], int[], int)
     */
    @Override
	public void drawPolygon(final int[] xPoints, final int[] yPoints, final int nPoints) {
        final Polygon poly = new Polygon(xPoints, yPoints, nPoints);
        draw(poly);
    }

    /**
     * @see Graphics#fillPolygon(int[], int[], int)
     */
    @Override
	public void fillPolygon(final int[] xPoints, final int[] yPoints, final int nPoints) {
        final Polygon poly = new Polygon();
        for (int i = 0; i < nPoints; i++) {
            poly.addPoint(xPoints[i], yPoints[i]);
        }
        fill(poly);
    }

    /**
     * @see Graphics#drawImage(Image, int, int, ImageObserver)
     */
    @Override
	public boolean drawImage(final Image img, final int x, final int y, final ImageObserver observer) {
        return drawImage(img, x, y, null, observer);
    }

    /**
     * @see Graphics#drawImage(Image, int, int, int, int, ImageObserver)
     */
    @Override
	public boolean drawImage(final Image img, final int x, final int y, final int width, final int height, final ImageObserver observer) {
        return drawImage(img, x, y, width, height, null, observer);
    }

    /**
     * @see Graphics#drawImage(Image, int, int, Color, ImageObserver)
     */
    @Override
	public boolean drawImage(final Image img, final int x, final int y, final Color bgcolor, final ImageObserver observer) {
        waitForImage(img);
        return drawImage(img, x, y, img.getWidth(observer), img.getHeight(observer), bgcolor, observer);
    }

    /**
     * @see Graphics#drawImage(Image, int, int, int, int, Color, ImageObserver)
     */
    @Override
	public boolean drawImage(final Image img, final int x, final int y, final int width, final int height, final Color bgcolor, final ImageObserver observer) {
        waitForImage(img);
        final double scalex = width/(double)img.getWidth(observer);
        final double scaley = height/(double)img.getHeight(observer);
        final AffineTransform tx = AffineTransform.getTranslateInstance(x,y);
        tx.scale(scalex,scaley);
        return drawImage(img, null, tx, bgcolor, observer);
    }

    /**
     * @see Graphics#drawImage(Image, int, int, int, int, int, int, int, int, ImageObserver)
     */
    @Override
	public boolean drawImage(final Image img, final int dx1, final int dy1, final int dx2, final int dy2, final int sx1, final int sy1, final int sx2, final int sy2, final ImageObserver observer) {
        return drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null, observer);
    }

    /**
     * @see Graphics#drawImage(Image, int, int, int, int, int, int, int, int, Color, ImageObserver)
     */
    @Override
	public boolean drawImage(final Image img, final int dx1, final int dy1, final int dx2, final int dy2, final int sx1, final int sy1, final int sx2, final int sy2, final Color bgcolor, final ImageObserver observer) {
        waitForImage(img);
        final double dwidth = (double)dx2-dx1;
        final double dheight = (double)dy2-dy1;
        final double swidth = (double)sx2-sx1;
        final double sheight = (double)sy2-sy1;

        //if either width or height is 0, then there is nothing to draw
        if (dwidth == 0 || dheight == 0 || swidth == 0 || sheight == 0) {
			return true;
		}

        final double scalex = dwidth/swidth;
        final double scaley = dheight/sheight;

        final double transx = sx1*scalex;
        final double transy = sy1*scaley;
        final AffineTransform tx = AffineTransform.getTranslateInstance(dx1-transx,dy1-transy);
        tx.scale(scalex,scaley);

        final BufferedImage mask = new BufferedImage(img.getWidth(observer), img.getHeight(observer), BufferedImage.TYPE_BYTE_BINARY);
        final Graphics g = mask.getGraphics();
        g.fillRect(sx1,sy1, (int)swidth, (int)sheight);
        drawImage(img, mask, tx, null, observer);
        g.dispose();
        return true;
    }

    /**
     * @see Graphics#dispose()
     */
    @Override
	public void dispose() {
        if (this.kid) {
			return;
		}
        if (!this.disposeCalled) {
            this.disposeCalled = true;
            this.cb.restoreState();
            this.cb.restoreState();
            this.dg2.dispose();
            this.dg2 = null;
            if (this.kids != null) {
                final ByteBuffer buf = new ByteBuffer();
                internalDispose(buf);
                final ByteBuffer buf2 = this.cb.getInternalBuffer();
                buf2.reset();
                buf2.append(buf);
            }
        }
    }

    private void internalDispose(final ByteBuffer buf) {
        int last = 0;
        int pos = 0;
        final ByteBuffer buf2 = this.cb.getInternalBuffer();
        if (this.kids != null) {
            for (int k = 0; k < this.kids.size(); k += 2) {
                pos = ((Integer)this.kids.get(k)).intValue();
                final PdfGraphics2D g2 = (PdfGraphics2D)this.kids.get(k + 1);
                g2.cb.restoreState();
                g2.cb.restoreState();
                buf.append(buf2.getBuffer(), last, pos - last);
                g2.dg2.dispose();
                g2.dg2 = null;
                g2.internalDispose(buf);
                last = pos;
            }
        }
        buf.append(buf2.getBuffer(), last, buf2.size() - last);
    }

    ///////////////////////////////////////////////
    //
    //
    //		implementation specific methods
    //
    //


    private void followPath(Shape s, final int drawType) {
        if (s==null) {
			return;
		}
        if (drawType==STROKE) {
            if (!(this.stroke instanceof BasicStroke)) {
                s = this.stroke.createStrokedShape(s);
                followPath(s, FILL);
                return;
            }
        }
        if (drawType==STROKE) {
            setStrokeDiff(this.stroke, this.oldStroke);
            this.oldStroke = this.stroke;
            setStrokePaint();
        }
        else if (drawType==FILL) {
			setFillPaint();
		}
        PathIterator points;
        int traces = 0;
        if (drawType == CLIP) {
			points = s.getPathIterator(IDENTITY);
		} else {
			points = s.getPathIterator(this.transform);
		}
        final float[] coords = new float[6];
        while(!points.isDone()) {
            ++traces;
            final int segtype = points.currentSegment(coords);
            normalizeY(coords);
            switch(segtype) {
                case PathIterator.SEG_CLOSE:
                    this.cb.closePath();
                    break;

                case PathIterator.SEG_CUBICTO:
                    this.cb.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                    break;

                case PathIterator.SEG_LINETO:
                    this.cb.lineTo(coords[0], coords[1]);
                    break;

                case PathIterator.SEG_MOVETO:
                    this.cb.moveTo(coords[0], coords[1]);
                    break;

                case PathIterator.SEG_QUADTO:
                    this.cb.curveTo(coords[0], coords[1], coords[2], coords[3]);
                    break;
            }
            points.next();
        }
        switch (drawType) {
        case FILL:
            if (traces > 0) {
                if (points.getWindingRule() == PathIterator.WIND_EVEN_ODD) {
					this.cb.eoFill();
				} else {
					this.cb.fill();
				}
            }
            break;
        case STROKE:
            if (traces > 0) {
				this.cb.stroke();
			}
            break;
        default: //drawType==CLIP
            if (traces == 0) {
				this.cb.rectangle(0, 0, 0, 0);
			}
            if (points.getWindingRule() == PathIterator.WIND_EVEN_ODD) {
				this.cb.eoClip();
			} else {
				this.cb.clip();
			}
            this.cb.newPath();
        }
    }

    private float normalizeY(final float y) {
        return this.height - y;
    }

    private void normalizeY(final float[] coords) {
        coords[1] = normalizeY(coords[1]);
        coords[3] = normalizeY(coords[3]);
        coords[5] = normalizeY(coords[5]);
    }

    private AffineTransform normalizeMatrix() {
        final double[] mx = new double[6];
        AffineTransform result = AffineTransform.getTranslateInstance(0,0);
        result.getMatrix(mx);
        mx[3]=-1;
        mx[5]=this.height;
        result = new AffineTransform(mx);
        result.concatenate(this.transform);
        return result;
    }

    private boolean drawImage(final Image img, final Image mask, AffineTransform xform, final Color bgColor, final ImageObserver obs) {
        if (xform==null) {
			xform = new AffineTransform();
		} else {
			xform = new AffineTransform(xform);
		}
        xform.translate(0, img.getHeight(obs));
        xform.scale(img.getWidth(obs), img.getHeight(obs));

        final AffineTransform inverse = normalizeMatrix();
        final AffineTransform flipper = AffineTransform.getScaleInstance(1,-1);
        inverse.concatenate(xform);
        inverse.concatenate(flipper);

        final double[] mx = new double[6];
        inverse.getMatrix(mx);
        if (this.currentFillGState != 255) {
            PdfGState gs = this.fillGState[255];
            if (gs == null) {
                gs = new PdfGState();
                gs.setFillOpacity(1);
                this.fillGState[255] = gs;
            }
            this.cb.setGState(gs);
        }

        try {
            com.aowagie.text.Image image = null;
            if(!this.convertImagesToJPEG){
                image = com.aowagie.text.Image.getInstance(img, bgColor);
            }
            else{
                BufferedImage scaled = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
                final Graphics2D g3 = scaled.createGraphics();
                g3.drawImage(img, 0, 0, img.getWidth(null), img.getHeight(null), null);
                g3.dispose();

                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final ImageWriteParam iwparam = new JPEGImageWriteParam(Locale.getDefault());
                iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                iwparam.setCompressionQuality(this.jpegQuality);//Set here your compression rate
                final ImageWriter iw = ImageIO.getImageWritersByFormatName("jpg").next();
                final ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
                iw.setOutput(ios);
                iw.write(null, new IIOImage(scaled, null, null), iwparam);
                iw.dispose();
                ios.close();

                scaled.flush();
                scaled = null;
                image = com.aowagie.text.Image.getInstance(baos.toByteArray());

            }
            if (mask!=null) {
                final com.aowagie.text.Image msk = com.aowagie.text.Image.getInstance(mask, null, true);
                msk.makeMask();
                msk.setInverted(true);
                image.setImageMask(msk);
            }
            this.cb.addImage(image, (float)mx[0], (float)mx[1], (float)mx[2], (float)mx[3], (float)mx[4], (float)mx[5]);
            final Object url = getRenderingHint(HyperLinkKey.KEY_INSTANCE);
            if (url != null && !url.equals(HyperLinkKey.VALUE_HYPERLINKKEY_OFF)) {
            	final PdfAction action = new  PdfAction(url.toString());
                this.cb.setAction(action, (float)mx[4], (float)mx[5], (float)(mx[0]+mx[4]), (float)(mx[3]+mx[5]));
            }
        } catch (final Exception ex) {
            throw new IllegalArgumentException();
        }
        if (this.currentFillGState != 255) {
            final PdfGState gs = this.fillGState[this.currentFillGState];
            this.cb.setGState(gs);
        }
        return true;
    }

    private boolean checkNewPaint(final Paint oldPaint) {
        if (this.paint == oldPaint) {
			return false;
		}
        return !(this.paint instanceof Color && this.paint.equals(oldPaint));
    }

    private void setFillPaint() {
        if (checkNewPaint(this.paintFill)) {
            this.paintFill = this.paint;
            setPaint(false, 0, 0, true);
        }
    }

    private void setStrokePaint() {
        if (checkNewPaint(this.paintStroke)) {
            this.paintStroke = this.paint;
            setPaint(false, 0, 0, false);
        }
    }

    private void setPaint(final boolean invert, final double xoffset, final double yoffset, final boolean fill) {
        if (this.paint instanceof Color) {
            final Color color = (Color)this.paint;
            final int alpha = color.getAlpha();
            if (fill) {
                if (alpha != this.currentFillGState) {
                    this.currentFillGState = alpha;
                    PdfGState gs = this.fillGState[alpha];
                    if (gs == null) {
                        gs = new PdfGState();
                        gs.setFillOpacity(alpha / 255f);
                        this.fillGState[alpha] = gs;
                    }
                    this.cb.setGState(gs);
                }
                this.cb.setColorFill(color);
            }
            else {
                if (alpha != this.currentStrokeGState) {
                    this.currentStrokeGState = alpha;
                    PdfGState gs = this.strokeGState[alpha];
                    if (gs == null) {
                        gs = new PdfGState();
                        gs.setStrokeOpacity(alpha / 255f);
                        this.strokeGState[alpha] = gs;
                    }
                    this.cb.setGState(gs);
                }
                this.cb.setColorStroke(color);
            }
        }
        else if (this.paint instanceof GradientPaint) {
            final GradientPaint gp = (GradientPaint)this.paint;
            final Point2D p1 = gp.getPoint1();
            this.transform.transform(p1, p1);
            final Point2D p2 = gp.getPoint2();
            this.transform.transform(p2, p2);
            final Color c1 = gp.getColor1();
            final Color c2 = gp.getColor2();
            final PdfShading shading = PdfShading.simpleAxial(this.cb.getPdfWriter(), (float)p1.getX(), normalizeY((float)p1.getY()), (float)p2.getX(), normalizeY((float)p2.getY()), c1, c2);
            final PdfShadingPattern pat = new PdfShadingPattern(shading);
            if (fill) {
				this.cb.setShadingFill(pat);
			} else {
				this.cb.setShadingStroke(pat);
			}
        }
        else if (this.paint instanceof TexturePaint) {
            try {
                final TexturePaint tp = (TexturePaint)this.paint;
                final BufferedImage img = tp.getImage();
                final Rectangle2D rect = tp.getAnchorRect();
                final com.aowagie.text.Image image = com.aowagie.text.Image.getInstance(img, null);
                final PdfPatternPainter pattern = this.cb.createPattern(image.getWidth(), image.getHeight());
                final AffineTransform inverse = normalizeMatrix();
                inverse.translate(rect.getX(), rect.getY());
                inverse.scale(rect.getWidth() / image.getWidth(), -rect.getHeight() / image.getHeight());
                final double[] mx = new double[6];
                inverse.getMatrix(mx);
                pattern.setPatternMatrix((float)mx[0], (float)mx[1], (float)mx[2], (float)mx[3], (float)mx[4], (float)mx[5]) ;
                image.setAbsolutePosition(0,0);
                pattern.addImage(image);
                if (fill) {
					this.cb.setPatternFill(pattern);
				} else {
					this.cb.setPatternStroke(pattern);
				}
            } catch (final Exception ex) {
                if (fill) {
					this.cb.setColorFill(Color.gray);
				} else {
					this.cb.setColorStroke(Color.gray);
				}
            }
        }
        else {
            try {
                BufferedImage img = null;
                int type = BufferedImage.TYPE_4BYTE_ABGR;
                if (this.paint.getTransparency() == Transparency.OPAQUE) {
                    type = BufferedImage.TYPE_3BYTE_BGR;
                }
                img = new BufferedImage((int)this.width, (int)this.height, type);
                Graphics2D g = (Graphics2D)img.getGraphics();
                g.transform(this.transform);
                final AffineTransform inv = this.transform.createInverse();
                Shape fillRect = new Rectangle2D.Double(0,0,img.getWidth(),img.getHeight());
                fillRect = inv.createTransformedShape(fillRect);
                g.setPaint(this.paint);
                g.fill(fillRect);
                if (invert) {
                    final AffineTransform tx = new AffineTransform();
                    tx.scale(1,-1);
                    tx.translate(-xoffset,-yoffset);
                    g.drawImage(img,tx,null);
                }
                g.dispose();
                g = null;
                final com.aowagie.text.Image image = com.aowagie.text.Image.getInstance(img, null);
                final PdfPatternPainter pattern = this.cb.createPattern(this.width, this.height);
                image.setAbsolutePosition(0,0);
                pattern.addImage(image);
                if (fill) {
					this.cb.setPatternFill(pattern);
				} else {
					this.cb.setPatternStroke(pattern);
				}
            } catch (final Exception ex) {
                if (fill) {
					this.cb.setColorFill(Color.gray);
				} else {
					this.cb.setColorStroke(Color.gray);
				}
            }
        }
    }

    private synchronized void waitForImage(final java.awt.Image image) {
        if (this.mediaTracker == null) {
			this.mediaTracker = new MediaTracker(new PdfGraphics2D.FakeComponent());
		}
        this.mediaTracker.addImage(image, 0);
        try {
            this.mediaTracker.waitForID(0);
        }
        catch (final InterruptedException e) {
            // empty on purpose
        }
        this.mediaTracker.removeImage(image);
    }

    static private class FakeComponent extends Component {

		private static final long serialVersionUID = 6450197945596086638L;
    }

    /**
     * @since 2.0.8
     */
    public static class HyperLinkKey extends RenderingHints.Key
	{
	 	public static final HyperLinkKey KEY_INSTANCE = new HyperLinkKey(9999);
	 	public static final Object VALUE_HYPERLINKKEY_OFF = "0";

		protected HyperLinkKey(final int arg0) {
			super(arg0);
		}

		@Override
		public boolean isCompatibleValue(final Object val)
		{
			return true;
		}
		@Override
		public String toString()
		{
			return "HyperLinkKey";
		}
	}

}
