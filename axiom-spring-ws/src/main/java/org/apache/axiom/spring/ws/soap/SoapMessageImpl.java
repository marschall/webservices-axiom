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
package org.apache.axiom.spring.ws.soap;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.soap.SOAP11Version;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPMessage;
import org.apache.axiom.spring.ws.AxiomWebServiceMessage;
import org.apache.axiom.spring.ws.SourceExtractionStrategy;
import org.apache.axiom.spring.ws.SourceExtractionStrategyStack;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ws.mime.Attachment;
import org.springframework.ws.mime.AttachmentException;
import org.springframework.ws.soap.AbstractSoapMessage;
import org.springframework.ws.soap.SoapEnvelope;
import org.springframework.ws.soap.SoapEnvelopeException;
import org.springframework.ws.stream.StreamingPayload;
import org.springframework.ws.transport.TransportConstants;
import org.springframework.ws.transport.TransportOutputStream;
import org.w3c.dom.Document;

final class SoapMessageImpl extends AbstractSoapMessage implements AxiomWebServiceMessage {
    private static final Log log = LogFactory.getLog(SoapMessageImpl.class);
    
    private SOAPMessage axiomMessage;
    private final SourceExtractionStrategyStack extractionStrategyStack = new SourceExtractionStrategyStack();
    private SoapEnvelopeImpl envelope;
    private String soapAction;
    
    SoapMessageImpl(SOAPMessage axiomMessage, String soapAction, boolean autoCreateHeader) {
        this.axiomMessage = axiomMessage;
        this.soapAction = soapAction;
        if (autoCreateHeader) {
            envelope = new SoapEnvelopeImpl(this, axiomMessage.getSOAPEnvelope(), true);
        }
    }

    public SoapEnvelope getEnvelope() throws SoapEnvelopeException {
        SOAPEnvelope axiomEnvelope = axiomMessage.getSOAPEnvelope();
        if (envelope == null || envelope.axiomNode != axiomEnvelope) {
            envelope = new SoapEnvelopeImpl(this, axiomEnvelope, false);
        }
        return envelope;
    }

    @Override
    public String getSoapAction() {
        return SoapUtils.escapeAction(soapAction);
    }

    @Override
    public void setSoapAction(String soapAction) {
        this.soapAction = SoapUtils.unescapeAction(soapAction);
    }

    public Document getDocument() {
        if (axiomMessage instanceof Document) {
            return (Document)axiomMessage;
        } else {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }
    }

    public void setDocument(Document document) {
        if (document instanceof SOAPMessage) {
            axiomMessage = (SOAPMessage)document;
        } else {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }
    }

    public boolean isXopPackage() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public boolean convertToXopPackage() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Attachment getAttachment(String contentId) throws AttachmentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Iterator<Attachment> getAttachments() throws AttachmentException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Attachment addAttachment(String contentId, DataHandler dataHandler) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        OMOutputFormat outputFormat = new OMOutputFormat();
        boolean soap11 = ((SOAPFactory)axiomMessage.getOMFactory()).getSOAPVersion() == SOAP11Version.getSingleton();
        outputFormat.setSOAP11(soap11);
        if (outputStream instanceof TransportOutputStream) {
            TransportOutputStream transportOutputStream = (TransportOutputStream)outputStream;
            // TODO: ensure that charset is specified in content type
            // TODO: omit XML declaration
            String contentType = outputFormat.getContentType();
            if (soap11) {
                transportOutputStream.addHeader(TransportConstants.HEADER_SOAP_ACTION, "\"" + soapAction + "\"");
            } else {
                contentType += "; action=\"" + soapAction + "\"";
            }
            transportOutputStream.addHeader(TransportConstants.HEADER_CONTENT_TYPE, contentType);
        }
        try {
            axiomMessage.serializeAndConsume(outputStream);
        } catch (XMLStreamException ex) {
            throw new SoapMessageSerializationException("Message serialization failure", ex);
        }
    }

    public QName getPayloadRootQName() {
        log.debug("Getting the QName of the payload root element");
        SOAPEnvelope envelope = axiomMessage.getSOAPEnvelope();
        OMNamespace ns = envelope.getSOAPBodyFirstElementNS();
        String localName = envelope.getSOAPBodyFirstElementLocalName();
        QName qname = new QName(ns.getNamespaceURI(), localName, ns.getPrefix());
        if (log.isDebugEnabled()) {
            log.debug("Payload root QName is " + qname);
        }
        return qname;
    }

    public void setStreamingPayload(StreamingPayload payload) {
        SOAPBody body = axiomMessage.getSOAPEnvelope().getBody();
        body.removeChildren();
        body.addChild(axiomMessage.getOMFactory().createOMElement(new StreamingPayloadOMDataSource(payload)));
    }

    public void pushSourceExtractionStrategy(SourceExtractionStrategy strategy, Object bean) {
        extractionStrategyStack.push(strategy, bean);
    }

    public void popSourceExtractionStrategy(Object bean) {
        extractionStrategyStack.pop(bean);
    }
    
    SourceExtractionStrategy getSourceExtractionStrategy() {
        return extractionStrategyStack.getCurrent();
    }
}
