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

import org.apache.axiom.om.OMConstants;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.impl.dom.ParentNode;
import org.apache.axiom.soap.SOAPConstants;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPProcessingException;
import org.apache.axiom.soap.impl.common.AxiomSOAPBody;

public abstract class SOAPBodyImpl extends SOAPElement implements AxiomSOAPBody,
        OMConstants {
    public SOAPBodyImpl(OMFactory factory) {
        super(factory);
    }

    public SOAPBodyImpl(ParentNode parentNode, OMNamespace ns,
            OMXMLParserWrapper builder, OMFactory factory, boolean generateNSDecl) {
        super(parentNode, SOAPConstants.BODY_LOCAL_NAME, ns, builder, factory, generateNSDecl);
    }

    /**
     * Indicates whether a <code>SOAPFault</code> object exists in this <code>SOAPBody</code> object.
     *
     * @return <code>true</code> if a <code>SOAPFault</code> object exists in this
     *         <code>SOAPBody</code> object; <code>false</code> otherwise
     */
    public boolean hasFault() {
        return getFirstElement() instanceof SOAPFault;
    }

    /**
     * Returns the <code>SOAPFault</code> object in this <code>SOAPBody</code> object.
     *
     * @return the <code>SOAPFault</code> object in this <code>SOAPBody</code> object
     */
    public SOAPFault getFault() {
        OMElement element = getFirstElement();
        return element instanceof SOAPFault ? (SOAPFault)element : null;
    }

    /**
     * @param soapFault
     * @throws org.apache.axiom.om.OMException
     *
     * @throws OMException
     */
    public void addFault(SOAPFault soapFault) throws OMException {
        if (hasFault()) {
            throw new OMException(
                    "SOAP Body already has a SOAP Fault and there can not be " +
                            "more than one SOAP fault");
        }
        addChild(soapFault);
    }

    public void checkParent(OMElement parent) throws SOAPProcessingException {
        if (!(parent instanceof SOAPEnvelopeImpl)) {
            throw new SOAPProcessingException(
                    "Expecting an implementation of SOAP Envelope as the " +
                            "parent. But received some other implementation");
        }
    }

    /*public OMNode detach() throws OMException {
         throw new SOAPProcessingException(
                 "Can not detach SOAP Body, SOAP Envelope must have a Body !!");
     }*/
    
    public OMNamespace getFirstElementNS() {
        OMElement element = this.getFirstElement();
        if (element == null) {
            return null;
        } else {
            return element.getNamespace();
        } 

    }

    public String getFirstElementLocalName() {
        OMElement element = this.getFirstElement();
        if (element == null) {
            return null;
        } else {
            return element.getLocalName();
        } 

    }
}
