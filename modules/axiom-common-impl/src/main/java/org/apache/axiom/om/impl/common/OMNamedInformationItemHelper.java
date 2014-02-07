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
import org.apache.axiom.om.impl.util.OMSerializerUtil;

public final class OMNamedInformationItemHelper {
    private OMNamedInformationItemHelper() {}

    public static OMNamespace handleNamespace(IElement context, OMNamespace ns, boolean attr, boolean declare) {
        String namespaceURI = ns == null ? "" : ns.getNamespaceURI();
        String prefix = ns == null ? "" : ns.getPrefix();
        if (namespaceURI.length() == 0) {
            if (prefix != null && prefix.length() != 0) {
                throw new IllegalArgumentException("Cannot bind a prefix to the empty namespace name");
            }
            if (!attr && declare) {
                // Special case: no namespace; we need to generate a namespace declaration only if
                // there is a conflicting namespace declaration (i.e. a declaration for the default
                // namespace with a non empty URI) is in scope
                if (context.getDefaultNamespace() != null) {
                    context.declareDefaultNamespace("");
                }
            }
            return null;
        } else {
            if (attr && prefix != null && prefix.length() == 0) {
                throw new IllegalArgumentException("An attribute with a namespace must be prefixed");
            }
            boolean addNSDecl = false;
            if (context != null && (declare || prefix == null)) {
                OMNamespace existingNSDecl = context.findNamespace(namespaceURI, prefix);
                if (existingNSDecl == null
                        || (prefix != null && !existingNSDecl.getPrefix().equals(prefix))
                        || (prefix == null && attr && existingNSDecl.getPrefix().length() == 0)) {
                    addNSDecl = declare;
                } else {
                    prefix = existingNSDecl.getPrefix();
                    ns = existingNSDecl;
                }
            }
            if (prefix == null) {
                prefix = OMSerializerUtil.getNextNSPrefix();
                ns = new OMNamespaceImpl(namespaceURI, prefix);
            }
            if (addNSDecl) {
                context.addNamespaceDeclaration(ns);
            }
            return ns;
        }
    }
}
