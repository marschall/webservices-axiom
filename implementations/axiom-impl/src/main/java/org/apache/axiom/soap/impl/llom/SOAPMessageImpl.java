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

import org.apache.axiom.om.OMCloneOptions;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.impl.llom.OMDocumentImpl;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPProcessingException;
import org.apache.axiom.soap.impl.common.AxiomSOAPMessage;

public class SOAPMessageImpl extends OMDocumentImpl implements AxiomSOAPMessage {
    public SOAPMessageImpl(SOAPFactory factory) {
        super(factory);
    }

    public SOAPEnvelope getSOAPEnvelope() throws SOAPProcessingException {
        return (SOAPEnvelope) getOMDocumentElement();
    }

    public void setSOAPEnvelope(SOAPEnvelope envelope) {
        setOMDocumentElement(envelope);
    }

    protected void checkDocumentElement(OMElement element) {
        if (!(element instanceof SOAPEnvelope)) {
            throw new OMException("Child not allowed; must be a SOAPEnvelope");
        }
    }

    protected OMDocument createClone(OMCloneOptions options) {
        // Note: we need to use getOMFactory here (instead of the factory attribute)
        // directly because the factory for a SOAPMessage may be determined lazily.
        return new SOAPMessageImpl((SOAPFactory)getOMFactory());
    }
}
