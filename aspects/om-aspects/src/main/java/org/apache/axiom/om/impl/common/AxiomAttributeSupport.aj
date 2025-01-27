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
package org.apache.axiom.om.impl.common;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.intf.AxiomAttribute;
import org.apache.axiom.om.impl.intf.AxiomElement;

public aspect AxiomAttributeSupport {
    public final OMElement AxiomAttribute.getOwner() {
        return (OMElement)coreGetOwnerElement();
    }

    final void AxiomAttribute.beforeSetLocalName() {
    }
    
    public final void AxiomAttribute.setNamespace(OMNamespace namespace, boolean decl) {
        internalSetNamespace(NSUtil.handleNamespace((AxiomElement)getOwner(), namespace, true, decl));
    }
    
    public final void AxiomAttribute.setOMNamespace(OMNamespace omNamespace) {
        internalSetNamespace(omNamespace);
    }
    
    public final String AxiomAttribute.getAttributeValue() {
        return coreGetCharacterData().toString();
    }
    
    public final void AxiomAttribute.setAttributeValue(String value) {
        coreSetCharacterData(value, Policies.DETACH_POLICY);
    }
    
    public final String AxiomAttribute.getAttributeType() {
        return coreGetType();
    }

    public final void AxiomAttribute.setAttributeType(String type) {
        coreSetType(type);
    }

    public final void AxiomAttribute.setSpecified(boolean specified) {
        coreSetSpecified(specified);
    }
}
