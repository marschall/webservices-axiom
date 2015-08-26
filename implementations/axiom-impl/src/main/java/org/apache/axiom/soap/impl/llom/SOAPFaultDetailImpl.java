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
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.impl.common.AxiomContainer;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.impl.common.AxiomSOAPFaultDetail;

public abstract class SOAPFaultDetailImpl extends SOAPElement implements AxiomSOAPFaultDetail {
    public SOAPFaultDetailImpl(OMFactory factory) {
        super(factory);
    }

    protected OMElement createClone(OMCloneOptions options, AxiomContainer targetParent) {
        return ((SOAPFactory)getOMFactory()).createSOAPFaultDetail((SOAPFault)targetParent);
    }
}
