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

package org.apache.axiom.soap.impl.dom;

import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.impl.dom.ParentNode;
import org.apache.axiom.om.util.ElementHelper;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPFaultText;
import org.apache.axiom.soap.SOAPProcessingException;
import org.apache.axiom.soap.impl.common.AxiomSOAPFaultReason;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class SOAPFaultReasonImpl extends SOAPElement implements
        AxiomSOAPFaultReason {
    protected SOAPFaultText text;

    public SOAPFaultReasonImpl(OMFactory factory) {
        super(factory);
    }

    public SOAPFaultReasonImpl(ParentNode parentNode, OMNamespace ns,
            OMXMLParserWrapper builder, OMFactory factory, boolean generateNSDecl) {
        super(parentNode, ((SOAPFactory)factory).getSOAPVersion().getFaultReasonQName().getLocalPart(),
                ns, builder, factory, generateNSDecl);
    }

    public void addSOAPText(SOAPFaultText soapFaultText) throws SOAPProcessingException {
        ElementHelper.setNewElement(this, text, soapFaultText);
    }

    public List getAllSoapTexts() {
        //TODO Ruchith check
        List faultTexts = new ArrayList();
        Iterator childrenIter = this.getChildren();
        while (childrenIter.hasNext()) {
            OMNode node = (OMNode) childrenIter.next();
            if (node.getType() == OMNode.ELEMENT_NODE && (node instanceof SOAPFaultText)) {
                faultTexts.add(((SOAPFaultText) node));
            }
        }
        return faultTexts;
    }

    public SOAPFaultText getSOAPFaultText(String language) {
        //TODO Ruchith
        throw new UnsupportedOperationException();
    }
}
