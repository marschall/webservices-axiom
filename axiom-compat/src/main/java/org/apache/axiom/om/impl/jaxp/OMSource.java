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

import javax.xml.transform.sax.SAXSource;

import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.serialize.OMXMLReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.InputSource;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;

/**
 * Implementation of {@link javax.xml.transform.Source} for AXIOM.
 * The implementation is based on {@link SAXSource} and directly transforms an AXIOM
 * tree into a stream of SAX events using {@link OMXMLReader}.
 * <p>
 * Note that this class only supports {@link ContentHandler} and {@link LexicalHandler}.
 * {@link DTDHandler} and {@link DeclHandler} are not supported.
 * 
 * @deprecated As of version 1.2.13, application code should use
 * {@link OMContainer#getSAXSource(boolean)} instead of this class.
 */
public class OMSource extends SAXSource {
    public OMSource(OMElement element) {
        this((OMContainer)element);
    }
    
    public OMSource(OMContainer node) {
        super(new OMXMLReader(node), new InputSource());
    }
}
