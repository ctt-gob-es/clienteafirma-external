/*
 * Copyright 2003-2005 by Paulo Soares.
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

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

import com.aowagie.text.DocumentException;
import com.aowagie.text.Element;
import com.aowagie.text.ExceptionConverter;
import com.aowagie.text.Image;
import com.aowagie.text.Rectangle;
import com.aowagie.text.pdf.codec.Base64;

/**
 * Query and change fields in existing documents either by method
 * calls or by FDF merging.
 *
 * @author Paulo Soares (psoares@consiste.pt)
 */
public class AcroFields {

    PdfReader reader;
    PdfWriter writer;
    HashMap<String, AcroFields.Item> fields;
    private int topFirst;
    private HashMap sigNames;
    private boolean append;
    public static final int DA_FONT = 0;
    public static final int DA_SIZE = 1;
    public static final int DA_COLOR = 2;
    private final HashMap extensionFonts = new HashMap();
    private XfaForm xfa;

    /**
     * A field type invalid or not found.
     */
    public static final int FIELD_TYPE_NONE = 0;

    /**
     * A field type.
     */
    public static final int FIELD_TYPE_PUSHBUTTON = 1;

    /**
     * A field type.
     */
    public static final int FIELD_TYPE_CHECKBOX = 2;

    /**
     * A field type.
     */
    public static final int FIELD_TYPE_RADIOBUTTON = 3;

    /**
     * A field type.
     */
    public static final int FIELD_TYPE_TEXT = 4;

    /**
     * A field type.
     */
    public static final int FIELD_TYPE_LIST = 5;

    /**
     * A field type.
     */
    public static final int FIELD_TYPE_COMBO = 6;

    /**
     * A field type.
     */
    public static final int FIELD_TYPE_SIGNATURE = 7;

    private boolean lastWasString;

    /** Holds value of property generateAppearances. */
    private boolean generateAppearances = true;

    private final HashMap localFonts = new HashMap();

    private float extraMarginLeft;
    private float extraMarginTop;
    private ArrayList substitutionFonts;

    AcroFields(final PdfReader reader, final PdfWriter writer) {
        this.reader = reader;
        this.writer = writer;
        try {
            this.xfa = new XfaForm(reader);
        }
        catch (final Exception e) {
            throw new ExceptionConverter(e);
        }
        if (writer instanceof PdfStamperImp) {
            this.append = ((PdfStamperImp)writer).isAppend();
        }
        fill();
    }

    void fill() {
        this.fields = new HashMap();
        final PdfDictionary top = (PdfDictionary)PdfReader.getPdfObjectRelease(this.reader.getCatalog().get(PdfName.ACROFORM));
        if (top == null) {
			return;
		}
        final PdfArray arrfds = (PdfArray)PdfReader.getPdfObjectRelease(top.get(PdfName.FIELDS));
        if (arrfds == null || arrfds.size() == 0) {
			return;
		}
        for (int k = 1; k <= this.reader.getNumberOfPages(); ++k) {
            final PdfDictionary page = this.reader.getPageNRelease(k);
            final PdfArray annots = (PdfArray)PdfReader.getPdfObjectRelease(page.get(PdfName.ANNOTS), page);
            if (annots == null) {
				continue;
			}
            for (int j = 0; j < annots.size(); ++j) {
                PdfDictionary annot = annots.getAsDict(j);
                if (annot == null) {
                    PdfReader.releaseLastXrefPartial(annots.getAsIndirectObject(j));
                    continue;
                }

                // Comprobamos que la firma encontrada estaba entre las firmas declaradas
                boolean found = false;
                final PRIndirectReference foundSignRef = (PRIndirectReference) annots.getPdfObject(j);
                for (int l = 0; l < arrfds.size() && !found; l++) {
                	final PRIndirectReference declaredSignRef = (PRIndirectReference) arrfds.getPdfObject(l);
                	if (foundSignRef.getNumber() == declaredSignRef.getNumber()) {
                		found = true;
                	}
                }

                if (!found) {
                	PdfReader.releaseLastXrefPartial(annots.getAsIndirectObject(j));
                	continue;
                }

                if (!PdfName.WIDGET.equals(annot.getAsName(PdfName.SUBTYPE))) {
                    PdfReader.releaseLastXrefPartial(annots.getAsIndirectObject(j));
                    continue;
                }
                final PdfDictionary widget = annot;
                final PdfDictionary dic = new PdfDictionary();
                dic.putAll(annot);
                String name = "";
                PdfDictionary value = null;
                PdfObject lastV = null;
                while (annot != null) {
                    dic.mergeDifferent(annot);
                    final PdfString t = annot.getAsString(PdfName.T);
                    if (t != null) {
						name = t.toUnicodeString() + "." + name;
					}
                    if (lastV == null && annot.get(PdfName.V) != null) {
						lastV = PdfReader.getPdfObjectRelease(annot.get(PdfName.V));
					}
                    if (value == null &&  t != null) {
                        value = annot;
                        if (annot.get(PdfName.V) == null && lastV  != null) {
							value.put(PdfName.V, lastV);
						}
                    }
                    annot = annot.getAsDict(PdfName.PARENT);
                }
                if (name.length() > 0) {
					name = name.substring(0, name.length() - 1);
				}
                Item item = this.fields.get(name);
                if (item == null) {
                    item = new Item();
                    this.fields.put(name, item);
                }
                if (value == null) {
					item.addValue(widget);
				} else {
					item.addValue(value);
				}
                item.addWidget(widget);
                item.addWidgetRef(annots.getAsIndirectObject(j)); // must be a reference
                if (top != null) {
					dic.mergeDifferent(top);
				}
                item.addMerged(dic);
                item.addPage(k);
                item.addTabOrder(j);
            }
        }
        // some tools produce invisible signatures without an entry in the page annotation array
        // look for a single level annotation
        final PdfNumber sigFlags = top.getAsNumber(PdfName.SIGFLAGS);
        if (sigFlags == null || (sigFlags.intValue() & 1) != 1) {
			return;
		}
        for (int j = 0; j < arrfds.size(); ++j) {
            final PdfDictionary annot = arrfds.getAsDict(j);
            if (annot == null) {
                PdfReader.releaseLastXrefPartial(arrfds.getAsIndirectObject(j));
                continue;
            }
            if (!PdfName.WIDGET.equals(annot.getAsName(PdfName.SUBTYPE))) {
                PdfReader.releaseLastXrefPartial(arrfds.getAsIndirectObject(j));
                continue;
            }
            final PdfArray kids = (PdfArray)PdfReader.getPdfObjectRelease(annot.get(PdfName.KIDS));
            if (kids != null) {
				continue;
			}
            final PdfDictionary dic = new PdfDictionary();
            dic.putAll(annot);
            final PdfString t = annot.getAsString(PdfName.T);
            if (t == null) {
				continue;
			}
            final String name = t.toUnicodeString();
            if (this.fields.containsKey(name)) {
				continue;
			}
            final Item item = new Item();
            this.fields.put(name, item);
            item.addValue(dic);
            item.addWidget(dic);
            item.addWidgetRef(arrfds.getAsIndirectObject(j)); // must be a reference
            item.addMerged(dic);
            item.addPage(-1);
            item.addTabOrder(-1);
        }
    }

    /**
     * Gets the list of appearance names. Use it to get the names allowed
     * with radio and checkbox fields. If the /Opt key exists the values will
     * also be included. The name 'Off' may also be valid
     * even if not returned in the list.
     *
     * @param fieldName the fully qualified field name
     * @return the list of names or <CODE>null</CODE> if the field does not exist
     */
    public String[] getAppearanceStates(final String fieldName) {
        final Item fd = this.fields.get(fieldName);
        if (fd == null) {
			return null;
		}
        final HashMap names = new HashMap();
        final PdfDictionary vals = fd.getValue(0);
        final PdfString stringOpt = vals.getAsString( PdfName.OPT );
        if (stringOpt != null) {
        	names.put(stringOpt.toUnicodeString(), null);
        }
        else {
            final PdfArray arrayOpt = vals.getAsArray(PdfName.OPT);
            if (arrayOpt != null) {
            	for (int k = 0; k < arrayOpt.size(); ++k) {
            		final PdfString valStr = arrayOpt.getAsString( k );
            		if (valStr != null) {
						names.put(valStr.toUnicodeString(), null);
					}
            	}
            }
        }
        for (int k = 0; k < fd.size(); ++k) {
            PdfDictionary dic = fd.getWidget( k );
            dic = dic.getAsDict(PdfName.AP);
            if (dic == null) {
				continue;
			}
            dic = dic.getAsDict(PdfName.N);
            if (dic == null) {
				continue;
			}
            for (final Object element : dic.getKeys()) {
                final String name = PdfName.decodeName(((PdfName)element).toString());
                names.put(name, null);
            }
        }
        final String out[] = new String[names.size()];
        return (String[])names.keySet().toArray(out);
    }

    private String[] getListOption(final String fieldName, final int idx) {
        final Item fd = getFieldItem(fieldName);
        if (fd == null) {
			return null;
		}
        final PdfArray ar = fd.getMerged(0).getAsArray(PdfName.OPT);
        if (ar == null) {
			return null;
		}
        final String[] ret = new String[ar.size()];
        for (int k = 0; k < ar.size(); ++k) {
            PdfObject obj = ar.getDirectObject( k );
            try {
                if (obj.isArray()) {
                    obj = ((PdfArray)obj).getDirectObject(idx);
                }
                if (obj.isString()) {
					ret[k] = ((PdfString)obj).toUnicodeString();
				} else {
					ret[k] = obj.toString();
				}
            }
            catch (final Exception e) {
                ret[k] = "";
            }
        }
        return ret;
    }

    /**
     * Gets the list of export option values from fields of type list or combo.
     * If the field doesn't exist or the field type is not list or combo it will return
     * <CODE>null</CODE>.
     *
     * @param fieldName the field name
     * @return the list of export option values from fields of type list or combo
     */
    public String[] getListOptionExport(final String fieldName) {
        return getListOption(fieldName, 0);
    }

    /**
     * Gets the list of display option values from fields of type list or combo.
     * If the field doesn't exist or the field type is not list or combo it will return
     * <CODE>null</CODE>.
     *
     * @param fieldName the field name
     * @return the list of export option values from fields of type list or combo
     */
    public String[] getListOptionDisplay(final String fieldName) {
        return getListOption(fieldName, 1);
    }

    /**
     * Sets the option list for fields of type list or combo. One of <CODE>exportValues</CODE>
     * or <CODE>displayValues</CODE> may be <CODE>null</CODE> but not both. This method will only
     * set the list but will not set the value or appearance. For that, calling <CODE>setField()</CODE>
     * is required.
     * <p>
     * An example:
     * </p>
     * <PRE>
     * PdfReader pdf = new PdfReader("input.pdf");
     * PdfStamper stp = new PdfStamper(pdf, new FileOutputStream("output.pdf"));
     * AcroFields af = stp.getAcroFields();
     * af.setListOption("ComboBox", new String[]{"a", "b", "c"}, new String[]{"first", "second", "third"});
     * af.setField("ComboBox", "b");
     * stp.close();
     * </PRE>
     * @param fieldName the field name
     * @param exportValues the export values
     * @param displayValues the display values
     * @return <CODE>true</CODE> if the operation succeeded, <CODE>false</CODE> otherwise
     */
    public boolean setListOption(final String fieldName, final String[] exportValues, final String[] displayValues) {
        if (exportValues == null && displayValues == null) {
			return false;
		}
        if (exportValues != null && displayValues != null && exportValues.length != displayValues.length) {
			throw new IllegalArgumentException("The export and the display array must have the same size.");
		}
        final int ftype = getFieldType(fieldName);
        if (ftype != FIELD_TYPE_COMBO && ftype != FIELD_TYPE_LIST) {
			return false;
		}
        final Item fd = this.fields.get(fieldName);
        String[] sing = null;
        if (exportValues == null && displayValues != null) {
			sing = displayValues;
		} else if (exportValues != null && displayValues == null) {
			sing = exportValues;
		}
        final PdfArray opt = new PdfArray();
        if (sing != null) {
            for (final String element : sing) {
				opt.add(new PdfString(element, PdfObject.TEXT_UNICODE));
			}
        }
        else {
            for (int k = 0; k < exportValues.length; ++k) {
                final PdfArray a = new PdfArray();
                a.add(new PdfString(exportValues[k], PdfObject.TEXT_UNICODE));
                a.add(new PdfString(displayValues[k], PdfObject.TEXT_UNICODE));
                opt.add(a);
            }
        }
        fd.writeToAll( PdfName.OPT, opt, Item.WRITE_VALUE | Item.WRITE_MERGED );
        return true;
    }

    /**
     * Gets the field type. The type can be one of: <CODE>FIELD_TYPE_PUSHBUTTON</CODE>,
     * <CODE>FIELD_TYPE_CHECKBOX</CODE>, <CODE>FIELD_TYPE_RADIOBUTTON</CODE>,
     * <CODE>FIELD_TYPE_TEXT</CODE>, <CODE>FIELD_TYPE_LIST</CODE>,
     * <CODE>FIELD_TYPE_COMBO</CODE> or <CODE>FIELD_TYPE_SIGNATURE</CODE>.
     * <p>
     * If the field does not exist or is invalid it returns
     * <CODE>FIELD_TYPE_NONE</CODE>.
     *
     * @param fieldName the field name
     * @return the field type
     */
    public int getFieldType(final String fieldName) {
        final Item fd = getFieldItem(fieldName);
        if (fd == null) {
			return FIELD_TYPE_NONE;
		}
        final PdfDictionary merged = fd.getMerged( 0 );
        final PdfName type = merged.getAsName(PdfName.FT);
        if (type == null) {
			return FIELD_TYPE_NONE;
		}
        int ff = 0;
        final PdfNumber ffo = merged.getAsNumber(PdfName.FF);
        if (ffo != null) {
            ff = ffo.intValue();
        }
        if (PdfName.BTN.equals(type)) {
            if ((ff & PdfFormField.FF_PUSHBUTTON) != 0) {
				return FIELD_TYPE_PUSHBUTTON;
			}
            if ((ff & PdfFormField.FF_RADIO) != 0) {
				return FIELD_TYPE_RADIOBUTTON;
			} else {
				return FIELD_TYPE_CHECKBOX;
			}
        }
        else if (PdfName.TX.equals(type)) {
            return FIELD_TYPE_TEXT;
        }
        else if (PdfName.CH.equals(type)) {
            if ((ff & PdfFormField.FF_COMBO) != 0) {
				return FIELD_TYPE_COMBO;
			} else {
				return FIELD_TYPE_LIST;
			}
        }
        else if (PdfName.SIG.equals(type)) {
            return FIELD_TYPE_SIGNATURE;
        }
        return FIELD_TYPE_NONE;
    }

    /**
     * Export the fields as a FDF.
     *
     * @param writer the FDF writer
     */
    public void exportAsFdf(final FdfWriter writer) {
        for (final Object element : this.fields.entrySet()) {
            final Map.Entry entry = (Map.Entry)element;
            final Item item = (Item)entry.getValue();
            final String name = (String)entry.getKey();
            final PdfObject v = item.getMerged(0).get(PdfName.V);
            if (v == null) {
				continue;
			}
            final String value = getField(name);
            if (this.lastWasString) {
				writer.setFieldAsString(name, value);
			} else {
				writer.setFieldAsName(name, value);
			}
        }
    }

    /**
     * Renames a field. Only the last part of the name can be renamed. For example,
     * if the original field is "ab.cd.ef" only the "ef" part can be renamed.
     *
     * @param oldName the old field name
     * @param newName the new field name
     * @return <CODE>true</CODE> if the renaming was successful, <CODE>false</CODE>
     * otherwise
     */
    public boolean renameField(final String oldName, String newName) {
        final int idx1 = oldName.lastIndexOf('.') + 1;
        final int idx2 = newName.lastIndexOf('.') + 1;
        if (idx1 != idx2) {
			return false;
		}
        if (!oldName.substring(0, idx1).equals(newName.substring(0, idx2))) {
			return false;
		}
        if (this.fields.containsKey(newName)) {
			return false;
		}
        final Item item = this.fields.get(oldName);
        if (item == null) {
			return false;
		}
        newName = newName.substring(idx2);
        final PdfString ss = new PdfString(newName, PdfObject.TEXT_UNICODE);

        item.writeToAll( PdfName.T, ss, Item.WRITE_VALUE | Item.WRITE_MERGED);
        item.markUsed( this, Item.WRITE_VALUE );

        this.fields.remove(oldName);
        this.fields.put(newName, item);

        return true;
    }

    public static Object[] splitDAelements(final String da) {
        try {
            final PRTokeniser tk = new PRTokeniser(PdfEncodings.convertToBytes(da, null));
            final ArrayList stack = new ArrayList();
            final Object ret[] = new Object[3];
            while (tk.nextToken()) {
                if (tk.getTokenType() == PRTokeniser.TK_COMMENT) {
					continue;
				}
                if (tk.getTokenType() == PRTokeniser.TK_OTHER) {
                    final String operator = tk.getStringValue();
                    if (operator.equals("Tf")) {
                        if (stack.size() >= 2) {
                            ret[DA_FONT] = stack.get(stack.size() - 2);
                            ret[DA_SIZE] = new Float((String)stack.get(stack.size() - 1));
                        }
                    }
                    else if (operator.equals("g")) {
                        if (stack.size() >= 1) {
                            final float gray = new Float((String)stack.get(stack.size() - 1)).floatValue();
                            if (gray != 0) {
								ret[DA_COLOR] = new GrayColor(gray);
							}
                        }
                    }
                    else if (operator.equals("rg")) {
                        if (stack.size() >= 3) {
                            final float red = new Float((String)stack.get(stack.size() - 3)).floatValue();
                            final float green = new Float((String)stack.get(stack.size() - 2)).floatValue();
                            final float blue = new Float((String)stack.get(stack.size() - 1)).floatValue();
                            ret[DA_COLOR] = new Color(red, green, blue);
                        }
                    }
                    else if (operator.equals("k")) {
                        if (stack.size() >= 4) {
                            final float cyan = new Float((String)stack.get(stack.size() - 4)).floatValue();
                            final float magenta = new Float((String)stack.get(stack.size() - 3)).floatValue();
                            final float yellow = new Float((String)stack.get(stack.size() - 2)).floatValue();
                            final float black = new Float((String)stack.get(stack.size() - 1)).floatValue();
                            ret[DA_COLOR] = new CMYKColor(cyan, magenta, yellow, black);
                        }
                    }
                    stack.clear();
                } else {
					stack.add(tk.getStringValue());
				}
            }
            return ret;
        }
        catch (final IOException ioe) {
            throw new ExceptionConverter(ioe);
        }
    }

    public void decodeGenericDictionary(final PdfDictionary merged, final BaseField tx) throws IOException, DocumentException {
        int flags = 0;
        // the text size and color
        final PdfString da = merged.getAsString(PdfName.DA);
        if (da != null) {
            final Object dab[] = splitDAelements(da.toUnicodeString());
            if (dab[DA_SIZE] != null) {
				tx.setFontSize(((Float)dab[DA_SIZE]).floatValue());
			}
            if (dab[DA_COLOR] != null) {
				tx.setTextColor((Color)dab[DA_COLOR]);
			}
            if (dab[DA_FONT] != null) {
                PdfDictionary font = merged.getAsDict(PdfName.DR);
                if (font != null) {
                    font = font.getAsDict(PdfName.FONT);
                    if (font != null) {
                        final PdfObject po = font.get(new PdfName((String)dab[DA_FONT]));
                        if (po != null && po.type() == PdfObject.INDIRECT) {
                            final PRIndirectReference por = (PRIndirectReference)po;
                            final BaseFont bp = new DocumentFont((PRIndirectReference)po);
                            tx.setFont(bp);
                            final Integer porkey = new Integer(por.getNumber());
                            BaseFont porf = (BaseFont)this.extensionFonts.get(porkey);
                            if (porf == null) {
                                if (!this.extensionFonts.containsKey(porkey)) {
                                    final PdfDictionary fo = (PdfDictionary)PdfReader.getPdfObject(po);
                                    final PdfDictionary fd = fo.getAsDict(PdfName.FONTDESCRIPTOR);
                                    if (fd != null) {
                                        PRStream prs = (PRStream)PdfReader.getPdfObject(fd.get(PdfName.FONTFILE2));
                                        if (prs == null) {
											prs = (PRStream)PdfReader.getPdfObject(fd.get(PdfName.FONTFILE3));
										}
                                        if (prs == null) {
                                            this.extensionFonts.put(porkey, null);
                                        }
                                        else {
                                            try {
                                                porf = BaseFont.createFont("font.ttf", BaseFont.IDENTITY_H, true, false, PdfReader.getStreamBytes(prs), null);
                                            }
                                            catch (final Exception e) {
                                            }
                                            this.extensionFonts.put(porkey, porf);
                                        }
                                    }
                                }
                            }
                            if (tx instanceof TextField) {
								((TextField)tx).setExtensionFont(porf);
							}
                        }
                        else {
                            BaseFont bf = (BaseFont)this.localFonts.get(dab[DA_FONT]);
                            if (bf == null) {
                                final String fn[] = (String[])stdFieldFontNames.get(dab[DA_FONT]);
                                if (fn != null) {
                                    try {
                                        String enc = "winansi";
                                        if (fn.length > 1) {
											enc = fn[1];
										}
                                        bf = BaseFont.createFont(fn[0], enc, false);
                                        tx.setFont(bf);
                                    }
                                    catch (final Exception e) {
                                        // empty
                                    }
                                }
                            } else {
								tx.setFont(bf);
							}
                        }
                    }
                }
            }
        }
        //rotation, border and background color
        final PdfDictionary mk = merged.getAsDict(PdfName.MK);
        if (mk != null) {
            PdfArray ar = mk.getAsArray(PdfName.BC);
            final Color border = getMKColor(ar);
            tx.setBorderColor(border);
            if (border != null) {
				tx.setBorderWidth(1);
			}
            ar = mk.getAsArray(PdfName.BG);
            tx.setBackgroundColor(getMKColor(ar));
            final PdfNumber rotation = mk.getAsNumber(PdfName.R);
            if (rotation != null) {
				tx.setRotation(rotation.intValue());
			}
        }
        //flags
        PdfNumber nfl = merged.getAsNumber(PdfName.F);
        flags = 0;
        tx.setVisibility(BaseField.VISIBLE_BUT_DOES_NOT_PRINT);
        if (nfl != null) {
            flags = nfl.intValue();
            if ((flags & PdfAnnotation.FLAGS_PRINT) != 0 && (flags & PdfAnnotation.FLAGS_HIDDEN) != 0) {
				tx.setVisibility(BaseField.HIDDEN);
			} else if ((flags & PdfAnnotation.FLAGS_PRINT) != 0 && (flags & PdfAnnotation.FLAGS_NOVIEW) != 0) {
				tx.setVisibility(BaseField.HIDDEN_BUT_PRINTABLE);
			} else if ((flags & PdfAnnotation.FLAGS_PRINT) != 0) {
				tx.setVisibility(BaseField.VISIBLE);
			}
        }
        //multiline
        nfl = merged.getAsNumber(PdfName.FF);
        flags = 0;
        if (nfl != null) {
			flags = nfl.intValue();
		}
        tx.setOptions(flags);
        if ((flags & PdfFormField.FF_COMB) != 0) {
            final PdfNumber maxLen = merged.getAsNumber(PdfName.MAXLEN);
            int len = 0;
            if (maxLen != null) {
				len = maxLen.intValue();
			}
            tx.setMaxCharacterLength(len);
        }
        //alignment
        nfl = merged.getAsNumber(PdfName.Q);
        if (nfl != null) {
            if (nfl.intValue() == PdfFormField.Q_CENTER) {
				tx.setAlignment(Element.ALIGN_CENTER);
			} else if (nfl.intValue() == PdfFormField.Q_RIGHT) {
				tx.setAlignment(Element.ALIGN_RIGHT);
			}
        }
        //border styles
        final PdfDictionary bs = merged.getAsDict(PdfName.BS);
        if (bs != null) {
            final PdfNumber w = bs.getAsNumber(PdfName.W);
            if (w != null) {
				tx.setBorderWidth(w.floatValue());
			}
            final PdfName s = bs.getAsName(PdfName.S);
            if (PdfName.D.equals(s)) {
				tx.setBorderStyle(PdfBorderDictionary.STYLE_DASHED);
			} else if (PdfName.B.equals(s)) {
				tx.setBorderStyle(PdfBorderDictionary.STYLE_BEVELED);
			} else if (PdfName.I.equals(s)) {
				tx.setBorderStyle(PdfBorderDictionary.STYLE_INSET);
			} else if (PdfName.U.equals(s)) {
				tx.setBorderStyle(PdfBorderDictionary.STYLE_UNDERLINE);
			}
        }
        else {
            final PdfArray bd = merged.getAsArray(PdfName.BORDER);
            if (bd != null) {
                if (bd.size() >= 3) {
					tx.setBorderWidth(bd.getAsNumber(2).floatValue());
				}
                if (bd.size() >= 4) {
					tx.setBorderStyle(PdfBorderDictionary.STYLE_DASHED);
				}
            }
        }
    }

    PdfAppearance getAppearance(final PdfDictionary merged, String text, final String fieldName) throws IOException, DocumentException {
        this.topFirst = 0;
        TextField tx = null;
        if (this.fieldCache == null || !this.fieldCache.containsKey(fieldName)) {
            tx = new TextField(this.writer, null, null);
            tx.setExtraMargin(this.extraMarginLeft, this.extraMarginTop);
            tx.setBorderWidth(0);
            tx.setSubstitutionFonts(this.substitutionFonts);
            decodeGenericDictionary(merged, tx);
            //rect
            final PdfArray rect = merged.getAsArray(PdfName.RECT);
            Rectangle box = PdfReader.getNormalizedRectangle(rect);
            if (tx.getRotation() == 90 || tx.getRotation() == 270) {
				box = box.rotate();
			}
            tx.setBox(box);
            if (this.fieldCache != null) {
				this.fieldCache.put(fieldName, tx);
			}
        }
        else {
            tx = (TextField)this.fieldCache.get(fieldName);
            tx.setWriter(this.writer);
        }
        final PdfName fieldType = merged.getAsName(PdfName.FT);
        if (PdfName.TX.equals(fieldType)) {
            tx.setText(text);
            return tx.getAppearance();
        }
        if (!PdfName.CH.equals(fieldType)) {
			throw new DocumentException("An appearance was requested without a variable text field.");
		}
        final PdfArray opt = merged.getAsArray(PdfName.OPT);
        int flags = 0;
        final PdfNumber nfl = merged.getAsNumber(PdfName.FF);
        if (nfl != null) {
			flags = nfl.intValue();
		}
        if ((flags & PdfFormField.FF_COMBO) != 0 && opt == null) {
            tx.setText(text);
            return tx.getAppearance();
        }
        if (opt != null) {
            final String choices[] = new String[opt.size()];
            final String choicesExp[] = new String[opt.size()];
            for (int k = 0; k < opt.size(); ++k) {
                final PdfObject obj = opt.getPdfObject(k);
                if (obj.isString()) {
                    choices[k] = choicesExp[k] = ((PdfString)obj).toUnicodeString();
                }
                else {
                    final PdfArray a = (PdfArray) obj;
                    choicesExp[k] = a.getAsString(0).toUnicodeString();
                    choices[k] = a.getAsString(1).toUnicodeString();
                }
            }
            if ((flags & PdfFormField.FF_COMBO) != 0) {
                for (int k = 0; k < choices.length; ++k) {
                    if (text.equals(choicesExp[k])) {
                        text = choices[k];
                        break;
                    }
                }
                tx.setText(text);
                return tx.getAppearance();
            }
            int idx = 0;
            for (int k = 0; k < choicesExp.length; ++k) {
                if (text.equals(choicesExp[k])) {
                    idx = k;
                    break;
                }
            }
            tx.setChoices(choices);
            tx.setChoiceExports(choicesExp);
            tx.setChoiceSelection(idx);
        }
        final PdfAppearance app = tx.getListAppearance();
        this.topFirst = tx.getTopFirst();
        return app;
    }

    Color getMKColor(final PdfArray ar) {
        if (ar == null) {
			return null;
		}
        switch (ar.size()) {
            case 1:
                return new GrayColor(ar.getAsNumber(0).floatValue());
            case 3:
                return new Color(ExtendedColor.normalize(ar.getAsNumber(0).floatValue()), ExtendedColor.normalize(ar.getAsNumber(1).floatValue()), ExtendedColor.normalize(ar.getAsNumber(2).floatValue()));
            case 4:
                return new CMYKColor(ar.getAsNumber(0).floatValue(), ar.getAsNumber(1).floatValue(), ar.getAsNumber(2).floatValue(), ar.getAsNumber(3).floatValue());
            default:
                return null;
        }
    }

    /**
     * Gets the field value.
     *
     * @param name the fully qualified field name
     * @return the field value
     */
    public String getField(String name) {
        if (this.xfa.isXfaPresent()) {
            name = this.xfa.findFieldName(name, this);
            if (name == null) {
				return null;
			}
            name = XfaForm.Xml2Som.getShortName(name);
            return XfaForm.getNodeText(this.xfa.findDatasetsNode(name));
        }
        final Item item = this.fields.get(name);
        if (item == null) {
			return null;
		}
        this.lastWasString = false;
        final PdfDictionary mergedDict = item.getMerged( 0 );

        // Jose A. Rodriguez posted a fix to the mailing list (May 11, 2009)
        // explaining that the value can also be a stream value
        // the fix was made against an old iText version. Bruno adapted it.
        final PdfObject v = PdfReader.getPdfObject(mergedDict.get(PdfName.V));
        if (v == null) {
			return "";
		}
        if (v instanceof PRStream) {
                byte[] valBytes;
				try {
					valBytes = PdfReader.getStreamBytes((PRStream)v);
	                return new String(valBytes);
				} catch (final IOException e) {
					throw new ExceptionConverter(e);
				}
        }

        final PdfName type = mergedDict.getAsName(PdfName.FT);
        if (PdfName.BTN.equals(type)) {
            final PdfNumber ff = mergedDict.getAsNumber(PdfName.FF);
            int flags = 0;
            if (ff != null) {
				flags = ff.intValue();
			}
            if ((flags & PdfFormField.FF_PUSHBUTTON) != 0) {
				return "";
			}
            String value = "";
            if (v instanceof PdfName) {
				value = PdfName.decodeName(v.toString());
			} else if (v instanceof PdfString) {
				value = ((PdfString)v).toUnicodeString();
			}
            final PdfArray opts = item.getValue(0).getAsArray(PdfName.OPT);
            if (opts != null) {
                int idx = 0;
                try {
                    idx = Integer.parseInt(value);
                    final PdfString ps = opts.getAsString(idx);
                    value = ps.toUnicodeString();
                    this.lastWasString = true;
                }
                catch (final Exception e) {
                }
            }
            return value;
        }
        if (v instanceof PdfString) {
            this.lastWasString = true;
            return ((PdfString)v).toUnicodeString();
        } else if (v instanceof PdfName) {
            return PdfName.decodeName(v.toString());
        } else {
			return "";
		}
    }

    /**
     * Gets the field values of a Choice field.
     *
     * @param name the fully qualified field name
     * @return the field value
     * @since 2.1.3
     */
    public String[] getListSelection(final String name) {
    	String[] ret;
    	final String s = getField(name);
    	if (s == null) {
    		ret = new String[]{};
    	}
    	else {
    		ret = new String[]{ s };
    	}
        final Item item = this.fields.get(name);
        if (item == null) {
			return ret;
		}
        //PdfName type = (PdfName)PdfReader.getPdfObject(((PdfDictionary)item.merged.get(0)).get(PdfName.FT));
        //if (!PdfName.CH.equals(type)) {
        //	return ret;
        //}
        final PdfArray values = item.getMerged(0).getAsArray(PdfName.I);
        if (values == null) {
			return ret;
		}
        ret = new String[values.size()];
        final String[] options = getListOptionExport(name);
        PdfNumber n;
        int idx = 0;
        for (final Iterator i = values.listIterator(); i.hasNext(); ) {
        	n = (PdfNumber)i.next();
        	ret[idx++] = options[n.intValue()];
        }
        return ret;
    }


    /**
     * Sets a field property. Valid property names are:
     * <ul>
     * <li>textfont - sets the text font. The value for this entry is a <CODE>BaseFont</CODE>.<br>
     * <li>textcolor - sets the text color. The value for this entry is a <CODE>java.awt.Color</CODE>.<br>
     * <li>textsize - sets the text size. The value for this entry is a <CODE>Float</CODE>.
     * <li>bgcolor - sets the background color. The value for this entry is a <CODE>java.awt.Color</CODE>.
     *     If <code>null</code> removes the background.<br>
     * <li>bordercolor - sets the border color. The value for this entry is a <CODE>java.awt.Color</CODE>.
     *     If <code>null</code> removes the border.<br>
     * </ul>
     *
     * @param field the field name
     * @param name the property name
     * @param value the property value
     * @param inst an array of <CODE>int</CODE> indexing into <CODE>AcroField.Item.merged</CODE> elements to process.
     * Set to <CODE>null</CODE> to process all
     * @return <CODE>true</CODE> if the property exists, <CODE>false</CODE> otherwise
     */
    public boolean setFieldProperty(final String field, final String name, final Object value, final int inst[]) {
        if (this.writer == null) {
			throw new RuntimeException("This AcroFields instance is read-only.");
		}
        try {
            final Item item = this.fields.get(field);
            if (item == null) {
				return false;
			}
            final InstHit hit = new InstHit(inst);
            PdfDictionary merged;
            PdfString da;
            if (name.equalsIgnoreCase("textfont")) {
                for (int k = 0; k < item.size(); ++k) {
                    if (hit.isHit(k)) {
                        merged = item.getMerged( k );
                        da = merged.getAsString(PdfName.DA);
                        PdfDictionary dr = merged.getAsDict(PdfName.DR);
                        if (da != null && dr != null) {
                            final Object dao[] = splitDAelements(da.toUnicodeString());
                            final PdfAppearance cb = new PdfAppearance();
                            if (dao[DA_FONT] != null) {
                                final BaseFont bf = (BaseFont)value;
                                PdfName psn = (PdfName)PdfAppearance.stdFieldFontNames.get(bf.getPostscriptFontName());
                                if (psn == null) {
                                    psn = new PdfName(bf.getPostscriptFontName());
                                }
                                PdfDictionary fonts = dr.getAsDict(PdfName.FONT);
                                if (fonts == null) {
                                    fonts = new PdfDictionary();
                                    dr.put(PdfName.FONT, fonts);
                                }
                                final PdfIndirectReference fref = (PdfIndirectReference)fonts.get(psn);
                                final PdfDictionary top = this.reader.getCatalog().getAsDict(PdfName.ACROFORM);
                                markUsed(top);
                                dr = top.getAsDict(PdfName.DR);
                                if (dr == null) {
                                    dr = new PdfDictionary();
                                    top.put(PdfName.DR, dr);
                                }
                                markUsed(dr);
                                PdfDictionary fontsTop = dr.getAsDict(PdfName.FONT);
                                if (fontsTop == null) {
                                    fontsTop = new PdfDictionary();
                                    dr.put(PdfName.FONT, fontsTop);
                                }
                                markUsed(fontsTop);
                                final PdfIndirectReference frefTop = (PdfIndirectReference)fontsTop.get(psn);
                                if (frefTop != null) {
                                    if (fref == null) {
										fonts.put(psn, frefTop);
									}
                                }
                                else if (fref == null) {
                                    FontDetails fd;
                                    if (bf.getFontType() == BaseFont.FONT_TYPE_DOCUMENT) {
                                        fd = new FontDetails(null, ((DocumentFont)bf).getIndirectReference(), bf);
                                    }
                                    else {
                                        bf.setSubset(false);
                                        fd = this.writer.addSimple(bf);
                                        this.localFonts.put(psn.toString().substring(1), bf);
                                    }
                                    fontsTop.put(psn, fd.getIndirectReference());
                                    fonts.put(psn, fd.getIndirectReference());
                                }
                                final ByteBuffer buf = cb.getInternalBuffer();
                                buf.append(psn.getBytes()).append(' ').append(((Float)dao[DA_SIZE]).floatValue()).append(" Tf ");
                                if (dao[DA_COLOR] != null) {
									cb.setColorFill((Color)dao[DA_COLOR]);
								}
                                final PdfString s = new PdfString(cb.toString());
                                item.getMerged(k).put(PdfName.DA, s);
                                item.getWidget(k).put(PdfName.DA, s);
                                markUsed(item.getWidget(k));
                            }
                        }
                    }
                }
            }
            else if (name.equalsIgnoreCase("textcolor")) {
                for (int k = 0; k < item.size(); ++k) {
                    if (hit.isHit(k)) {
                        merged = item.getMerged( k );
                        da = merged.getAsString(PdfName.DA);
                        if (da != null) {
                            final Object dao[] = splitDAelements(da.toUnicodeString());
                            final PdfAppearance cb = new PdfAppearance();
                            if (dao[DA_FONT] != null) {
                                final ByteBuffer buf = cb.getInternalBuffer();
                                buf.append(new PdfName((String)dao[DA_FONT]).getBytes()).append(' ').append(((Float)dao[DA_SIZE]).floatValue()).append(" Tf ");
                                cb.setColorFill((Color)value);
                                final PdfString s = new PdfString(cb.toString());
                                item.getMerged(k).put(PdfName.DA, s);
                                item.getWidget(k).put(PdfName.DA, s);
                                markUsed(item.getWidget(k));
                            }
                        }
                    }
                }
            }
            else if (name.equalsIgnoreCase("textsize")) {
                for (int k = 0; k < item.size(); ++k) {
                    if (hit.isHit(k)) {
                        merged = item.getMerged( k );
                        da = merged.getAsString(PdfName.DA);
                        if (da != null) {
                            final Object dao[] = splitDAelements(da.toUnicodeString());
                            final PdfAppearance cb = new PdfAppearance();
                            if (dao[DA_FONT] != null) {
                                final ByteBuffer buf = cb.getInternalBuffer();
                                buf.append(new PdfName((String)dao[DA_FONT]).getBytes()).append(' ').append(((Float)value).floatValue()).append(" Tf ");
                                if (dao[DA_COLOR] != null) {
									cb.setColorFill((Color)dao[DA_COLOR]);
								}
                                final PdfString s = new PdfString(cb.toString());
                                item.getMerged(k).put(PdfName.DA, s);
                                item.getWidget(k).put(PdfName.DA, s);
                                markUsed(item.getWidget(k));
                            }
                        }
                    }
                }
            }
            else if (name.equalsIgnoreCase("bgcolor") || name.equalsIgnoreCase("bordercolor")) {
                final PdfName dname = name.equalsIgnoreCase("bgcolor") ? PdfName.BG : PdfName.BC;
                for (int k = 0; k < item.size(); ++k) {
                    if (hit.isHit(k)) {
                        merged = item.getMerged( k );
                        PdfDictionary mk = merged.getAsDict(PdfName.MK);
                        if (mk == null) {
                            if (value == null) {
								return true;
							}
                            mk = new PdfDictionary();
                            item.getMerged(k).put(PdfName.MK, mk);
                            item.getWidget(k).put(PdfName.MK, mk);
                            markUsed(item.getWidget(k));
                        } else {
                            markUsed( mk );
                        }
                        if (value == null) {
							mk.remove(dname);
						} else {
							mk.put(dname, PdfAnnotation.getMKColor((Color)value));
						}
                    }
                }
            } else {
				return false;
			}
            return true;
        }
        catch (final Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    /**
     * Sets a field property. Valid property names are:
     * <ul>
     * <li>flags - a set of flags specifying various characteristics of the field's widget annotation.
     * The value of this entry replaces that of the F entry in the form's corresponding annotation dictionary.<br>
     * <li>setflags - a set of flags to be set (turned on) in the F entry of the form's corresponding
     * widget annotation dictionary. Bits equal to 1 cause the corresponding bits in F to be set to 1.<br>
     * <li>clrflags - a set of flags to be cleared (turned off) in the F entry of the form's corresponding
     * widget annotation dictionary. Bits equal to 1 cause the corresponding
     * bits in F to be set to 0.<br>
     * <li>fflags - a set of flags specifying various characteristics of the field. The value
     * of this entry replaces that of the Ff entry in the form's corresponding field dictionary.<br>
     * <li>setfflags - a set of flags to be set (turned on) in the Ff entry of the form's corresponding
     * field dictionary. Bits equal to 1 cause the corresponding bits in Ff to be set to 1.<br>
     * <li>clrfflags - a set of flags to be cleared (turned off) in the Ff entry of the form's corresponding
     * field dictionary. Bits equal to 1 cause the corresponding bits in Ff
     * to be set to 0.<br>
     * </ul>
     *
     * @param field the field name
     * @param name the property name
     * @param value the property value
     * @param inst an array of <CODE>int</CODE> indexing into <CODE>AcroField.Item.merged</CODE> elements to process.
     * Set to <CODE>null</CODE> to process all
     * @return <CODE>true</CODE> if the property exists, <CODE>false</CODE> otherwise
     */
    public boolean setFieldProperty(final String field, final String name, final int value, final int inst[]) {
        if (this.writer == null) {
			throw new RuntimeException("This AcroFields instance is read-only.");
		}
        final Item item = this.fields.get(field);
        if (item == null) {
			return false;
		}
        final InstHit hit = new InstHit(inst);
        if (name.equalsIgnoreCase("flags")) {
            final PdfNumber num = new PdfNumber(value);
            for (int k = 0; k < item.size(); ++k) {
                if (hit.isHit(k)) {
                    item.getMerged(k).put(PdfName.F, num);
                    item.getWidget(k).put(PdfName.F, num);
                    markUsed(item.getWidget(k));
                }
            }
        }
        else if (name.equalsIgnoreCase("setflags")) {
            for (int k = 0; k < item.size(); ++k) {
                if (hit.isHit(k)) {
                    PdfNumber num = item.getWidget(k).getAsNumber(PdfName.F);
                    int val = 0;
                    if (num != null) {
						val = num.intValue();
					}
                    num = new PdfNumber(val | value);
                    item.getMerged(k).put(PdfName.F, num);
                    item.getWidget(k).put(PdfName.F, num);
                    markUsed(item.getWidget(k));
                }
            }
        }
        else if (name.equalsIgnoreCase("clrflags")) {
            for (int k = 0; k < item.size(); ++k) {
                if (hit.isHit(k)) {
                    final PdfDictionary widget = item.getWidget( k );
                    PdfNumber num = widget.getAsNumber(PdfName.F);
                    int val = 0;
                    if (num != null) {
						val = num.intValue();
					}
                    num = new PdfNumber(val & ~value);
                    item.getMerged(k).put(PdfName.F, num);
                    widget.put(PdfName.F, num);
                    markUsed(widget);
                }
            }
        }
        else if (name.equalsIgnoreCase("fflags")) {
            final PdfNumber num = new PdfNumber(value);
            for (int k = 0; k < item.size(); ++k) {
                if (hit.isHit(k)) {
                    item.getMerged(k).put(PdfName.FF, num);
                    item.getValue(k).put(PdfName.FF, num);
                    markUsed(item.getValue(k));
                }
            }
        }
        else if (name.equalsIgnoreCase("setfflags")) {
            for (int k = 0; k < item.size(); ++k) {
                if (hit.isHit(k)) {
                    final PdfDictionary valDict = item.getValue( k );
                    PdfNumber num = valDict.getAsNumber( PdfName.FF );
                    int val = 0;
                    if (num != null) {
						val = num.intValue();
					}
                    num = new PdfNumber(val | value);
                    item.getMerged(k).put(PdfName.FF, num);
                    valDict.put(PdfName.FF, num);
                    markUsed(valDict);
                }
            }
        }
        else if (name.equalsIgnoreCase("clrfflags")) {
            for (int k = 0; k < item.size(); ++k) {
                if (hit.isHit(k)) {
                    final PdfDictionary valDict = item.getValue( k );
                    PdfNumber num = valDict.getAsNumber(PdfName.FF);
                    int val = 0;
                    if (num != null) {
						val = num.intValue();
					}
                    num = new PdfNumber(val & ~value);
                    item.getMerged(k).put(PdfName.FF, num);
                    valDict.put(PdfName.FF, num);
                    markUsed(valDict);
                }
            }
        } else {
			return false;
		}
        return true;
    }

    /**
     * Merges an XML data structure into this form.
     *
     * @param n the top node of the data structure
     * @throws java.io.IOException on error
     * @throws com.aowagie.text.DocumentException on error
     */
    public void mergeXfaData(final Node n) throws IOException, DocumentException {
        final XfaForm.Xml2SomDatasets data = new XfaForm.Xml2SomDatasets(n);
        for (final Iterator it = data.getOrder().iterator(); it.hasNext();) {
            final String name = (String)it.next();
            final String text = XfaForm.getNodeText((Node)data.getName2Node().get(name));
            setField(name, text);
        }
    }

    /**
     * Sets the fields by FDF merging.
     *
     * @param fdf the FDF form
     * @throws IOException on error
     * @throws DocumentException on error
     */
    public void setFields(final FdfReader fdf) throws IOException, DocumentException {
        final HashMap fd = fdf.getFields();
        for (final Iterator i = fd.keySet().iterator(); i.hasNext();) {
            final String f = (String)i.next();
            final String v = fdf.getFieldValue(f);
            if (v != null) {
				setField(f, v);
			}
        }
    }

    /**
     * Sets the fields by XFDF merging.
     *
     * @param xfdf the XFDF form
     * @throws IOException on error
     * @throws DocumentException on error
     */
    public void setFields(final XfdfReader xfdf) throws IOException, DocumentException {
        final HashMap fd = xfdf.getFields();
        for (final Iterator i = fd.keySet().iterator(); i.hasNext();) {
            final String f = (String)i.next();
            final String v = xfdf.getFieldValue(f);
            if (v != null) {
				setField(f, v);
			}
            final List l = xfdf.getListValues(f);
            if (l != null) {
				setListSelection(v, (String[])l.toArray(new String[l.size()]));
			}
        }
    }

    /**
     * Regenerates the field appearance.
     * This is useful when you change a field property, but not its value,
     * for instance form.setFieldProperty("f", "bgcolor", Color.BLUE, null);
     * This won't have any effect, unless you use regenerateField("f") after changing
     * the property.
     *
     * @param name the fully qualified field name or the partial name in the case of XFA forms
     * @throws IOException on error
     * @throws DocumentException on error
     * @return <CODE>true</CODE> if the field was found and changed,
     * <CODE>false</CODE> otherwise
     */
    public boolean regenerateField(final String name) throws IOException, DocumentException {
    	final String value = getField(name);
        return setField(name, value, value);
    }

    /**
     * Sets the field value.
     *
     * @param name the fully qualified field name or the partial name in the case of XFA forms
     * @param value the field value
     * @throws IOException on error
     * @throws DocumentException on error
     * @return <CODE>true</CODE> if the field was found and changed,
     * <CODE>false</CODE> otherwise
     */
    public boolean setField(final String name, final String value) throws IOException, DocumentException {
        return setField(name, value, null);
    }

    /**
     * Sets the field value and the display string. The display string
     * is used to build the appearance in the cases where the value
     * is modified by Acrobat with JavaScript and the algorithm is
     * known.
     *
     * @param name the fully qualified field name or the partial name in the case of XFA forms
     * @param value the field value
     * @param display the string that is used for the appearance. If <CODE>null</CODE>
     * the <CODE>value</CODE> parameter will be used
     * @return <CODE>true</CODE> if the field was found and changed,
     * <CODE>false</CODE> otherwise
     * @throws IOException on error
     * @throws DocumentException on error
     */
    public boolean setField(String name, String value, String display) throws IOException, DocumentException {
        if (this.writer == null) {
			throw new DocumentException("This AcroFields instance is read-only.");
		}
        if (this.xfa.isXfaPresent()) {
            name = this.xfa.findFieldName(name, this);
            if (name == null) {
				return false;
			}
            final String shortName = XfaForm.Xml2Som.getShortName(name);
            Node xn = this.xfa.findDatasetsNode(shortName);
            if (xn == null) {
                xn = this.xfa.getDatasetsSom().insertNode(this.xfa.getDatasetsNode(), shortName);
            }
            this.xfa.setNodeText(xn, value);
        }
        final Item item = this.fields.get(name);
        if (item == null) {
			return false;
		}
        PdfDictionary merged = item.getMerged( 0 );
        final PdfName type = merged.getAsName(PdfName.FT);
        if (PdfName.TX.equals(type)) {
            final PdfNumber maxLen = merged.getAsNumber(PdfName.MAXLEN);
            int len = 0;
            if (maxLen != null) {
				len = maxLen.intValue();
			}
            if (len > 0) {
				value = value.substring(0, Math.min(len, value.length()));
			}
        }
        if (display == null) {
			display = value;
		}
        if (PdfName.TX.equals(type) || PdfName.CH.equals(type)) {
            final PdfString v = new PdfString(value, PdfObject.TEXT_UNICODE);
            for (int idx = 0; idx < item.size(); ++idx) {
                final PdfDictionary valueDic = item.getValue(idx);
                valueDic.put(PdfName.V, v);
                valueDic.remove(PdfName.I);
                markUsed(valueDic);
                merged = item.getMerged(idx);
                merged.remove(PdfName.I);
                merged.put(PdfName.V, v);
                final PdfDictionary widget = item.getWidget(idx);
                if (this.generateAppearances) {
                    final PdfAppearance app = getAppearance(merged, display, name);
                    if (PdfName.CH.equals(type)) {
                        final PdfNumber n = new PdfNumber(this.topFirst);
                        widget.put(PdfName.TI, n);
                        merged.put(PdfName.TI, n);
                    }
                    PdfDictionary appDic = widget.getAsDict(PdfName.AP);
                    if (appDic == null) {
                        appDic = new PdfDictionary();
                        widget.put(PdfName.AP, appDic);
                        merged.put(PdfName.AP, appDic);
                    }
                    appDic.put(PdfName.N, app.getIndirectReference());
                    this.writer.releaseTemplate(app);
                }
                else {
                    widget.remove(PdfName.AP);
                    merged.remove(PdfName.AP);
                }
                markUsed(widget);
            }
            return true;
        }
        else if (PdfName.BTN.equals(type)) {
            final PdfNumber ff = item.getMerged(0).getAsNumber(PdfName.FF);
            int flags = 0;
            if (ff != null) {
				flags = ff.intValue();
			}
            if ((flags & PdfFormField.FF_PUSHBUTTON) != 0) {
                //we'll assume that the value is an image in base64
                Image img;
                try {
                    img = Image.getInstance(Base64.decode(value));
                }
                catch (final Exception e) {
                    return false;
                }
                final PushbuttonField pb = getNewPushbuttonFromField(name);
                pb.setImage(img);
                replacePushbuttonField(name, pb.getField());
                return true;
            }
            final PdfName v = new PdfName(value);
            final ArrayList lopt = new ArrayList();
            final PdfArray opts = item.getValue(0).getAsArray(PdfName.OPT);
            if (opts != null) {
                for (int k = 0; k < opts.size(); ++k) {
                    final PdfString valStr = opts.getAsString(k);
                    if (valStr != null) {
						lopt.add(valStr.toUnicodeString());
					} else {
						lopt.add(null);
					}
                }
            }
            final int vidx = lopt.indexOf(value);
            PdfName valt = null;
            PdfName vt;
            if (vidx >= 0) {
                vt = valt = new PdfName(String.valueOf(vidx));
            } else {
				vt = v;
			}
            for (int idx = 0; idx < item.size(); ++idx) {
                merged = item.getMerged(idx);
                final PdfDictionary widget = item.getWidget(idx);
                final PdfDictionary valDict = item.getValue(idx);
                markUsed(item.getValue(idx));
                if (valt != null) {
                    final PdfString ps = new PdfString(value, PdfObject.TEXT_UNICODE);
                    valDict.put(PdfName.V, ps);
                    merged.put(PdfName.V, ps);
                }
                else {
                    valDict.put(PdfName.V, v);
                    merged.put(PdfName.V, v);
                }
                markUsed(widget);
                if (isInAP(widget,  vt)) {
                    merged.put(PdfName.AS, vt);
                    widget.put(PdfName.AS, vt);
                }
                else {
                    merged.put(PdfName.AS, PdfName.Off);
                    widget.put(PdfName.AS, PdfName.Off);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Sets different values in a list selection.
     * No appearance is generated yet; nor does the code check if multiple select is allowed.
     *
     * @param	name	the name of the field
     * @param	value	an array with values that need to be selected
     * @return	true only if the field value was changed
     * @since 2.1.4
     * @throws IOException Exception with information about a I/O error
     * @throws DocumentException Exception with information about a document error
     */
	public boolean setListSelection(final String name, final String[] value) throws IOException, DocumentException {
        final Item item = getFieldItem(name);
        if (item == null) {
			return false;
		}
        final PdfName type = item.getMerged(0).getAsName(PdfName.FT);
        if (!PdfName.CH.equals(type)) {
        	return false;
        }
        final String[] options = getListOptionExport(name);
        final PdfArray array = new PdfArray();
        for (final String element : value) {
        	for (int j = 0; j < options.length; j++) {
        		if (options[j].equals(element)) {
        			array.add(new PdfNumber(j));
        		}
        	}
        }
        item.writeToAll(PdfName.I, array, Item.WRITE_MERGED | Item.WRITE_VALUE);
        item.writeToAll(PdfName.V, null, Item.WRITE_MERGED | Item.WRITE_VALUE);
        item.writeToAll(PdfName.AP, null, Item.WRITE_MERGED | Item.WRITE_WIDGET);
        item.markUsed( this, Item.WRITE_VALUE | Item.WRITE_WIDGET );
        return true;
	}

    boolean isInAP(final PdfDictionary dic, final PdfName check) {
        final PdfDictionary appDic = dic.getAsDict(PdfName.AP);
        if (appDic == null) {
			return false;
		}
        final PdfDictionary NDic = appDic.getAsDict(PdfName.N);
        return NDic != null && NDic.get(check) != null;
    }

    /**
     * Gets all the fields. The fields are keyed by the fully qualified field name and
     * the value is an instance of <CODE>AcroFields.Item</CODE>.
     *
     * @return all the fields
     */
    public HashMap<String, AcroFields.Item> getFields() {
        return this.fields;
    }

    /**
     * Gets the field structure.
     *
     * @param name the name of the field
     * @return the field structure or <CODE>null</CODE> if the field
     * does not exist
     */
    public Item getFieldItem(String name) {
        if (this.xfa.isXfaPresent()) {
            name = this.xfa.findFieldName(name, this);
            if (name == null) {
				return null;
			}
        }
        return this.fields.get(name);
    }

    /**
     * Gets the long XFA translated name.
     *
     * @param name the name of the field
     * @return the long field name
     */
    public String getTranslatedFieldName(String name) {
        if (this.xfa.isXfaPresent()) {
            final String namex = this.xfa.findFieldName(name, this);
            if (namex != null) {
				name = namex;
			}
        }
        return name;
    }

    /**
     * Gets the field box positions in the document. The return is an array of <CODE>float</CODE>
     * multiple of 5. For each of this groups the values are: [page, llx, lly, urx,
     * ury]. The coordinates have the page rotation in consideration.
     *
     * @param name the field name
     * @return the positions or <CODE>null</CODE> if field does not exist
     */
    public float[] getFieldPositions(final String name) {
        final Item item = getFieldItem(name);
        if (item == null) {
			return null;
		}
        final float ret[] = new float[item.size() * 5];
        int ptr = 0;
        for (int k = 0; k < item.size(); ++k) {
            try {
                final PdfDictionary wd = item.getWidget(k);
                final PdfArray rect = wd.getAsArray(PdfName.RECT);
                if (rect == null) {
					continue;
				}
                Rectangle r = PdfReader.getNormalizedRectangle(rect);
                final int page = item.getPage(k).intValue();
                final int rotation = this.reader.getPageRotation(page);
                ret[ptr++] = page;
                if (rotation != 0) {
                    final Rectangle pageSize = this.reader.getPageSize(page);
                    switch (rotation) {
                        case 270:
                            r = new Rectangle(
                                pageSize.getTop() - r.getBottom(),
                                r.getLeft(),
                                pageSize.getTop() - r.getTop(),
                                r.getRight());
                            break;
                        case 180:
                            r = new Rectangle(
                                pageSize.getRight() - r.getLeft(),
                                pageSize.getTop() - r.getBottom(),
                                pageSize.getRight() - r.getRight(),
                                pageSize.getTop() - r.getTop());
                            break;
                        case 90:
                            r = new Rectangle(
                                r.getBottom(),
                                pageSize.getRight() - r.getLeft(),
                                r.getTop(),
                                pageSize.getRight() - r.getRight());
                            break;
                    }
                    r.normalize();
                }
                ret[ptr++] = r.getLeft();
                ret[ptr++] = r.getBottom();
                ret[ptr++] = r.getRight();
                ret[ptr++] = r.getTop();
            }
            catch (final Exception e) {
                // empty on purpose
            }
        }
        if (ptr < ret.length) {
            final float ret2[] = new float[ptr];
            System.arraycopy(ret, 0, ret2, 0, ptr);
            return ret2;
        }
        return ret;
    }

    private int removeRefFromArray(final PdfArray array, final PdfObject refo) {
        if (refo == null || !refo.isIndirect()) {
			return array.size();
		}
        final PdfIndirectReference ref = (PdfIndirectReference)refo;
        for (int j = 0; j < array.size(); ++j) {
            final PdfObject obj = array.getPdfObject(j);
            if (!obj.isIndirect()) {
				continue;
			}
            if (((PdfIndirectReference)obj).getNumber() == ref.getNumber()) {
				array.remove(j--);
			}
        }
        return array.size();
    }

    /**
     * Removes all the fields from <CODE>page</CODE>.
     *
     * @param page the page to remove the fields from
     * @return <CODE>true</CODE> if any field was removed, <CODE>false otherwise</CODE>
     */
    public boolean removeFieldsFromPage(final int page) {
        if (page < 1) {
			return false;
		}
        final String names[] = new String[this.fields.size()];
        this.fields.keySet().toArray(names);
        boolean found = false;
        for (final String name : names) {
            final boolean fr = removeField(name, page);
            found = found || fr;
        }
        return found;
    }

    /**
     * Removes a field from the document. If page equals -1 all the fields with this
     * <CODE>name</CODE> are removed from the document otherwise only the fields in
     * that particular page are removed.
     *
     * @param name the field name
     * @param page the page to remove the field from or -1 to remove it from all the pages
     * @return <CODE>true</CODE> if the field exists, <CODE>false otherwise</CODE>
     */
    public boolean removeField(final String name, final int page) {
        final Item item = getFieldItem(name);
        if (item == null) {
			return false;
		}
        final PdfDictionary acroForm = (PdfDictionary)PdfReader.getPdfObject(this.reader.getCatalog().get(PdfName.ACROFORM), this.reader.getCatalog());

        if (acroForm == null) {
			return false;
		}
        final PdfArray arrayf = acroForm.getAsArray(PdfName.FIELDS);
        if (arrayf == null) {
			return false;
		}
        for (int k = 0; k < item.size(); ++k) {
            final int pageV = item.getPage(k).intValue();
            if (page != -1 && page != pageV) {
				continue;
			}
            PdfIndirectReference ref = item.getWidgetRef(k);
            PdfDictionary wd = item.getWidget( k );
            final PdfDictionary pageDic = this.reader.getPageN(pageV);
            final PdfArray annots = pageDic.getAsArray(PdfName.ANNOTS);
            if (annots != null) {
                if (removeRefFromArray(annots, ref) == 0) {
                    pageDic.remove(PdfName.ANNOTS);
                    markUsed(pageDic);
                } else {
					markUsed(annots);
				}
            }
            PdfReader.killIndirect(ref);
            PdfIndirectReference kid = ref;
            while ((ref = wd.getAsIndirectObject(PdfName.PARENT)) != null) {
                wd = wd.getAsDict( PdfName.PARENT );
                final PdfArray kids = wd.getAsArray(PdfName.KIDS);
                if (removeRefFromArray(kids, kid) != 0) {
					break;
				}
                kid = ref;
                PdfReader.killIndirect(ref);
            }
            if (ref == null) {
                removeRefFromArray(arrayf, kid);
                markUsed(arrayf);
            }
            if (page != -1) {
                item.remove( k );
                --k;
            }
        }
        if (page == -1 || item.size() == 0) {
			this.fields.remove(name);
		}
        return true;
    }

    /**
     * Removes a field from the document.
     *
     * @param name the field name
     * @return <CODE>true</CODE> if the field exists, <CODE>false otherwise</CODE>
     */
    public boolean removeField(final String name) {
        return removeField(name, -1);
    }

    /**
     * Gets the property generateAppearances.
     *
     * @return the property generateAppearances
     */
    public boolean isGenerateAppearances() {
        return this.generateAppearances;
    }

    /**
     * Sets the option to generate appearances. Not generating appearances
     * will speed-up form filling but the results can be
     * unexpected in Acrobat. Don't use it unless your environment is well
     * controlled. The default is <CODE>true</CODE>.
     *
     * @param generateAppearances the option to generate appearances
     */
    public void setGenerateAppearances(final boolean generateAppearances) {
        this.generateAppearances = generateAppearances;
        final PdfDictionary top = this.reader.getCatalog().getAsDict(PdfName.ACROFORM);
        if (generateAppearances) {
			top.remove(PdfName.NEEDAPPEARANCES);
		} else {
			top.put(PdfName.NEEDAPPEARANCES, PdfBoolean.PDFTRUE);
		}
    }

    /** The field representations for retrieval and modification. */
    public static class Item {

        /**
         * <CODE>writeToAll</CODE> constant.
         *
         *  @since 2.1.5
         */
        public static final int WRITE_MERGED = 1;

        /**
         * <CODE>writeToAll</CODE> and <CODE>markUsed</CODE> constant.
         *
         *  @since 2.1.5
         */
        public static final int WRITE_WIDGET = 2;

        /**
         * <CODE>writeToAll</CODE> and <CODE>markUsed</CODE> constant.
         *
         *  @since 2.1.5
         */
        public static final int WRITE_VALUE = 4;

        /**
         * This function writes the given key/value pair to all the instances
         * of merged, widget, and/or value, depending on the <code>writeFlags</code> setting
         *
         * @since 2.1.5
         *
         * @param key        you'll never guess what this is for.
         * @param value      if value is null, the key will be removed
         * @param writeFlags ORed together WRITE_* flags
         */
        public void writeToAll(final PdfName key, final PdfObject value, final int writeFlags) {
            int i;
            PdfDictionary curDict = null;
            if ((writeFlags & WRITE_MERGED) != 0) {
                for (i = 0; i < this.merged.size(); ++i) {
                    curDict = getMerged(i);
                    curDict.put(key, value);
                }
            }
            if ((writeFlags & WRITE_WIDGET) != 0) {
                for (i = 0; i < this.widgets.size(); ++i) {
                    curDict = getWidget(i);
                    curDict.put(key, value);
                }
            }
            if ((writeFlags & WRITE_VALUE) != 0) {
                for (i = 0; i < this.values.size(); ++i) {
                    curDict = getValue(i);
                    curDict.put(key, value);
                }
            }
        }

        /**
         * Mark all the item dictionaries used matching the given flags
         *
         * @since 2.1.5
         * @param parentFields Fields to mark
         * @param writeFlags WRITE_MERGED is ignored
         */
        public void markUsed( final AcroFields parentFields, final int writeFlags ) {
            if ((writeFlags & WRITE_VALUE) != 0) {
                for (int i = 0; i < size(); ++i) {
                    parentFields.markUsed( getValue( i ) );
                }
            }
            if ((writeFlags & WRITE_WIDGET) != 0) {
                for (int i = 0; i < size(); ++i) {
                    parentFields.markUsed(getWidget(i));
                }
            }
        }

        /**
         * An array of <CODE>PdfDictionary</CODE> where the value tag /V
         * is present.
         *
         * @deprecated (will remove 'public' in the future)
         */
        @Deprecated
		public ArrayList values = new ArrayList();

        /**
         * An array of <CODE>PdfDictionary</CODE> with the widgets.
         *
         * @deprecated (will remove 'public' in the future)
         */
        @Deprecated
		public ArrayList widgets = new ArrayList();

        /**
         * An array of <CODE>PdfDictionary</CODE> with the widget references.
         *
         * @deprecated (will remove 'public' in the future)
         */
        @Deprecated
		public ArrayList widget_refs = new ArrayList();

        /**
         * An array of <CODE>PdfDictionary</CODE> with all the field
         * and widget tags merged.
         *
         * @deprecated (will remove 'public' in the future)
         */
        @Deprecated
		public ArrayList merged = new ArrayList();

        /**
         * An array of <CODE>Integer</CODE> with the page numbers where
         * the widgets are displayed.
         *
         * @deprecated (will remove 'public' in the future)
         */
        @Deprecated
		public ArrayList page = new ArrayList();
        /**
         * An array of <CODE>Integer</CODE> with the tab order of the field in the page.
         *
         * @deprecated (will remove 'public' in the future)
         */
        @Deprecated
		public ArrayList tabOrder = new ArrayList();

        /**
         * Preferred method of determining the number of instances
         * of a given field.
         *
         * @since 2.1.5
         * @return number of instances
         */
        public int size() {
            return this.values.size();
        }

        /**
         * Remove the given instance from this item.  It is possible to
         * remove all instances using this function.
         *
         * @since 2.1.5
         * @param killIdx
         */
        void remove(final int killIdx) {
            this.values.remove(killIdx);
            this.widgets.remove(killIdx);
            this.widget_refs.remove(killIdx);
            this.merged.remove(killIdx);
            this.page.remove(killIdx);
            this.tabOrder.remove(killIdx);
        }

        /**
         * Retrieve the value dictionary of the given instance
         *
         * @since 2.1.5
         * @param idx instance index
         * @return dictionary storing this instance's value.  It may be shared across instances.
         */
        public PdfDictionary getValue(final int idx) {
            return (PdfDictionary) this.values.get(idx);
        }

        /**
         * Add a value dict to this Item
         *
         * @since 2.1.5
         * @param value new value dictionary
         */
        void addValue(final PdfDictionary value) {
            this.values.add(value);
        }

        /**
         * Retrieve the widget dictionary of the given instance
         *
         * @since 2.1.5
         * @param idx instance index
         * @return The dictionary found in the appropriate page's Annot array.
         */
        public PdfDictionary getWidget(final int idx) {
            return (PdfDictionary) this.widgets.get(idx);
        }

        /**
         * Add a widget dict to this Item
         *
         * @since 2.1.5
         * @param widget
         */
        void addWidget(final PdfDictionary widget) {
            this.widgets.add(widget);
        }

        /**
         * Retrieve the reference to the given instance
         *
         * @since 2.1.5
         * @param idx instance index
         * @return reference to the given field instance
         */
        public PdfIndirectReference getWidgetRef(final int idx) {
            return (PdfIndirectReference) this.widget_refs.get(idx);
        }

        /**
         * Add a widget ref to this Item
         *
         * @since 2.1.5
         * @param widgRef
         */
        void addWidgetRef(final PdfIndirectReference widgRef) {
            this.widget_refs.add(widgRef);
        }

        /**
         * Retrieve the merged dictionary for the given instance.  The merged
         * dictionary contains all the keys present in parent fields, though they
         * may have been overwritten (or modified?) by children.
         * Example: a merged radio field dict will contain /V
         *
         * @since 2.1.5
         * @param idx  instance index
         * @return the merged dictionary for the given instance
         */
        public PdfDictionary getMerged(final int idx) {
            return (PdfDictionary) this.merged.get(idx);
        }

        /**
         * Adds a merged dictionary to this Item.
         *
         * @since 2.1.5
         * @param mergeDict
         */
        void addMerged(final PdfDictionary mergeDict) {
            this.merged.add(mergeDict);
        }

        /**
         * Retrieve the page number of the given instance
         *
         * @since 2.1.5
         * @param idx Number of page
         * @return remember, pages are "1-indexed", not "0-indexed" like field instances.
         */
        public Integer getPage(final int idx) {
            return (Integer) this.page.get(idx);
        }

        /**
         * Adds a page to the current Item.
         *
         * @since 2.1.5
         * @param pg
         */
        void addPage(final int pg) {
            this.page.add(new Integer(pg));
        }

        /**
         * forces a page value into the Item.
         *
         * @since 2.1.5
         * @param idx
         */
        void forcePage(final int idx, final int pg) {
            this.page.set(idx, new Integer( pg ));
        }

        /**
         * Gets the tabOrder.
         *
         * @since 2.1.5
         * @param idx Number of tab
         * @return tab index of the given field instance
         */
        public Integer getTabOrder(final int idx) {
            return (Integer) this.tabOrder.get(idx);
        }

        /**
         * Adds a tab order value to this Item.
         *
         * @since 2.1.5
         * @param order
         */
        void addTabOrder(final int order) {
            this.tabOrder.add(new Integer(order));
        }
    }

    private static class InstHit {
        IntHashtable hits;
        public InstHit(final int inst[]) {
            if (inst == null) {
				return;
			}
            this.hits = new IntHashtable();
            for (final int element : inst) {
				this.hits.put(element, 1);
			}
        }

        public boolean isHit(final int n) {
            if (this.hits == null) {
				return true;
			}
            return this.hits.containsKey(n);
        }
    }

    /** Gets the field names that have signatures and are signed.
     * @return the field names that have signatures and are signed. */
    public List<String> getSignatureNames() {
        if (this.sigNames != null) {
			return new ArrayList(this.sigNames.keySet());
		}
        this.sigNames = new HashMap();
        final ArrayList sorter = new ArrayList();
        for (final Object element : this.fields.entrySet()) {
            final Map.Entry entry = (Map.Entry)element;
            final Item item = (Item)entry.getValue();
            final PdfDictionary merged = item.getMerged(0);
            if (!PdfName.SIG.equals(merged.get(PdfName.FT))) {
				continue;
			}
            final PdfDictionary v = merged.getAsDict(PdfName.V);
            if (v == null) {
				continue;
			}
            final PdfString contents = v.getAsString(PdfName.CONTENTS);
            if (contents == null) {
				continue;
			}
            final PdfArray ro = v.getAsArray(PdfName.BYTERANGE);
            if (ro == null) {
				continue;
			}
            final int rangeSize = ro.size();
            if (rangeSize < 2) {
				continue;
			}
            final int length = ro.getAsNumber(rangeSize - 1).intValue() + ro.getAsNumber(rangeSize - 2).intValue();
            sorter.add(new Object[]{entry.getKey(), new int[]{length, 0}});
        }
        Collections.sort(sorter, new AcroFields.SorterComparator());
        if (!sorter.isEmpty()) {
            if (((int[])((Object[])sorter.get(sorter.size() - 1))[1])[0] == this.reader.getFileLength()) {
				this.totalRevisions = sorter.size();
			} else {
				this.totalRevisions = sorter.size() + 1;
			}
            for (int k = 0; k < sorter.size(); ++k) {
                final Object objs[] = (Object[])sorter.get(k);
                final String name = (String)objs[0];
                final int p[] = (int[])objs[1];
                p[1] = k + 1;
                this.sigNames.put(name, p);
            }
        }
        return new ArrayList(this.sigNames.keySet());
    }

    /** Gets the field names that have blank signatures.
     * @return the field names that have blank signatures. */
    public List<String> getBlankSignatureNames() {
        getSignatureNames();
        final ArrayList sigs = new ArrayList();
        for (final Object element : this.fields.entrySet()) {
            final Map.Entry entry = (Map.Entry)element;
            final Item item = (Item)entry.getValue();
            final PdfDictionary merged = item.getMerged(0);
            if (!PdfName.SIG.equals(merged.getAsName(PdfName.FT))) {
				continue;
			}
            if (this.sigNames.containsKey(entry.getKey())) {
				continue;
			}
            sigs.add(entry.getKey());
        }
        return sigs;
    }

    /**
     * Gets the signature dictionary, the one keyed by /V.
     *
     * @param name the field name
     * @return the signature dictionary keyed by /V or <CODE>null</CODE> if the field is not
     * a signature
     */
    public PdfDictionary getSignatureDictionary(String name) {
        getSignatureNames();
        name = getTranslatedFieldName(name);
        if (!this.sigNames.containsKey(name)) {
			return null;
		}
        final Item item = this.fields.get(name);
        final PdfDictionary merged = item.getMerged(0);
        return merged.getAsDict(PdfName.V);
    }

    /**
     * Checks is the signature covers the entire document or just part of it.
     *
     * @param name the signature field name
     * @return <CODE>true</CODE> if the signature covers the entire document,
     * <CODE>false</CODE> otherwise
     */
    public boolean signatureCoversWholeDocument(String name) {
        getSignatureNames();
        name = getTranslatedFieldName(name);
        if (!this.sigNames.containsKey(name)) {
			return false;
		}
        return ((int[])this.sigNames.get(name))[0] == this.reader.getFileLength();
    }

    /**
     * Verifies a signature. An example usage is:
     * <pre>
     * KeyStore kall = PdfPKCS7.loadCacertsKeyStore();
     * PdfReader reader = new PdfReader("my_signed_doc.pdf");
     * AcroFields af = reader.getAcroFields();
     * ArrayList names = af.getSignatureNames();
     * for (int k = 0; k &lt; names.size(); ++k) {
     *    String name = (String)names.get(k);
     *    System.out.println("Signature name: " + name);
     *    System.out.println("Signature covers whole document: " + af.signatureCoversWholeDocument(name));
     *    PdfPKCS7 pk = af.verifySignature(name);
     *    Calendar cal = pk.getSignDate();
     *    Certificate pkc[] = pk.getCertificates();
     *    System.out.println("Subject: " + PdfPKCS7.getSubjectFields(pk.getSigningCertificate()));
     *    System.out.println("Document modified: " + !pk.verify());
     *    Object fails[] = PdfPKCS7.verifyCertificates(pkc, kall, null, cal);
     *    if (fails == null)
     *        System.out.println("Certificates verified against the KeyStore");
     *    else
     *        System.out.println("Certificate failed: " + fails[1]);
     * }
     * </pre>
     * @param name the signature field name
     * @return a <CODE>PdfPKCS7</CODE> class to continue the verification
     */
    public PdfPKCS7 verifySignature(final String name) {
        return verifySignature(name, null);
    }

    /**
     * Verifies a signature. An example usage is:
     * <pre>
     * KeyStore kall = PdfPKCS7.loadCacertsKeyStore();
     * PdfReader reader = new PdfReader("my_signed_doc.pdf");
     * AcroFields af = reader.getAcroFields();
     * ArrayList names = af.getSignatureNames();
     * for (int k = 0; k &lt; names.size(); ++k) {
     *    String name = (String)names.get(k);
     *    System.out.println("Signature name: " + name);
     *    System.out.println("Signature covers whole document: " + af.signatureCoversWholeDocument(name));
     *    PdfPKCS7 pk = af.verifySignature(name);
     *    Calendar cal = pk.getSignDate();
     *    Certificate pkc[] = pk.getCertificates();
     *    System.out.println("Subject: " + PdfPKCS7.getSubjectFields(pk.getSigningCertificate()));
     *    System.out.println("Document modified: " + !pk.verify());
     *    Object fails[] = PdfPKCS7.verifyCertificates(pkc, kall, null, cal);
     *    if (fails == null)
     *        System.out.println("Certificates verified against the KeyStore");
     *    else
     *        System.out.println("Certificate failed: " + fails[1]);
     * }
     * </pre>
     * @param name the signature field name
     * @param provider the provider or <code>null</code> for the default provider
     * @return a <CODE>PdfPKCS7</CODE> class to continue the verification
     */
    public PdfPKCS7 verifySignature(final String name, final String provider) {
        final PdfDictionary v = getSignatureDictionary(name);
        if (v == null) {
			return null;
		}
        try {
            final PdfName sub = v.getAsName(PdfName.SUBFILTER);
            final PdfString contents = v.getAsString(PdfName.CONTENTS);
            PdfPKCS7 pk = null;
            if (sub.equals(PdfName.ADBE_X509_RSA_SHA1)) {
                final PdfString cert = v.getAsString(PdfName.CERT);
                pk = new PdfPKCS7(contents.getOriginalBytes(), cert.getBytes(), provider);
            } else {
				pk = new PdfPKCS7(contents.getOriginalBytes(), provider);
			}
            updateByteRange(pk, v);
            PdfString str = v.getAsString(PdfName.M);
            if (str != null) {
				pk.setSignDate(PdfDate.decode(str.toString()));
			}
            final PdfObject obj = PdfReader.getPdfObject(v.get(PdfName.NAME));
            if (obj != null) {
              if (obj.isString()) {
				pk.setSignName(((PdfString)obj).toUnicodeString());
			} else if(obj.isName()) {
				pk.setSignName(PdfName.decodeName(obj.toString()));
			}
            }
            str = v.getAsString(PdfName.REASON);
            if (str != null) {
				pk.setReason(str.toUnicodeString());
			}
            str = v.getAsString(PdfName.LOCATION);
            if (str != null) {
				pk.setLocation(str.toUnicodeString());
			}
            return pk;
        }
        catch (final Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    private void updateByteRange(final PdfPKCS7 pkcs7, final PdfDictionary v) {
        final PdfArray b = v.getAsArray(PdfName.BYTERANGE);
        final RandomAccessFileOrArray rf = this.reader.getSafeFile();
        try {
            rf.reOpen();
            final byte buf[] = new byte[8192];
            for (int k = 0; k < b.size(); ++k) {
                final int start = b.getAsNumber(k).intValue();
                int length = b.getAsNumber(++k).intValue();
                rf.seek(start);
                while (length > 0) {
                    final int rd = rf.read(buf, 0, Math.min(length, buf.length));
                    if (rd <= 0) {
						break;
					}
                    length -= rd;
                    pkcs7.update(buf, 0, rd);
                }
            }
        }
        catch (final Exception e) {
            throw new ExceptionConverter(e);
        }
        finally {
            try{rf.close();}catch(final Exception e){}
        }
    }

    private void markUsed(final PdfObject obj) {
        if (!this.append) {
			return;
		}
        ((PdfStamperImp)this.writer).markUsed(obj);
    }

    /**
     * Gets the total number of revisions this document has.
     *
     * @return the total number of revisions
     */
    public int getTotalRevisions() {
        getSignatureNames();
        return this.totalRevisions;
    }

    /**
     * Gets this <CODE>field</CODE> revision.
     *
     * @param field the signature field name
     * @return the revision or zero if it's not a signature field
     */
    public int getRevision(String field) {
        getSignatureNames();
        field = getTranslatedFieldName(field);
        if (!this.sigNames.containsKey(field)) {
			return 0;
		}
        return ((int[])this.sigNames.get(field))[1];
    }

    /**
     * Extracts a revision from the document.
     *
     * @param field the signature field name
     * @return an <CODE>InputStream</CODE> covering the revision. Returns <CODE>null</CODE> if
     * it's not a signature field
     * @throws IOException on error
     */
    public InputStream extractRevision(String field) throws IOException {
        getSignatureNames();
        field = getTranslatedFieldName(field);
        if (!this.sigNames.containsKey(field)) {
			return null;
		}
        final int length = ((int[])this.sigNames.get(field))[0];
        final RandomAccessFileOrArray raf = this.reader.getSafeFile();
        raf.reOpen();
        raf.seek(0);
        return new RevisionStream(raf, length);
    }

    /**
     * Gets the appearances cache.
     *
     * @return the appearances cache
     * @since	2.1.5	this method used to return a HashMap
     */
    public Map getFieldCache() {
        return this.fieldCache;
    }

    /**
     * Sets a cache for field appearances. Parsing the existing PDF to
     * create a new TextField is time expensive. For those tasks that repeatedly
     * fill the same PDF with different field values the use of the cache has dramatic
     * speed advantages. An example usage:
     * <pre>
     * String pdfFile = ...;// the pdf file used as template
     * ArrayList xfdfFiles = ...;// the xfdf file names
     * ArrayList pdfOutFiles = ...;// the output file names, one for each element in xpdfFiles
     * HashMap cache = new HashMap();// the appearances cache
     * PdfReader originalReader = new PdfReader(pdfFile);
     * for (int k = 0; k &lt; xfdfFiles.size(); ++k) {
     *    PdfReader reader = new PdfReader(originalReader);
     *    XfdfReader xfdf = new XfdfReader((String)xfdfFiles.get(k));
     *    PdfStamper stp = new PdfStamper(reader, new FileOutputStream((String)pdfOutFiles.get(k)));
     *    AcroFields af = stp.getAcroFields();
     *    af.setFieldCache(cache);
     *    af.setFields(xfdf);
     *    stp.close();
     * }
     * </pre>
     *
     * @param fieldCache a Map that will carry the cached appearances
     * @since	2.1.5	this method used to take a HashMap as parameter
     */
    public void setFieldCache(final Map fieldCache) {
        this.fieldCache = fieldCache;
    }

    /**
     * Sets extra margins in text fields to better mimic the Acrobat layout.
     *
     * @param extraMarginLeft the extra margin left
     * @param extraMarginTop the extra margin top
     */
    public void setExtraMargin(final float extraMarginLeft, final float extraMarginTop) {
        this.extraMarginLeft = extraMarginLeft;
        this.extraMarginTop = extraMarginTop;
    }

    /**
     * Adds a substitution font to the list. The fonts in this list will be used if the original
     * font doesn't contain the needed glyphs.
     *
     * @param font the font
     */
    public void addSubstitutionFont(final BaseFont font) {
        if (this.substitutionFonts == null) {
			this.substitutionFonts = new ArrayList();
		}
        this.substitutionFonts.add(font);
    }

    private static final HashMap stdFieldFontNames = new HashMap();

    /**
     * Holds value of property totalRevisions.
     */
    private int totalRevisions;

    /**
     * Holds value of property fieldCache.
     *
     * @since	2.1.5	this used to be a HashMap
     */
    private Map fieldCache;

    static {
        stdFieldFontNames.put("CoBO", new String[]{"Courier-BoldOblique"});
        stdFieldFontNames.put("CoBo", new String[]{"Courier-Bold"});
        stdFieldFontNames.put("CoOb", new String[]{"Courier-Oblique"});
        stdFieldFontNames.put("Cour", new String[]{"Courier"});
        stdFieldFontNames.put("HeBO", new String[]{"Helvetica-BoldOblique"});
        stdFieldFontNames.put("HeBo", new String[]{"Helvetica-Bold"});
        stdFieldFontNames.put("HeOb", new String[]{"Helvetica-Oblique"});
        stdFieldFontNames.put("Helv", new String[]{"Helvetica"});
        stdFieldFontNames.put("Symb", new String[]{"Symbol"});
        stdFieldFontNames.put("TiBI", new String[]{"Times-BoldItalic"});
        stdFieldFontNames.put("TiBo", new String[]{"Times-Bold"});
        stdFieldFontNames.put("TiIt", new String[]{"Times-Italic"});
        stdFieldFontNames.put("TiRo", new String[]{"Times-Roman"});
        stdFieldFontNames.put("ZaDb", new String[]{"ZapfDingbats"});
        stdFieldFontNames.put("HySm", new String[]{"HYSMyeongJo-Medium", "UniKS-UCS2-H"});
        stdFieldFontNames.put("HyGo", new String[]{"HYGoThic-Medium", "UniKS-UCS2-H"});
        stdFieldFontNames.put("KaGo", new String[]{"HeiseiKakuGo-W5", "UniKS-UCS2-H"});
        stdFieldFontNames.put("KaMi", new String[]{"HeiseiMin-W3", "UniJIS-UCS2-H"});
        stdFieldFontNames.put("MHei", new String[]{"MHei-Medium", "UniCNS-UCS2-H"});
        stdFieldFontNames.put("MSun", new String[]{"MSung-Light", "UniCNS-UCS2-H"});
        stdFieldFontNames.put("STSo", new String[]{"STSong-Light", "UniGB-UCS2-H"});
    }

    private static class RevisionStream extends InputStream {
        private final byte b[] = new byte[1];
        private final RandomAccessFileOrArray raf;
        private final int length;
        private int rangePosition = 0;
        private boolean closed;

        private RevisionStream(final RandomAccessFileOrArray raf, final int length) {
            this.raf = raf;
            this.length = length;
        }

        @Override
		public int read() throws IOException {
            final int n = read(this.b);
            if (n != 1) {
				return -1;
			}
            return this.b[0] & 0xff;
        }

        @Override
		public int read(final byte[] b, final int off, final int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || off > b.length || len < 0 ||
            off + len > b.length || off + len < 0) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }
            if (this.rangePosition >= this.length) {
                close();
                return -1;
            }
            final int elen = Math.min(len, this.length - this.rangePosition);
            this.raf.readFully(b, off, elen);
            this.rangePosition += elen;
            return elen;
        }

        @Override
		public void close() throws IOException {
            if (!this.closed) {
                this.raf.close();
                this.closed = true;
            }
        }
    }

    private static class SorterComparator implements Comparator {
        @Override
		public int compare(final Object o1, final Object o2) {
            final int n1 = ((int[])((Object[])o1)[1])[0];
            final int n2 = ((int[])((Object[])o2)[1])[0];
            return n1 - n2;
        }
    }

    /**
     * Gets the list of substitution fonts. The list is composed of <CODE>BaseFont</CODE> and can be <CODE>null</CODE>. The fonts in this list will be used if the original
     * font doesn't contain the needed glyphs.
     *
     * @return the list
     */
    public ArrayList getSubstitutionFonts() {
        return this.substitutionFonts;
    }

    /**
     * Sets a list of substitution fonts. The list is composed of <CODE>BaseFont</CODE> and can also be <CODE>null</CODE>. The fonts in this list will be used if the original
     * font doesn't contain the needed glyphs.
     *
     * @param substitutionFonts the list
     */
    public void setSubstitutionFonts(final ArrayList substitutionFonts) {
        this.substitutionFonts = substitutionFonts;
    }

    /**
     * Gets the XFA form processor.
     *
     * @return the XFA form processor
     */
    public XfaForm getXfa() {
        return this.xfa;
    }

    private static final PdfName[] buttonRemove = {PdfName.MK, PdfName.F , PdfName.FF , PdfName.Q , PdfName.BS , PdfName.BORDER};

    /**
     * Creates a new pushbutton from an existing field. If there are several pushbuttons with the same name
     * only the first one is used. This pushbutton can be changed and be used to replace
     * an existing one, with the same name or other name, as long is it is in the same document. To replace an existing pushbutton
     * call {@link #replacePushbuttonField(String,PdfFormField)}.
     *
     * @param field the field name that should be a pushbutton
     * @return a new pushbutton or <CODE>null</CODE> if the field is not a pushbutton
     */
    public PushbuttonField getNewPushbuttonFromField(final String field) {
        return getNewPushbuttonFromField(field, 0);
    }

    /**
     * Creates a new pushbutton from an existing field. This pushbutton can be changed and be used to replace
     * an existing one, with the same name or other name, as long is it is in the same document. To replace an existing pushbutton
     * call {@link #replacePushbuttonField(String,PdfFormField,int)}.
     *
     * @param field the field name that should be a pushbutton
     * @param order the field order in fields with same name
     * @return a new pushbutton or <CODE>null</CODE> if the field is not a pushbutton
     *
     * @since 2.0.7
     */
    public PushbuttonField getNewPushbuttonFromField(final String field, final int order) {
        try {
            if (getFieldType(field) != FIELD_TYPE_PUSHBUTTON) {
				return null;
			}
            final Item item = getFieldItem(field);
            if (order >= item.size()) {
				return null;
			}
            final int posi = order * 5;
            final float[] pos = getFieldPositions(field);
            final Rectangle box = new Rectangle(pos[posi + 1], pos[posi + 2], pos[posi + 3], pos[posi + 4]);
            final PushbuttonField newButton = new PushbuttonField(this.writer, box, null);
            final PdfDictionary dic = item.getMerged(order);
            decodeGenericDictionary(dic, newButton);
            final PdfDictionary mk = dic.getAsDict(PdfName.MK);
            if (mk != null) {
                final PdfString text = mk.getAsString(PdfName.CA);
                if (text != null) {
					newButton.setText(text.toUnicodeString());
				}
                final PdfNumber tp = mk.getAsNumber(PdfName.TP);
                if (tp != null) {
					newButton.setLayout(tp.intValue() + 1);
				}
                final PdfDictionary ifit = mk.getAsDict(PdfName.IF);
                if (ifit != null) {
                    PdfName sw = ifit.getAsName(PdfName.SW);
                    if (sw != null) {
                        int scale = PushbuttonField.SCALE_ICON_ALWAYS;
                        if (sw.equals(PdfName.B)) {
							scale = PushbuttonField.SCALE_ICON_IS_TOO_BIG;
						} else if (sw.equals(PdfName.S)) {
							scale = PushbuttonField.SCALE_ICON_IS_TOO_SMALL;
						} else if (sw.equals(PdfName.N)) {
							scale = PushbuttonField.SCALE_ICON_NEVER;
						}
                        newButton.setScaleIcon(scale);
                    }
                    sw = ifit.getAsName(PdfName.S);
                    if (sw != null) {
                        if (sw.equals(PdfName.A)) {
							newButton.setProportionalIcon(false);
						}
                    }
                    final PdfArray aj = ifit.getAsArray(PdfName.A);
                    if (aj != null && aj.size() == 2) {
                        final float left = aj.getAsNumber(0).floatValue();
                        final float bottom = aj.getAsNumber(1).floatValue();
                        newButton.setIconHorizontalAdjustment(left);
                        newButton.setIconVerticalAdjustment(bottom);
                    }
                    final PdfBoolean fb = ifit.getAsBoolean(PdfName.FB);
                    if (fb != null && fb.booleanValue()) {
						newButton.setIconFitToBounds(true);
					}
                }
                final PdfObject i = mk.get(PdfName.I);
                if (i != null && i.isIndirect()) {
					newButton.setIconReference((PRIndirectReference)i);
				}
            }
            return newButton;
        }
        catch (final Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    /**
     * Replaces the first field with a new pushbutton. The pushbutton can be created with
     * {@link #getNewPushbuttonFromField(String)} from the same document or it can be a
     * generic PdfFormField of the type pushbutton.
     *
     * @param field the field name
     * @param button the <CODE>PdfFormField</CODE> representing the pushbutton
     * @return <CODE>true</CODE> if the field was replaced, <CODE>false</CODE> if the field
     * was not a pushbutton
     */
    public boolean replacePushbuttonField(final String field, final PdfFormField button) {
        return replacePushbuttonField(field, button, 0);
    }

    /**
     * Replaces the designated field with a new pushbutton. The pushbutton can be created with
     * {@link #getNewPushbuttonFromField(String,int)} from the same document or it can be a
     * generic PdfFormField of the type pushbutton.
     *
     * @param field the field name
     * @param button the <CODE>PdfFormField</CODE> representing the pushbutton
     * @param order the field order in fields with same name
     * @return <CODE>true</CODE> if the field was replaced, <CODE>false</CODE> if the field
     * was not a pushbutton
     *
     * @since 2.0.7
     */
    public boolean replacePushbuttonField(final String field, final PdfFormField button, final int order) {
        if (getFieldType(field) != FIELD_TYPE_PUSHBUTTON) {
			return false;
		}
        final Item item = getFieldItem(field);
        if (order >= item.size()) {
			return false;
		}
        final PdfDictionary merged = item.getMerged(order);
        final PdfDictionary values = item.getValue(order);
        final PdfDictionary widgets = item.getWidget(order);
        for (final PdfName element : buttonRemove) {
            merged.remove(element);
            values.remove(element);
            widgets.remove(element);
        }
        for (final Object element : button.getKeys()) {
            final PdfName key = (PdfName)element;
            if (key.equals(PdfName.T) || key.equals(PdfName.RECT)) {
				continue;
			}
            if (key.equals(PdfName.FF)) {
				values.put(key, button.get(key));
			} else {
				widgets.put(key, button.get(key));
			}
            merged.put(key, button.get(key));
        }
        return true;
    }

}
