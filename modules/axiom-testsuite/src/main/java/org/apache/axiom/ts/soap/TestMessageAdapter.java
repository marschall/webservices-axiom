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
package org.apache.axiom.ts.soap;

import org.apache.axiom.om.OMMetaFactory;
import org.apache.axiom.om.util.StAXParserConfiguration;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPModelBuilder;
import org.junit.Assert;
import org.xml.sax.InputSource;

/**
 * {@link TestMessage} adapter that adds some Axiom specific methods to retrieve the content of the
 * test message.
 */
public final class TestMessageAdapter {
    private final TestMessage testMessage;

    TestMessageAdapter(TestMessage testMessage) {
        this.testMessage = testMessage;
    }
    
    public SOAPModelBuilder getBuilder(OMMetaFactory metaFactory) {
        return metaFactory.createSOAPModelBuilder(StAXParserConfiguration.SOAP, new InputSource(testMessage.getInputStream()));
    }
    
    public SOAPEnvelope getSOAPEnvelope(OMMetaFactory metaFactory) {
        SOAPEnvelope envelope = getBuilder(metaFactory).getSOAPEnvelope();
        // TODO: this is not the right place to assert this
        Assert.assertSame(testMessage.getSOAPSpec().getEnvelopeNamespaceURI(), ((SOAPFactory)envelope.getOMFactory()).getSoapVersionURI());
        return envelope;
    }
}