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

package org.apache.axiom.soap.impl.llom.soap11;

import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.impl.common.AxiomSOAP11Body;
import org.apache.axiom.soap.impl.llom.SOAPBodyImpl;

public class SOAP11BodyImpl extends SOAPBodyImpl implements AxiomSOAP11Body {
    public SOAP11BodyImpl(OMFactory factory) {
        super(factory);
    }

    public SOAPFault addFault(Exception e) throws OMException {
        return ((SOAP11Factory)getOMFactory()).createSOAPFault(this, e);
    }
}
