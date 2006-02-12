/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ws.commons.soap.impl.llom;

import org.apache.ws.commons.om.OMElement;
import org.apache.ws.commons.om.OMNamespace;
import org.apache.ws.commons.om.OMXMLParserWrapper;
import org.apache.ws.commons.om.util.ElementHelper;
import org.apache.ws.commons.soap.SOAP12Constants;
import org.apache.ws.commons.soap.SOAPFaultSubCode;
import org.apache.ws.commons.soap.SOAPFaultValue;
import org.apache.ws.commons.soap.SOAPProcessingException;

public abstract class SOAPFaultSubCodeImpl extends SOAPElement implements SOAPFaultSubCode {

    protected SOAPFaultValue value;
    protected SOAPFaultSubCode subCode;

    protected SOAPFaultSubCodeImpl(OMNamespace ns) {
        super(SOAP12Constants.SOAP_FAULT_VALUE_LOCAL_NAME, ns);
    }


    protected SOAPFaultSubCodeImpl(OMElement parent, String localName) throws SOAPProcessingException {
        super(parent, localName, true);
    }

    protected SOAPFaultSubCodeImpl(OMElement parent,
                                   String localName,
                                   OMXMLParserWrapper builder) {
        super(parent, localName, builder);
    }

    public void setValue(SOAPFaultValue soapFaultSubCodeValue) throws SOAPProcessingException {
        ElementHelper.setNewElement(this, value, soapFaultSubCodeValue);
    }

    public SOAPFaultValue getValue() {
        if (value == null) {
            value =
                    (SOAPFaultValue) ElementHelper.getChildWithName(this,
                            SOAP12Constants.SOAP_FAULT_VALUE_LOCAL_NAME);
        }
        return value;
    }

    public void setSubCode(SOAPFaultSubCode subCode) throws SOAPProcessingException {
        ElementHelper.setNewElement(this, this.subCode, subCode);

    }

    public SOAPFaultSubCode getSubCode() {
        if (subCode == null) {
            subCode =
                    (SOAPFaultSubCode) ElementHelper.getChildWithName(this,
                            SOAP12Constants.SOAP_FAULT_SUB_CODE_LOCAL_NAME);
        }
        return subCode;
    }
}
