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

import org.apache.axiom.om.OMCloneOptions;
import org.apache.axiom.om.OMConstants;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.impl.dom.NSAwareElement;
import org.apache.axiom.om.impl.dom.ParentNode;
import org.apache.axiom.soap.SOAPConstants;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPFaultDetail;
import org.apache.axiom.soap.SOAPProcessingException;
import org.apache.axiom.soap.impl.common.AxiomSOAPFault;

import javax.xml.namespace.QName;

import java.io.PrintWriter;
import java.io.StringWriter;

public abstract class SOAPFaultImpl extends SOAPElement implements AxiomSOAPFault,
        OMConstants {

    protected Exception e;

    public SOAPFaultImpl(OMFactory factory) {
        super(factory);
    }

    public void setException(Exception e) {
        this.e = e;
        putExceptionToSOAPFault(e);
    }

    protected abstract SOAPFaultDetail getNewSOAPFaultDetail(SOAPFault fault)
            throws SOAPProcessingException;

    // --------------- Getters and Settors --------------------------- //

    /** If exception detailElement is not there we will return null */
    public Exception getException() throws OMException {
        SOAPFaultDetail detail = getDetail();
        if (detail == null) {
            return null;
        }

        OMElement exceptionElement = getDetail().getFirstChildWithName(
                new QName(SOAPConstants.SOAP_FAULT_DETAIL_EXCEPTION_ENTRY));
        if (exceptionElement != null && exceptionElement.getText() != null) {
            return new Exception(exceptionElement.getText());
        }
        return null;
    }

    protected void putExceptionToSOAPFault(Exception e)
            throws SOAPProcessingException {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        sw.flush();
        SOAPFaultDetail detail = getDetail();
        if (detail == null) {
            detail = getNewSOAPFaultDetail(this);
            setDetail(detail);
        }
        OMElement faultDetailEnty = new NSAwareElement((ParentNode)detail,
                                                    SOAPConstants.SOAP_FAULT_DETAIL_EXCEPTION_ENTRY,
                                                    null, null, getOMFactory(), true);
        faultDetailEnty.setText(sw.getBuffer().toString());
    }

    @Override
    protected final void copyData(OMCloneOptions options, NSAwareElement clone) {
        if (e != null) {
            ((SOAPFault)clone).setException(e);
        }
    }
}
