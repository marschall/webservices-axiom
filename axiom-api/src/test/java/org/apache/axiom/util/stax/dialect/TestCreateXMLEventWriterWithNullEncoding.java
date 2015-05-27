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

import javax.xml.stream.XMLOutputFactory;

public class TestCreateXMLEventWriterWithNullEncoding extends DialectTestCase {
    public TestCreateXMLEventWriterWithNullEncoding(StAXImplementation staxImpl) {
        super(staxImpl);
    }

    protected void runTest() throws Throwable {
        XMLOutputFactory factory = staxImpl.newNormalizedXMLOutputFactory();
        // This should cause an exception
        try {
            factory.createXMLEventWriter(System.out, null);
        } catch (Throwable ex) {
            // Expected
            return;
        }
        // Attention here: since the fail method works by throwing an exception and we
        // catch Throwable, it must be invoked outside of the catch block!
        fail("Expected createXMLEventWriter to throw an exception");
    }
}
