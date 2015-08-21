/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.axiom.soap.impl.llom;

import org.apache.axiom.core.CoreParentNode;
import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPProcessingException;
import org.apache.axiom.soap.impl.common.AxiomSOAPElement;

public abstract class SOAPElement extends OMElementImpl implements AxiomSOAPElement {
    public SOAPElement(OMFactory factory) {
        super(factory);
    }

    /**
     * @param parent
     * @param localName
     * @param extractNamespaceFromParent
     */
    protected SOAPElement(OMElement parent,
                          String localName,
                          boolean extractNamespaceFromParent,
                          SOAPFactory factory) throws SOAPProcessingException {
        super(parent, localName, null, null, factory, true);
        if (parent == null) {
            throw new SOAPProcessingException(
                    " Can not create " + localName +
                            " element without a parent !!");
        }
        checkParent(parent);

        if (extractNamespaceFromParent) {
            internalSetNamespace(parent.getNamespace());
        }
    }


    protected SOAPElement(OMContainer parent,
                          String localName,
                          OMXMLParserWrapper builder,
                          SOAPFactory factory) {
        super(parent, localName, null, builder, factory, false);
    }

    /**
     * @param localName
     * @param ns
     */
    protected SOAPElement(String localName, OMNamespace ns,
                          SOAPFactory factory) {
        super(null, localName, ns, null, factory, true);
    }

    public void internalSetParent(CoreParentNode element) {
        super.internalSetParent(element);

        if (element instanceof OMElement) {
            checkParent((OMElement) element);
        }
    }
}
