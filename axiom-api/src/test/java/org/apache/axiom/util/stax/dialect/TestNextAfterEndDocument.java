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
package org.apache.axiom.util.stax.dialect;

import java.io.StringReader;
import java.util.NoSuchElementException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Checks the type of exception thrown by {@link XMLStreamReader#next()} if the end of the document
 * has already been reached.
 */
public class TestNextAfterEndDocument extends DialectTestCase {
    public TestNextAfterEndDocument(StAXImplementation staxImpl) {
        super(staxImpl);
    }

    protected void runTest() throws Throwable {
        XMLInputFactory factory = staxImpl.newNormalizedXMLInputFactory();
        XMLStreamReader reader = factory.createXMLStreamReader(new StringReader("<root/>"));
        while (reader.next() != XMLStreamReader.END_DOCUMENT) {
            // Just loop
        }
        try {
            reader.next();
            fail("Expected exception");
        } catch (IllegalStateException ex) {
            // Expected
        } catch (NoSuchElementException ex) {
            // This is also OK
        } catch (XMLStreamException ex) {
            fail("Expected IllegalStateException or NoSuchElementException");
        }
    }
}
