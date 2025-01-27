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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.util.ElementHelper;
import org.apache.axiom.soap.SOAPFaultText;
import org.apache.axiom.soap.SOAPProcessingException;
import org.apache.axiom.soap.impl.intf.AxiomSOAPFaultReason;

public abstract class SOAPFaultReasonImpl extends SOAPElement implements
        AxiomSOAPFaultReason {
    protected SOAPFaultText text;

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
