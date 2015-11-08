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

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

class AaltoDialect extends AbstractStAXDialect {
    
    static final AaltoDialect INSTANCE = new AaltoDialect();

    public String getName() {
        return "Aalto";
    }

    public XMLInputFactory enableCDataReporting(XMLInputFactory factory) {
        factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        return factory;
    }

    public XMLInputFactory disallowDoctypeDecl(XMLInputFactory factory) {
        return StAXDialectUtils.disallowDoctypeDecl(factory);
    }

    public XMLInputFactory makeThreadSafe(XMLInputFactory factory) {
        return factory;
    }

    public XMLOutputFactory makeThreadSafe(XMLOutputFactory factory) {
        return factory;
    }

    public XMLInputFactory normalize(XMLInputFactory factory) {
        // Woodstox 3 used to report whitespace in prolog, but this is no longer the case by default
        // in Woodstox 4. The following property changes that behavior.
        factory.setProperty("org.codehaus.stax2.reportPrologWhitespace", Boolean.TRUE);
        factory = new NormalizingXMLInputFactoryWrapper(factory, this);
//        if (wstx276) {
//            factory = new CloseShieldXMLInputFactoryWrapper(factory);
//        }
        return factory;
    }

    public XMLOutputFactory normalize(XMLOutputFactory factory) {
        return new AaltoOutputFactoryWrapper(factory, this);
    }

    @Override
    public XMLStreamReader normalize(XMLStreamReader reader) {
        return new AaltoStreamReaderWrapper(reader);
    }

    @Override
    public XMLStreamWriter normalize(XMLStreamWriter writer) {
        return new AaltoStreamWriterWrapper(writer);
    }

}
