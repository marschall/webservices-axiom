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
package org.apache.axiom.ts.om.element;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMMetaFactory;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLStreamReaderConfiguration;
import org.apache.axiom.ts.AxiomTestCase;

/**
 * Tests the behavior of
 * {@link OMElement#getXMLStreamReader(boolean, OMXMLStreamReaderConfiguration)} with
 * {@link OMXMLStreamReaderConfiguration#isPreserveNamespaceContext()} set to <code>true</code>.
 */
public class TestGetXMLStreamReaderWithPreserveNamespaceContext extends AxiomTestCase {
    public TestGetXMLStreamReaderWithPreserveNamespaceContext(OMMetaFactory metaFactory) {
        super(metaFactory);
    }

    protected void runTest() throws Throwable {
        InputStream in = TestGetXMLStreamReaderWithPreserveNamespaceContext.class.getResourceAsStream("AXIOM-114.xml");
        OMElement root = OMXMLBuilderFactory.createOMBuilder(metaFactory.getOMFactory(), in).getDocumentElement();
        OMXMLStreamReaderConfiguration configuration = new OMXMLStreamReaderConfiguration();
        configuration.setPreserveNamespaceContext(true);
        XMLStreamReader reader = root.getFirstElement().getFirstElement().getXMLStreamReader(true, configuration);
        assertEquals(XMLStreamReader.START_ELEMENT, reader.next());
        assertEquals(4, reader.getNamespaceCount());
        Set<String> prefixes = new HashSet<>();
        for (int i=0; i<4; i++) {
            prefixes.add(reader.getNamespacePrefix(i));
        }
        assertEquals(new HashSet<>(Arrays.asList(new String[] { "soapenv", "xsd", "xsi", "ns"} )), prefixes);
    }
}
