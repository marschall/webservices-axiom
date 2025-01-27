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

import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.intf.AxiomNamespaceDeclaration;

public aspect AxiomNamespaceDeclarationSupport {
    private OMNamespace AxiomNamespaceDeclaration.declaredNamespace;
    
    public final String AxiomNamespaceDeclaration.coreGetDeclaredPrefix() {
        return declaredNamespace.getPrefix();
    }

    public final OMNamespace AxiomNamespaceDeclaration.getDeclaredNamespace() {
        String namespaceURI = coreGetCharacterData().toString();
        if (!namespaceURI.equals(declaredNamespace.getNamespaceURI())) {
            declaredNamespace = new OMNamespaceImpl(namespaceURI, declaredNamespace.getPrefix());
        }
        return declaredNamespace;
    }
    
    public final void AxiomNamespaceDeclaration.coreSetDeclaredNamespace(String prefix, String namespaceURI) {
        setDeclaredNamespace(new OMNamespaceImpl(namespaceURI, prefix));
    }
    
    public final void AxiomNamespaceDeclaration.setDeclaredNamespace(OMNamespace declaredNamespace) {
        this.declaredNamespace = declaredNamespace;
        coreSetCharacterData(declaredNamespace.getNamespaceURI(), Policies.DETACH_POLICY);
    }
}
