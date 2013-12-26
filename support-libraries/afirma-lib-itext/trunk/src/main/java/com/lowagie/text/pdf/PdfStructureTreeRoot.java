/*
 * $Id: PdfStructureTreeRoot.java 3914 2009-04-26 09:16:47Z blowagie $
 *
 * Copyright 2005 by Paulo Soares.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * The structure tree root corresponds to the highest hierarchy level in a tagged PDF.
 * @author Paulo Soares (psoares@consiste.pt)
 */
class PdfStructureTreeRoot extends PdfDictionary {

    private final HashMap parentTree = new LinkedHashMap();
    private final PdfIndirectReference reference;

    /**
     * Holds value of property writer.
     */
    private final PdfWriter writer;

    /** Creates a new instance of PdfStructureTreeRoot */
    PdfStructureTreeRoot(final PdfWriter writer) {
        super(PdfName.STRUCTTREEROOT);
        this.writer = writer;
        this.reference = writer.getPdfIndirectReference();
    }



    /**
     * Gets the writer.
     * @return the writer
     */
    public PdfWriter getWriter() {
        return this.writer;
    }

    /**
     * Gets the reference this object will be written to.
     * @return the reference this object will be written to
     * @since	2.1.6 method removed in 2.1.5, but restored in 2.1.6
     */
    public PdfIndirectReference getReference() {
        return this.reference;
    }



    private void nodeProcess(final PdfDictionary struc, final PdfIndirectReference reference) throws IOException {
        final PdfObject obj = struc.get(PdfName.K);
        if (obj != null && obj.isArray() && !((PdfObject)((PdfArray)obj).getArrayList().get(0)).isNumber()) {
            final PdfArray ar = (PdfArray)obj;
            final ArrayList a = ar.getArrayList();
            for (int k = 0; k < a.size(); ++k) {
                final PdfStructureElement e = (PdfStructureElement)a.get(k);
                a.set(k, e.getReference());
                nodeProcess(e, e.getReference());
            }
        }
        if (reference != null) {
			this.writer.addToBody(struc, reference);
		}
    }

    void buildTree() throws IOException {
        final HashMap numTree = new HashMap();
        for (final Iterator it = this.parentTree.keySet().iterator(); it.hasNext();) {
            final Integer i = (Integer)it.next();
            final PdfArray ar = (PdfArray)this.parentTree.get(i);
            numTree.put(i, this.writer.addToBody(ar).getIndirectReference());
        }
        final PdfDictionary dicTree = PdfNumberTree.writeTree(numTree, this.writer);
        if (dicTree != null) {
			put(PdfName.PARENTTREE, this.writer.addToBody(dicTree).getIndirectReference());
		}

        nodeProcess(this, this.reference);
    }
}
