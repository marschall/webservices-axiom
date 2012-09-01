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
package org.apache.axiom.ts.dom.attr;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.axiom.ts.dom.DOMTestCase;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

public class TestLookpNamespaceURI extends DOMTestCase {

    public TestLookpNamespaceURI(DocumentBuilderFactory dbf) {
        super(dbf);
    }

    protected void runTest() throws Throwable {

        String namespace = "http://apache/axiom/dom/ns";
        String defaultNamespace = "http://www.w3.org/2000/xmlns/";
        String prefix = "ns";
        String defaultPrefix = "xmlns";

        // <ns:Element xmlns:ns="http://apache/axiom/dom/ns">
        Document doc = dbf.newDocumentBuilder().newDocument();
        Element ele = doc.createElement("Element");
        ele.setAttributeNS(defaultNamespace, defaultPrefix+":"+prefix, namespace);

        NamedNodeMap attributes = ele.getAttributes();
        Attr attr = (Attr) attributes.item(0);
        
        assertEquals("Incorrect namespace returned for the attribute", namespace,
                attr.lookupNamespaceURI("ns"));
    }

}