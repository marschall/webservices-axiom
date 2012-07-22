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
package org.apache.axiom.om.impl.jaxp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestSuite;

import org.apache.axiom.om.AbstractTestCase;
import org.apache.axiom.om.OMMetaFactory;
import org.apache.axiom.testutils.conformance.ConformanceTestFile;
import org.xml.sax.InputSource;

public class StreamSourceToOMResultTestCase extends AbstractTestCase {
    private final OMMetaFactory omMetaFactory;
    private final TransformerFactory transformerFactory;
    private final ConformanceTestFile file;
    
    private StreamSourceToOMResultTestCase(OMMetaFactory omMetaFactory,
            TransformerFactory transformerFactory, String name, ConformanceTestFile file) {
        super(name);
        this.omMetaFactory = omMetaFactory;
        this.transformerFactory = transformerFactory;
        this.file = file;
    }
    
    protected void runTest() throws Throwable {
        StreamSource source = new StreamSource(file.getAsStream());
        OMResult result = new OMResult(omMetaFactory.getOMFactory());
        transformerFactory.newTransformer().transform(source, result);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        result.getDocument().serialize(out);
        assertXMLIdentical(compareXML(
                new InputSource(file.getAsStream()),
                new InputSource(new ByteArrayInputStream(out.toByteArray()))), true);
    }

    public static TestSuite suite(OMMetaFactory omMetaFactory,
            TransformerFactory transformerFactory) throws Exception {
        TestSuite suite = new TestSuite();
        ConformanceTestFile[] files = ConformanceTestFile.getConformanceTestFiles();
        for (int i=0; i<files.length; i++) {
            ConformanceTestFile file = files[i];
            suite.addTest(new StreamSourceToOMResultTestCase(omMetaFactory, transformerFactory,
                    file.getShortName(), file));
        }
        return suite;
    }
}
