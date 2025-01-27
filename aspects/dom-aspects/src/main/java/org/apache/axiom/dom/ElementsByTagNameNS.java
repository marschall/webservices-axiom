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
package org.apache.axiom.dom;

import java.util.Iterator;

import org.apache.axiom.core.Axis;
import org.apache.axiom.core.ElementMatcher;
import org.w3c.dom.Node;

public class ElementsByTagNameNS extends NodeListImpl {
    private final DOMParentNode node;
    private final String namespaceURI;
    private final String localName;
    
    public ElementsByTagNameNS(DOMParentNode node, String namespaceURI, String localName) {
        this.node = node;
        this.namespaceURI = namespaceURI == null ? "" : namespaceURI;
        this.localName = localName;
    }

    @Override
    protected Iterator<? extends Node> createIterator() {
        boolean nsWildcard = "*".equals(namespaceURI);
        boolean localNameWildcard = localName.equals("*");
        if (nsWildcard && localNameWildcard) {
            // TODO: there seems to be no unit test checking whether the iterator should return DOM1 elements!
            return node.coreGetElements(Axis.DESCENDANTS, DOMElement.class, ElementMatcher.ANY, null, null, DOMExceptionTranslator.INSTANCE, Policies.DETACH_POLICY);
        } else if (nsWildcard) {
            return node.coreGetElements(Axis.DESCENDANTS, DOMNSAwareElement.class, ElementMatcher.BY_LOCAL_NAME, null, localName, DOMExceptionTranslator.INSTANCE, Policies.DETACH_POLICY);
        } else if (localNameWildcard) {
            return node.coreGetElements(Axis.DESCENDANTS, DOMNSAwareElement.class, ElementMatcher.BY_NAMESPACE_URI, namespaceURI, null, DOMExceptionTranslator.INSTANCE, Policies.DETACH_POLICY);
        } else {
            return node.coreGetElements(Axis.DESCENDANTS, DOMNSAwareElement.class, ElementMatcher.BY_QNAME, namespaceURI, localName, DOMExceptionTranslator.INSTANCE, Policies.DETACH_POLICY);
        }
    }
}
