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

package org.apache.axiom.soap.impl.llom.soap12;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPFaultText;
import org.apache.axiom.soap.SOAPProcessingException;
import org.apache.axiom.soap.impl.intf.AxiomSOAP12FaultReason;
import org.apache.axiom.soap.impl.llom.SOAPFaultReasonImpl;

public class SOAP12FaultReasonImpl extends SOAPFaultReasonImpl implements AxiomSOAP12FaultReason {
    public void addSOAPText(SOAPFaultText soapFaultText)
            throws SOAPProcessingException {
        if (!(soapFaultText instanceof SOAP12FaultTextImpl)) {
            throw new SOAPProcessingException(
                    "Expecting SOAP12FaultTextImpl, got " + soapFaultText.getClass());
        }
        addChild(soapFaultText);
    }

    public SOAPFaultText getFirstSOAPText() {
        return (SOAPFaultText)getFirstChildWithName(SOAP12Constants.QNAME_FAULT_TEXT);
    }


    /**
     * getText() is overridden here in order to provide a uniform way for SOAP 1.1
     * and SOAP 1.2 to get the "default" reason string.
     *
     * @return the default (local language, or first) reason string
     */
    public String getText() {
        // TODO: Make this look for the correct lang for our locale first
        return getFirstSOAPText().getText();
    }

    public void checkParent(OMElement parent) throws SOAPProcessingException {
        if (!(parent instanceof SOAP12FaultImpl)) {
            throw new SOAPProcessingException(
                    "Expecting SOAP12FaultImpl, got " + parent.getClass());
        }
    }
}
