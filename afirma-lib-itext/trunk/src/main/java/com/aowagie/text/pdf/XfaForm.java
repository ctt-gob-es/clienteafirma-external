/*
 * $Id: XfaForm.java 3961 2009-06-09 10:06:10Z blowagie $
 *
 * Copyright 2006 Paulo Soares
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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.aowagie.text.xml.XmlDomWriter;

/**
 * Processes XFA forms.
 * @author Paulo Soares (psoares@consiste.pt)
 */
public class XfaForm {

    private Xml2SomTemplate templateSom;
    private Node templateNode;
    private Xml2SomDatasets datasetsSom;
    private Node datasetsNode;
    private AcroFieldsSearch acroFieldsSom;
    private PdfReader reader;
    private boolean xfaPresent;
    private org.w3c.dom.Document domDocument;
    private boolean changed;
    public static final String XFA_DATA_SCHEMA = "http://www.xfa.org/schema/xfa-data/1.0/";

    /**
     * An empty constructor to build on.
     */
    public XfaForm() {
    }

    /**
     * Return the XFA Object, could be an array, could be a Stream.
     * Returns null f no XFA Object is present.
     * @param	reader	a PdfReader instance
     * @return	the XFA object
     * @since	2.1.3
     */
    public static PdfObject getXfaObject(final PdfReader reader) {
    	final PdfDictionary af = (PdfDictionary)PdfReader.getPdfObjectRelease(reader.getCatalog().get(PdfName.ACROFORM));
        if (af == null) {
            return null;
        }
        return PdfReader.getPdfObjectRelease(af.get(PdfName.XFA));
    }

    /**
     * A constructor from a <CODE>PdfReader</CODE>. It basically does everything
     * from finding the XFA stream to the XML parsing.
     * @param reader the reader
     * @throws java.io.IOException on error
     * @throws javax.xml.parsers.ParserConfigurationException on error
     * @throws org.xml.sax.SAXException on error
     */
    public XfaForm(final PdfReader reader) throws IOException, ParserConfigurationException, SAXException {
        this.reader = reader;
        final PdfObject xfa = getXfaObject(reader);
        if (xfa == null) {
        	this.xfaPresent = false;
        	return;
        }
        this.xfaPresent = true;
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        if (xfa.isArray()) {
            final PdfArray ar = (PdfArray)xfa;
            for (int k = 1; k < ar.size(); k += 2) {
                final PdfObject ob = ar.getDirectObject(k);
                if (ob instanceof PRStream) {
                    final byte[] b = PdfReader.getStreamBytes((PRStream)ob);
                    bout.write(b);
                }
            }
        }
        else if (xfa instanceof PRStream) {
            final byte[] b = PdfReader.getStreamBytes((PRStream)xfa);
            bout.write(b);
        }
        bout.close();
        final DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
        fact.setNamespaceAware(true);
        final DocumentBuilder db = fact.newDocumentBuilder();
        db.setEntityResolver(new EntityResolver() {
			@Override
			public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
				return new InputSource(new StringReader(""));
			}        	
        });
        this.domDocument = db.parse(new ByteArrayInputStream(bout.toByteArray()));
        extractNodes();
    }

    /**
     * Extracts the nodes from the domDocument.
     * @since	2.1.5
     */
    private void extractNodes() {
        Node n = this.domDocument.getFirstChild();
        while (n.getChildNodes().getLength() == 0) {
        	n = n.getNextSibling();
        }
        n = n.getFirstChild();
        while (n != null) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                final String s = n.getLocalName();
                if (s.equals("template")) {
                	this.templateNode = n;
                    this.templateSom = new Xml2SomTemplate(n);
                }
                else if (s.equals("datasets")) {
                    this.datasetsNode = n;
                    this.datasetsSom = new Xml2SomDatasets(n.getFirstChild());
                }
            }
            n = n.getNextSibling();
        }
    }

    /**
     * Sets the XFA key from a byte array. The old XFA is erased.
     * @param form the data
     * @param reader the reader
     * @param writer the writer
     * @throws java.io.IOException on error
     */
    public static void setXfa(final XfaForm form, final PdfReader reader, final PdfWriter writer) throws IOException {
        final PdfDictionary af = (PdfDictionary)PdfReader.getPdfObjectRelease(reader.getCatalog().get(PdfName.ACROFORM));
        if (af == null) {
            return;
        }
        final PdfObject xfa = getXfaObject(reader);
        if (xfa.isArray()) {
            final PdfArray ar = (PdfArray)xfa;
            int t = -1;
            int d = -1;
            for (int k = 0; k < ar.size(); k += 2) {
                final PdfString s = ar.getAsString(k);
                if ("template".equals(s.toString())) {
                	t = k + 1;
                }
                if ("datasets".equals(s.toString())) {
                	d = k + 1;
                }
            }
            if (t > -1 && d > -1) {
                reader.killXref(ar.getAsIndirectObject(t));
                reader.killXref(ar.getAsIndirectObject(d));
                final PdfStream tStream = new PdfStream(serializeDoc(form.templateNode));
                tStream.flateCompress(writer.getCompressionLevel());
                ar.set(t, writer.addToBody(tStream).getIndirectReference());
                final PdfStream dStream = new PdfStream(serializeDoc(form.datasetsNode));
                dStream.flateCompress(writer.getCompressionLevel());
                ar.set(d, writer.addToBody(dStream).getIndirectReference());
                af.put(PdfName.XFA, new PdfArray(ar));
            	return;
            }
        }
        reader.killXref(af.get(PdfName.XFA));
        final PdfStream str = new PdfStream(serializeDoc(form.domDocument));
        str.flateCompress(writer.getCompressionLevel());
        final PdfIndirectReference ref = writer.addToBody(str).getIndirectReference();
        af.put(PdfName.XFA, ref);
    }

    /**
     * Sets the XFA key from the instance data. The old XFA is erased.
     * @param writer the writer
     * @throws java.io.IOException on error
     */
    public void setXfa(final PdfWriter writer) throws IOException {
        setXfa(this, this.reader, writer);
    }

    /**
     * Serializes a XML document to a byte array.
     * @param n the XML document
     * @throws java.io.IOException on error
     * @return the serialized XML document
     */
    public static byte[] serializeDoc(final Node n) throws IOException {
        final XmlDomWriter xw = new XmlDomWriter();
        final ByteArrayOutputStream fout = new ByteArrayOutputStream();
        xw.setOutput(fout, null);
        xw.setCanonical(false);
        xw.write(n);
        fout.close();
        return fout.toByteArray();
    }

    /**
     * Returns <CODE>true</CODE> if it is a XFA form.
     * @return <CODE>true</CODE> if it is a XFA form
     */
    public boolean isXfaPresent() {
        return this.xfaPresent;
    }

    /**
     * Gets the top level DOM document.
     * @return the top level DOM document
     */
    public org.w3c.dom.Document getDomDocument() {
        return this.domDocument;
    }


    /**
     * Finds the complete field name contained in the "classic" forms from a partial
     * name.
     * @param name the complete or partial name
     * @param af the fields
     * @return the complete name or <CODE>null</CODE> if not found
     */
    public String findFieldName(final String name, final AcroFields af) {
        final HashMap items = af.getFields();
        if (items.containsKey(name)) {
			return name;
		}
        if (this.acroFieldsSom == null) {
        	if (items.isEmpty() && this.xfaPresent) {
				this.acroFieldsSom = new AcroFieldsSearch(this.datasetsSom.getName2Node().keySet());
			} else {
				this.acroFieldsSom = new AcroFieldsSearch(items.keySet());
			}
        }
        if (this.acroFieldsSom.getAcroShort2LongName().containsKey(name)) {
			return (String)this.acroFieldsSom.getAcroShort2LongName().get(name);
		}
        return this.acroFieldsSom.inverseSearchGlobal(Xml2Som.splitParts(name));
    }

    /**
     * Finds the complete SOM name contained in the datasets section from a
     * possibly partial name.
     * @param name the complete or partial name
     * @return the complete name or <CODE>null</CODE> if not found
     */
    public String findDatasetsName(final String name) {
        if (this.datasetsSom.getName2Node().containsKey(name)) {
			return name;
		}
        return this.datasetsSom.inverseSearchGlobal(Xml2Som.splitParts(name));
    }

    /**
     * Finds the <CODE>Node</CODE> contained in the datasets section from a
     * possibly partial name.
     * @param name the complete or partial name
     * @return the <CODE>Node</CODE> or <CODE>null</CODE> if not found
     */
    public Node findDatasetsNode(String name) {
        if (name == null) {
			return null;
		}
        name = findDatasetsName(name);
        if (name == null) {
			return null;
		}
        return (Node)this.datasetsSom.getName2Node().get(name);
    }

    /**
     * Gets all the text contained in the child nodes of this node.
     * @param n the <CODE>Node</CODE>
     * @return the text found or "" if no text was found
     */
    public static String getNodeText(final Node n) {
        if (n == null) {
			return "";
		}
        return getNodeText(n, "");

    }

    private static String getNodeText(final Node n, String name) {
        Node n2 = n.getFirstChild();
        while (n2 != null) {
            if (n2.getNodeType() == Node.ELEMENT_NODE) {
                name = getNodeText(n2, name);
            }
            else if (n2.getNodeType() == Node.TEXT_NODE) {
                name += n2.getNodeValue();
            }
            n2 = n2.getNextSibling();
        }
        return name;
    }

    /**
     * Sets the text of this node. All the child's node are deleted and a new
     * child text node is created.
     * @param n the <CODE>Node</CODE> to add the text to
     * @param text the text to add
     */
    public void setNodeText(final Node n, final String text) {
        if (n == null) {
			return;
		}
        Node nc = null;
        while ((nc = n.getFirstChild()) != null) {
            n.removeChild(nc);
        }
        if (n.getAttributes().getNamedItemNS(XFA_DATA_SCHEMA, "dataNode") != null) {
			n.getAttributes().removeNamedItemNS(XFA_DATA_SCHEMA, "dataNode");
		}
        n.appendChild(this.domDocument.createTextNode(text));
        this.changed = true;
    }

    /**
     * Sets the XFA form flag signaling that this is a valid XFA form.
     * @param xfaPresent the XFA form flag signaling that this is a valid XFA form
     */
    public void setXfaPresent(final boolean xfaPresent) {
        this.xfaPresent = xfaPresent;
    }

    /**
     * Sets the top DOM document.
     * @param domDocument the top DOM document
     */
    public void setDomDocument(final org.w3c.dom.Document domDocument) {
        this.domDocument = domDocument;
        extractNodes();
    }

    /**
     * Gets the <CODE>PdfReader</CODE> used by this instance.
     * @return the <CODE>PdfReader</CODE> used by this instance
     */
    public PdfReader getReader() {
        return this.reader;
    }

    /**
     * Sets the <CODE>PdfReader</CODE> to be used by this instance.
     * @param reader the <CODE>PdfReader</CODE> to be used by this instance
     */
    public void setReader(final PdfReader reader) {
        this.reader = reader;
    }

    /**
     * Checks if this XFA form was changed.
     * @return <CODE>true</CODE> if this XFA form was changed
     */
    public boolean isChanged() {
        return this.changed;
    }

    /**
     * Sets the changed status of this XFA instance.
     * @param changed the changed status of this XFA instance
     */
    public void setChanged(final boolean changed) {
        this.changed = changed;
    }

    /**
     * A structure to store each part of a SOM name and link it to the next part
     * beginning from the lower hierarchy.
     */
    public static class InverseStore {
        protected ArrayList part = new ArrayList();
        protected ArrayList follow = new ArrayList();

        /**
         * Gets the full name by traversing the hierarchy using only the
         * index 0.
         * @return the full name
         */
        public String getDefaultName() {
            InverseStore store = this;
            while (true) {
                final Object obj = store.follow.get(0);
                if (obj instanceof String) {
					return (String)obj;
				}
                store = (InverseStore)obj;
            }
        }

        /**
         * Search the current node for a similar name. A similar name starts
         * with the same name but has a different index. For example, "detail[3]"
         * is similar to "detail[9]". The main use is to discard names that
         * correspond to out of bounds records.
         * @param name the name to search
         * @return <CODE>true</CODE> if a similitude was found
         */
        public boolean isSimilar(String name) {
            final int idx = name.indexOf('[');
            name = name.substring(0, idx + 1);
            for (int k = 0; k < this.part.size(); ++k) {
                if (((String)this.part.get(k)).startsWith(name)) {
					return true;
				}
            }
            return false;
        }
    }

    /**
     * Another stack implementation. The main use is to facilitate
     * the porting to other languages.
     */
    public static class Stack2 extends ArrayList {
        private static final long serialVersionUID = -7451476576174095212L;

		/**
         * Looks at the object at the top of this stack without removing it from the stack.
         * @return the object at the top of this stack
         */
        public Object peek() {
            if (size() == 0) {
				throw new EmptyStackException();
			}
            return get(size() - 1);
        }

        /**
         * Removes the object at the top of this stack and returns that object as the value of this function.
         * @return the object at the top of this stack
         */
        public Object pop() {
            if (size() == 0) {
				throw new EmptyStackException();
			}
            final Object ret = get(size() - 1);
            remove(size() - 1);
            return ret;
        }

        /**
         * Pushes an item onto the top of this stack.
         * @param item the item to be pushed onto this stack
         * @return the <CODE>item</CODE> argument
         */
        public Object push(final Object item) {
            add(item);
            return item;
        }

        /**
         * Tests if this stack is empty.
         * @return <CODE>true</CODE> if and only if this stack contains no items; <CODE>false</CODE> otherwise
         */
        public boolean empty() {
            return size() == 0;
        }
    }

    /**
     * A class for some basic SOM processing.
     */
    public static class Xml2Som {
        /**
         * The order the names appear in the XML, depth first.
         */
        protected ArrayList order;
        /**
         * The mapping of full names to nodes.
         */
        protected HashMap name2Node;
        /**
         * The data to do a search from the bottom hierarchy.
         */
        protected HashMap inverseSearch;
        /**
         * A stack to be used when parsing.
         */
        protected Stack2 stack;
        /**
         * A temporary store for the repetition count.
         */
        protected int anform;

        /**
         * Escapes a SOM string fragment replacing "." with "\.".
         * @param s the unescaped string
         * @return the escaped string
         */
        public static String escapeSom(final String s) {
            int idx = s.indexOf('.');
            if (idx < 0) {
				return s;
			}
            final StringBuffer sb = new StringBuffer();
            int last = 0;
            while (idx >= 0) {
                sb.append(s.substring(last, idx));
                sb.append('\\');
                last = idx;
                idx = s.indexOf('.', idx + 1);
            }
            sb.append(s.substring(last));
            return sb.toString();
        }

        /**
         * Unescapes a SOM string fragment replacing "\." with ".".
         * @param s the escaped string
         * @return the unescaped string
         */
        public static String unescapeSom(final String s) {
            int idx = s.indexOf('\\');
            if (idx < 0) {
				return s;
			}
            final StringBuffer sb = new StringBuffer();
            int last = 0;
            while (idx >= 0) {
                sb.append(s.substring(last, idx));
                last = idx + 1;
                idx = s.indexOf('\\', idx + 1);
            }
            sb.append(s.substring(last));
            return sb.toString();
        }

        /**
         * Outputs the stack as the sequence of elements separated
         * by '.'.
         * @return the stack as the sequence of elements separated by '.'
         */
        protected String printStack() {
            if (this.stack.empty()) {
				return "";
			}
            final StringBuffer s = new StringBuffer();
            for (int k = 0; k < this.stack.size(); ++k) {
				s.append('.').append((String)this.stack.get(k));
			}
            return s.substring(1);
        }

        /**
         * Gets the name with the <CODE>#subform</CODE> removed.
         * @param s the long name
         * @return the short name
         */
        public static String getShortName(final String s) {
            int idx = s.indexOf(".#subform[");
            if (idx < 0) {
				return s;
			}
            int last = 0;
            final StringBuffer sb = new StringBuffer();
            while (idx >= 0) {
                sb.append(s.substring(last, idx));
                idx = s.indexOf("]", idx + 10);
                if (idx < 0) {
					return sb.toString();
				}
                last = idx + 1;
                idx = s.indexOf(".#subform[", last);
            }
            sb.append(s.substring(last));
            return sb.toString();
        }

        /**
         * Adds a SOM name to the search node chain.
         * @param unstack the SOM name
         */
        public void inverseSearchAdd(final String unstack) {
            inverseSearchAdd(this.inverseSearch, this.stack, unstack);
        }

        /**
         * Adds a SOM name to the search node chain.
         * @param inverseSearch the start point
         * @param stack the stack with the separated SOM parts
         * @param unstack the full name
         */
        public static void inverseSearchAdd(final HashMap inverseSearch, final Stack2 stack, final String unstack) {
            String last = (String)stack.peek();
            InverseStore store = (InverseStore)inverseSearch.get(last);
            if (store == null) {
                store = new InverseStore();
                inverseSearch.put(last, store);
            }
            for (int k = stack.size() - 2; k >= 0; --k) {
                last = (String)stack.get(k);
                InverseStore store2;
                final int idx = store.part.indexOf(last);
                if (idx < 0) {
                    store.part.add(last);
                    store2 = new InverseStore();
                    store.follow.add(store2);
                } else {
					store2 = (InverseStore)store.follow.get(idx);
				}
                store = store2;
            }
            store.part.add("");
            store.follow.add(unstack);
        }

        /**
         * Searches the SOM hierarchy from the bottom.
         * @param parts the SOM parts
         * @return the full name or <CODE>null</CODE> if not found
         */
        public String inverseSearchGlobal(final ArrayList parts) {
            if (parts.isEmpty()) {
				return null;
			}
            InverseStore store = (InverseStore)this.inverseSearch.get(parts.get(parts.size() - 1));
            if (store == null) {
				return null;
			}
            for (int k = parts.size() - 2; k >= 0; --k) {
                final String part = (String)parts.get(k);
                final int idx = store.part.indexOf(part);
                if (idx < 0) {
                    if (store.isSimilar(part)) {
						return null;
					}
                    return store.getDefaultName();
                }
                store = (InverseStore)store.follow.get(idx);
            }
            return store.getDefaultName();
        }

        /**
         * Splits a SOM name in the individual parts.
         * @param name the full SOM name
         * @return the split name
         */
        public static Stack2 splitParts(String name) {
            while (name.startsWith(".")) {
				name = name.substring(1);
			}
            final Stack2 parts = new Stack2();
            int last = 0;
            int pos = 0;
            String part;
            while (true) {
                pos = last;
                while (true) {
                    pos = name.indexOf('.', pos);
                    if (pos < 0) {
						break;
					}
                    if (name.charAt(pos - 1) == '\\') {
						++pos;
					} else {
						break;
					}
                }
                if (pos < 0) {
					break;
				}
                part = name.substring(last, pos);
                if (!part.endsWith("]")) {
					part += "[0]";
				}
                parts.add(part);
                last = pos + 1;
            }
            part = name.substring(last);
            if (!part.endsWith("]")) {
				part += "[0]";
			}
            parts.add(part);
            return parts;
        }

        /**
         * Gets the order the names appear in the XML, depth first.
         * @return the order the names appear in the XML, depth first
         */
        public ArrayList getOrder() {
            return this.order;
        }

        /**
         * Sets the order the names appear in the XML, depth first
         * @param order the order the names appear in the XML, depth first
         */
        public void setOrder(final ArrayList order) {
            this.order = order;
        }

        /**
         * Gets the mapping of full names to nodes.
         * @return the mapping of full names to nodes
         */
        public HashMap getName2Node() {
            return this.name2Node;
        }

        /**
         * Sets the mapping of full names to nodes.
         * @param name2Node the mapping of full names to nodes
         */
        public void setName2Node(final HashMap name2Node) {
            this.name2Node = name2Node;
        }

        /**
         * Gets the data to do a search from the bottom hierarchy.
         * @return the data to do a search from the bottom hierarchy
         */
        public HashMap getInverseSearch() {
            return this.inverseSearch;
        }

        /**
         * Sets the data to do a search from the bottom hierarchy.
         * @param inverseSearch the data to do a search from the bottom hierarchy
         */
        public void setInverseSearch(final HashMap inverseSearch) {
            this.inverseSearch = inverseSearch;
        }
    }

    /**
     * Processes the datasets section in the XFA form.
     */
    public static class Xml2SomDatasets extends Xml2Som {
        /**
         * Creates a new instance from the datasets node. This expects
         * not the datasets but the data node that comes below.
         * @param n the datasets node
         */
        public Xml2SomDatasets(final Node n) {
            this.order = new ArrayList();
            this.name2Node = new HashMap();
            this.stack = new Stack2();
            this.anform = 0;
            this.inverseSearch = new HashMap();
            processDatasetsInternal(n);
        }

        /**
         * Inserts a new <CODE>Node</CODE> that will match the short name.
         * @param n the datasets top <CODE>Node</CODE>
         * @param shortName the short name
         * @return the new <CODE>Node</CODE> of the inserted name
         */
        public Node insertNode(Node n, final String shortName) {
            final Stack2 stack = splitParts(shortName);
            final org.w3c.dom.Document doc = n.getOwnerDocument();
            Node n2 = null;
            n = n.getFirstChild();
            for (int k = 0; k < stack.size(); ++k) {
                final String part = (String)stack.get(k);
                int idx = part.lastIndexOf('[');
                final String name = part.substring(0, idx);
                idx = Integer.parseInt(part.substring(idx + 1, part.length() - 1));
                int found = -1;
                for (n2 = n.getFirstChild(); n2 != null; n2 = n2.getNextSibling()) {
                    if (n2.getNodeType() == Node.ELEMENT_NODE) {
                        final String s = escapeSom(n2.getLocalName());
                        if (s.equals(name)) {
                            ++found;
                            if (found == idx) {
								break;
							}
                        }
                    }
                }
                for (; found < idx; ++found) {
                    n2 = doc.createElementNS(null, name);
                    n2 = n.appendChild(n2);
                    final Node attr = doc.createAttributeNS(XFA_DATA_SCHEMA, "dataNode");
                    attr.setNodeValue("dataGroup");
                    n2.getAttributes().setNamedItemNS(attr);
                }
                n = n2;
            }
            inverseSearchAdd(this.inverseSearch, stack, shortName);
            this.name2Node.put(shortName, n2);
            this.order.add(shortName);
            return n2;
        }

        private static boolean hasChildren(final Node n) {
            final Node dataNodeN = n.getAttributes().getNamedItemNS(XFA_DATA_SCHEMA, "dataNode");
            if (dataNodeN != null) {
                final String dataNode = dataNodeN.getNodeValue();
                if ("dataGroup".equals(dataNode)) {
					return true;
				} else if ("dataValue".equals(dataNode)) {
					return false;
				}
            }
            if (!n.hasChildNodes()) {
				return false;
			}
            Node n2 = n.getFirstChild();
            while (n2 != null) {
                if (n2.getNodeType() == Node.ELEMENT_NODE) {
                    return true;
                }
                n2 = n2.getNextSibling();
            }
            return false;
        }

        private void processDatasetsInternal(final Node n) {
            final HashMap ss = new HashMap();
            Node n2 = n.getFirstChild();
            while (n2 != null) {
                if (n2.getNodeType() == Node.ELEMENT_NODE) {
                    final String s = escapeSom(n2.getLocalName());
                    Integer i = (Integer)ss.get(s);
                    if (i == null) {
						i = new Integer(0);
					} else {
						i = new Integer(i.intValue() + 1);
					}
                    ss.put(s, i);
                    if (hasChildren(n2)) {
                        this.stack.push(s + "[" + i.toString() + "]");
                        processDatasetsInternal(n2);
                        this.stack.pop();
                    }
                    else {
                        this.stack.push(s + "[" + i.toString() + "]");
                        final String unstack = printStack();
                        this.order.add(unstack);
                        inverseSearchAdd(unstack);
                        this.name2Node.put(unstack, n2);
                        this.stack.pop();
                    }
                }
                n2 = n2.getNextSibling();
            }
        }
    }

    /**
     * A class to process "classic" fields.
     */
    public static class AcroFieldsSearch extends Xml2Som {
        private HashMap acroShort2LongName;

        /**
         * Creates a new instance from a Collection with the full names.
         * @param items the Collection
         */
        public AcroFieldsSearch(final Collection items) {
            this.inverseSearch = new HashMap();
            this.acroShort2LongName = new HashMap();
            for (final Iterator it = items.iterator(); it.hasNext();) {
                final String itemName = (String)it.next();
                final String itemShort = getShortName(itemName);
                this.acroShort2LongName.put(itemShort, itemName);
                inverseSearchAdd(this.inverseSearch, splitParts(itemShort), itemName);
            }
        }

        /**
         * Gets the mapping from short names to long names. A long
         * name may contain the #subform name part.
         * @return the mapping from short names to long names
         */
        public HashMap getAcroShort2LongName() {
            return this.acroShort2LongName;
        }

        /**
         * Sets the mapping from short names to long names. A long
         * name may contain the #subform name part.
         * @param acroShort2LongName the mapping from short names to long names
         */
        public void setAcroShort2LongName(final HashMap acroShort2LongName) {
            this.acroShort2LongName = acroShort2LongName;
        }
    }

    /**
     * Processes the template section in the XFA form.
     */
    public static class Xml2SomTemplate extends Xml2Som {
        private boolean dynamicForm;
        private int templateLevel;

        /**
         * Creates a new instance from the datasets node.
         * @param n the template node
         */
        public Xml2SomTemplate(final Node n) {
            this.order = new ArrayList();
            this.name2Node = new HashMap();
            this.stack = new Stack2();
            this.anform = 0;
            this.templateLevel = 0;
            this.inverseSearch = new HashMap();
            processTemplate(n, null);
        }

        /**
         * Gets the field type as described in the <CODE>template</CODE> section of the XFA.
         * @param s the exact template name
         * @return the field type or <CODE>null</CODE> if not found
         */
        public String getFieldType(final String s) {
            final Node n = (Node)this.name2Node.get(s);
            if (n == null) {
				return null;
			}
            if (n.getLocalName().equals("exclGroup")) {
				return "exclGroup";
			}
            Node ui = n.getFirstChild();
            while (ui != null) {
                if (ui.getNodeType() == Node.ELEMENT_NODE && ui.getLocalName().equals("ui")) {
                    break;
                }
                ui = ui.getNextSibling();
            }
            if (ui == null) {
				return null;
			}
            Node type = ui.getFirstChild();
            while (type != null) {
                if (type.getNodeType() == Node.ELEMENT_NODE && !(type.getLocalName().equals("extras") && type.getLocalName().equals("picture"))) {
                    return type.getLocalName();
                }
                type = type.getNextSibling();
            }
            return null;
        }

        private void processTemplate(final Node n, HashMap ff) {
            if (ff == null) {
				ff = new HashMap();
			}
            final HashMap ss = new HashMap();
            Node n2 = n.getFirstChild();
            while (n2 != null) {
                if (n2.getNodeType() == Node.ELEMENT_NODE) {
                    final String s = n2.getLocalName();
                    if (s.equals("subform")) {
                        final Node name = n2.getAttributes().getNamedItem("name");
                        String nn = "#subform";
                        boolean annon = true;
                        if (name != null) {
                            nn = escapeSom(name.getNodeValue());
                            annon = false;
                        }
                        Integer i;
                        if (annon) {
                            i = new Integer(this.anform);
                            ++this.anform;
                        }
                        else {
                            i = (Integer)ss.get(nn);
                            if (i == null) {
								i = new Integer(0);
							} else {
								i = new Integer(i.intValue() + 1);
							}
                            ss.put(nn, i);
                        }
                        this.stack.push(nn + "[" + i.toString() + "]");
                        ++this.templateLevel;
                        if (annon) {
							processTemplate(n2, ff);
						} else {
							processTemplate(n2, null);
						}
                        --this.templateLevel;
                        this.stack.pop();
                    }
                    else if (s.equals("field") || s.equals("exclGroup")) {
                        final Node name = n2.getAttributes().getNamedItem("name");
                        if (name != null) {
                            final String nn = escapeSom(name.getNodeValue());
                            Integer i = (Integer)ff.get(nn);
                            if (i == null) {
								i = new Integer(0);
							} else {
								i = new Integer(i.intValue() + 1);
							}
                            ff.put(nn, i);
                            this.stack.push(nn + "[" + i.toString() + "]");
                            final String unstack = printStack();
                            this.order.add(unstack);
                            inverseSearchAdd(unstack);
                            this.name2Node.put(unstack, n2);
                            this.stack.pop();
                        }
                    }
                    else if (!this.dynamicForm && this.templateLevel > 0 && s.equals("occur")) {
                        int initial = 1;
                        int min = 1;
                        int max = 1;
                        Node a = n2.getAttributes().getNamedItem("initial");
                        if (a != null) {
							try{initial = Integer.parseInt(a.getNodeValue().trim());}catch(final Exception e){}
						}
                        a = n2.getAttributes().getNamedItem("min");
                        if (a != null) {
							try{min = Integer.parseInt(a.getNodeValue().trim());}catch(final Exception e){}
						}
                        a = n2.getAttributes().getNamedItem("max");
                        if (a != null) {
							try{max = Integer.parseInt(a.getNodeValue().trim());}catch(final Exception e){}
						}
                        if (initial != min || min != max) {
							this.dynamicForm = true;
						}
                    }
                }
                n2 = n2.getNextSibling();
            }
        }

        /**
         * <CODE>true</CODE> if it's a dynamic form; <CODE>false</CODE>
         * if it's a static form.
         * @return <CODE>true</CODE> if it's a dynamic form; <CODE>false</CODE>
         * if it's a static form
         */
        public boolean isDynamicForm() {
            return this.dynamicForm;
        }

        /**
         * Sets the dynamic form flag. It doesn't change the template.
         * @param dynamicForm the dynamic form flag
         */
        public void setDynamicForm(final boolean dynamicForm) {
            this.dynamicForm = dynamicForm;
        }
    }

    /**
     * Gets the class that contains the template processing section of the XFA.
     * @return the class that contains the template processing section of the XFA
     */
    public Xml2SomTemplate getTemplateSom() {
        return this.templateSom;
    }

    /**
     * Sets the class that contains the template processing section of the XFA
     * @param templateSom the class that contains the template processing section of the XFA
     */
    public void setTemplateSom(final Xml2SomTemplate templateSom) {
        this.templateSom = templateSom;
    }

    /**
     * Gets the class that contains the datasets processing section of the XFA.
     * @return the class that contains the datasets processing section of the XFA
     */
    public Xml2SomDatasets getDatasetsSom() {
        return this.datasetsSom;
    }

    /**
     * Sets the class that contains the datasets processing section of the XFA.
     * @param datasetsSom the class that contains the datasets processing section of the XFA
     */
    public void setDatasetsSom(final Xml2SomDatasets datasetsSom) {
        this.datasetsSom = datasetsSom;
    }

    /**
     * Gets the class that contains the "classic" fields processing.
     * @return the class that contains the "classic" fields processing
     */
    public AcroFieldsSearch getAcroFieldsSom() {
        return this.acroFieldsSom;
    }

    /**
     * Sets the class that contains the "classic" fields processing.
     * @param acroFieldsSom the class that contains the "classic" fields processing
     */
    public void setAcroFieldsSom(final AcroFieldsSearch acroFieldsSom) {
        this.acroFieldsSom = acroFieldsSom;
    }

    /**
     * Gets the <CODE>Node</CODE> that corresponds to the datasets part.
     * @return the <CODE>Node</CODE> that corresponds to the datasets part
     */
    public Node getDatasetsNode() {
        return this.datasetsNode;
    }
}
