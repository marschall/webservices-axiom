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

package org.apache.axiom.om.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.activation.DataHandler;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axiom.attachments.impl.BufferUtils;
import org.apache.axiom.attachments.lifecycle.DataHandlerExt;
import org.apache.axiom.ext.stax.datahandler.DataHandlerProvider;
import org.apache.axiom.ext.stax.datahandler.DataHandlerWriter;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.util.CommonUtils;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.om.util.XMLStreamWriterFilter;
import org.apache.axiom.util.stax.XMLStreamWriterUtils;
import org.apache.axiom.util.stax.xop.ContentIDGenerator;
import org.apache.axiom.util.stax.xop.OptimizationPolicy;
import org.apache.axiom.util.stax.xop.XOPEncodingStreamWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * MTOMXMLStreamWriter is an XML + Attachments stream writer.
 * 
 * For the moment this assumes that transport takes the decision of whether to optimize or not by
 * looking at whether the MTOM optimize is enabled and also looking at the OM tree whether it has any
 * optimizable content.
 */
public class MTOMXMLStreamWriter implements XMLStreamWriter {
    /**
     * Stores a part that has been added without using the {@link DataHandlerWriter} API.
     */
    private static class Part {
        private final String contentID;
        private final DataHandler dataHandler;
        
        public Part(String contentID, DataHandler dataHandler) {
            this.contentID = contentID;
            this.dataHandler = dataHandler;
        }

        public String getContentID() {
            return contentID;
        }

        public DataHandler getDataHandler() {
            return dataHandler;
        }
    }
    
    private static final Log log = LogFactory.getLog(MTOMXMLStreamWriter.class);
    private XMLStreamWriter xmlWriter;
    private OutputStream outStream;
    private List/*<Part>*/ otherParts = new LinkedList();
    private OMMultipartWriter multipartWriter;
    private OutputStream rootPartOutputStream;
    private OMOutputFormat format;
    private final OptimizationPolicy optimizationPolicy;
    private final boolean preserveAttachments;
    
    // State variables
    private boolean isEndDocument = false; // has endElement been called
    private boolean isComplete = false;    // have the attachments been written
    private int depth = 0;                 // current element depth
    
    // Set the filter object if provided
    private XMLStreamWriterFilter xmlStreamWriterFilter  = null;

    public MTOMXMLStreamWriter(XMLStreamWriter xmlWriter) {
        this.xmlWriter = xmlWriter;
        if (log.isTraceEnabled()) {
            log.trace("Call Stack =" + CommonUtils.callStackToString());
        }
        format = new OMOutputFormat();
        optimizationPolicy = new OptimizationPolicyImpl(format);
        preserveAttachments = true;
    }

    public MTOMXMLStreamWriter(OutputStream outStream, OMOutputFormat format)
            throws XMLStreamException, FactoryConfigurationError {
        this(outStream, format, true);
    }
    
    /**
     * Creates a new MTOMXMLStreamWriter with specified encoding.
     * 
     * @param outStream
     * @param format
     * @param preserveAttachments
     *            specifies whether attachments must be preserved or can be consumed (i.e. streamed)
     *            during serialization; if set to <code>false</code> then
     *            {@link DataHandlerExt#readOnce()} or an equivalent method may be used to get the
     *            data for an attachment
     * @throws XMLStreamException
     * @throws FactoryConfigurationError
     * @see OMOutputFormat#DEFAULT_CHAR_SET_ENCODING
     */
    public MTOMXMLStreamWriter(OutputStream outStream, OMOutputFormat format, boolean preserveAttachments)
            throws XMLStreamException, FactoryConfigurationError {
        if (log.isDebugEnabled()) {
            log.debug("Creating MTOMXMLStreamWriter");
            log.debug("OutputStream =" + outStream.getClass());
            log.debug("OMFormat = " + format.toString());
            log.debug("preserveAttachments = " + preserveAttachments);
        }
        if (log.isTraceEnabled()) {
            log.trace("Call Stack =" + CommonUtils.callStackToString());
        }
        this.format = format;
        this.outStream = outStream;
        this.preserveAttachments = preserveAttachments;

        String encoding = format.getCharSetEncoding();
        if (encoding == null) { //Default encoding is UTF-8
            format.setCharSetEncoding(encoding = OMOutputFormat.DEFAULT_CHAR_SET_ENCODING);
        }

        optimizationPolicy = new OptimizationPolicyImpl(format);
        
        if (format.isOptimized()) {
            multipartWriter = new OMMultipartWriter(outStream, format);
            try {
                rootPartOutputStream = multipartWriter.writeRootPart();
            } catch (IOException ex) {
                throw new XMLStreamException(ex);
            }
            ContentIDGenerator contentIDGenerator = new ContentIDGenerator() {
                public String generateContentID(String existingContentID) {
                    return existingContentID != null ? existingContentID : getNextContentId();
                }
            };
            xmlWriter = new XOPEncodingStreamWriter(StAXUtils.createXMLStreamWriter(
                    format.getStAXWriterConfiguration(), rootPartOutputStream, encoding),
                    contentIDGenerator, optimizationPolicy);
        } else {
            xmlWriter = StAXUtils.createXMLStreamWriter(format.getStAXWriterConfiguration(),
                    outStream, format.getCharSetEncoding());
        }
        xmlStreamWriterFilter = format.getXmlStreamWriterFilter();
        if (xmlStreamWriterFilter != null) {
            if (log.isDebugEnabled()) {
                log.debug("Installing XMLStreamWriterFilter " + xmlStreamWriterFilter);
            }
            xmlStreamWriterFilter.setDelegate(xmlWriter);
            xmlWriter = xmlStreamWriterFilter;
        }
    }

    public void writeStartElement(String string) throws XMLStreamException {
        xmlWriter.writeStartElement(string);
        depth++;
    }

    public void writeStartElement(String string, String string1) throws XMLStreamException {
        xmlWriter.writeStartElement(string, string1);
        depth++;
    }

    public void writeStartElement(String string, String string1, String string2)
            throws XMLStreamException {
        xmlWriter.writeStartElement(string, string1, string2);
        depth++;
    }

    public void writeEmptyElement(String string, String string1) throws XMLStreamException {
        xmlWriter.writeEmptyElement(string, string1);
    }

    public void writeEmptyElement(String string, String string1, String string2)
            throws XMLStreamException {
        xmlWriter.writeEmptyElement(string, string1, string2);
    }

    public void writeEmptyElement(String string) throws XMLStreamException {
        xmlWriter.writeEmptyElement(string);
    }

    public void writeEndElement() throws XMLStreamException {
        xmlWriter.writeEndElement();
        depth--;
    }

    public void writeEndDocument() throws XMLStreamException {
        log.debug("writeEndDocument");
        xmlWriter.writeEndDocument();
        isEndDocument = true; 
    }

    public void close() throws XMLStreamException {
        // TODO: we should probably call flush if the attachments have not been written yet
        log.debug("close");
        // Only call flush because data may have been written to the underlying output stream
        // without ever calling writeStartElement. In this case, close would trigger an
        // exception.
        xmlWriter.flush();
    }

    /**
     * Flush is overridden to trigger the attachment serialization
     */
    public void flush() throws XMLStreamException {
        log.debug("Calling MTOMXMLStreamWriter.flush");
        xmlWriter.flush();
        // flush() triggers the optimized attachment writing.
        // If the optimized attachments are specified, and the xml
        // document is completed, then write out the attachments.
        if (format.isOptimized() && !isComplete & (isEndDocument || depth == 0)) {
            log.debug("The XML writing is completed.  Now the attachments are written");
            isComplete = true;
            try {
                rootPartOutputStream.close();
                // First write the attachments added properly through the DataHandlerWriter extension
                XOPEncodingStreamWriter encoder = (XOPEncodingStreamWriter)xmlWriter;
                for (Iterator it = encoder.getContentIDs().iterator(); it.hasNext(); ) {
                    String contentID = (String)it.next();
                    DataHandler dataHandler = encoder.getDataHandler(contentID);
                    if (preserveAttachments || !(dataHandler instanceof DataHandlerExt)) {
                        multipartWriter.writePart(dataHandler, contentID);
                    } else {
                        OutputStream out = multipartWriter.writePart(dataHandler.getContentType(), contentID);
                        BufferUtils.inputStream2OutputStream(((DataHandlerExt)dataHandler).readOnce(), out);
                        out.close();
                    }
                }
                // Now write parts that have been added by prepareDataHandler
                for (Iterator it = otherParts.iterator(); it.hasNext();) {
                    Part part = (Part)it.next();
                    multipartWriter.writePart(part.getDataHandler(), part.getContentID());
                }
                multipartWriter.complete();
            } catch (IOException e) {
                throw new OMException(e);
            }
        }
    }
    

    public void writeAttribute(String string, String string1) throws XMLStreamException {
        xmlWriter.writeAttribute(string, string1);
    }

    public void writeAttribute(String string, String string1, String string2, String string3)
            throws XMLStreamException {
        xmlWriter.writeAttribute(string, string1, string2, string3);
    }

    public void writeAttribute(String string, String string1, String string2)
            throws XMLStreamException {
        xmlWriter.writeAttribute(string, string1, string2);
    }

    public void writeNamespace(String string, String string1) throws XMLStreamException {
        xmlWriter.writeNamespace(string, string1);
    }

    public void writeDefaultNamespace(String string) throws XMLStreamException {
        xmlWriter.writeDefaultNamespace(string);
    }

    public void writeComment(String string) throws XMLStreamException {
        xmlWriter.writeComment(string);
    }

    public void writeProcessingInstruction(String string) throws XMLStreamException {
        xmlWriter.writeProcessingInstruction(string);
    }

    public void writeProcessingInstruction(String string, String string1)
            throws XMLStreamException {
        xmlWriter.writeProcessingInstruction(string, string1);
    }

    public void writeCData(String string) throws XMLStreamException {
        xmlWriter.writeCData(string);
    }

    public void writeDTD(String string) throws XMLStreamException {
        xmlWriter.writeDTD(string);
    }

    public void writeEntityRef(String string) throws XMLStreamException {
        xmlWriter.writeEntityRef(string);
    }

    public void writeStartDocument() throws XMLStreamException {
        xmlWriter.writeStartDocument();
    }

    public void writeStartDocument(String string) throws XMLStreamException {
        xmlWriter.writeStartDocument(string);
    }

    public void writeStartDocument(String string, String string1) throws XMLStreamException {
        xmlWriter.writeStartDocument(string, string1);
    }

    public void writeCharacters(String string) throws XMLStreamException {
        xmlWriter.writeCharacters(string);
    }

    public void writeCharacters(char[] chars, int i, int i1) throws XMLStreamException {
        xmlWriter.writeCharacters(chars, i, i1);
    }

    public String getPrefix(String string) throws XMLStreamException {
        return xmlWriter.getPrefix(string);
    }

    public void setPrefix(String string, String string1) throws XMLStreamException {
        xmlWriter.setPrefix(string, string1);
    }

    public void setDefaultNamespace(String string) throws XMLStreamException {
        xmlWriter.setDefaultNamespace(string);
    }

    public void setNamespaceContext(NamespaceContext namespaceContext) throws XMLStreamException {
        xmlWriter.setNamespaceContext(namespaceContext);
    }

    public NamespaceContext getNamespaceContext() {
        return xmlWriter.getNamespaceContext();
    }

    public Object getProperty(String string) throws IllegalArgumentException {
        return xmlWriter.getProperty(string);
    }

    /**
     * Check if MTOM is enabled.
     * <p>
     * Note that serialization code should use
     * {@link XMLStreamWriterUtils#writeDataHandler(XMLStreamWriter, DataHandler, String, boolean)}
     * or
     * {@link XMLStreamWriterUtils#writeDataHandler(XMLStreamWriter, DataHandlerProvider, String, boolean)}
     * to submit any binary content and let this writer decide whether the content should be written
     * as base64 encoded character data or using <tt>xop:Include</tt>. This makes optimization
     * entirely transparent for the caller and there should be no need to check if the writer is
     * producing MTOM. However, in some cases this is not possible, such as when integrating with
     * 3rd party libraries. The serialization code should then use
     * {@link #prepareDataHandler(DataHandler)} so that it can write <tt>xop:Include</tt> elements
     * directly to the stream. In that case, the code may use the {@link #isOptimized()} method
     * check if MTOM is enabled at all.
     * 
     * @return <code>true</code> if MTOM is enabled, <code>false</code> otherwise
     */
    public boolean isOptimized() {
        return format.isOptimized();
    }

    public String getContentType() {
        return format.getContentType();
    }

    /**
     * @deprecated
     * Serialization code should use
     * {@link XMLStreamWriterUtils#writeDataHandler(XMLStreamWriter, DataHandler, String, boolean)}
     * or {@link XMLStreamWriterUtils#writeDataHandler(XMLStreamWriter, DataHandlerProvider, String, boolean)}
     * to submit any binary content and let this writer decide whether the content should be
     * written as base64 encoded character data or using <tt>xop:Include</tt>. If this is not
     * possible, then {@link #prepareDataHandler(DataHandler)} should be used.
     */
    public void writeOptimized(OMText node) {
        log.debug("Start MTOMXMLStreamWriter.writeOptimized()");
        otherParts.add(new Part(node.getContentID(), (DataHandler)node.getDataHandler()));    
        log.debug("Exit MTOMXMLStreamWriter.writeOptimized()");
    }

    /**
     * @deprecated
     * Serialization code should use
     * {@link XMLStreamWriterUtils#writeDataHandler(XMLStreamWriter, DataHandler, String, boolean)}
     * or {@link XMLStreamWriterUtils#writeDataHandler(XMLStreamWriter, DataHandlerProvider, String, boolean)}
     * to submit any binary content and let this writer decide whether the content should be
     * written as base64 encoded character data or using <tt>xop:Include</tt>. If this is not
     * possible, then {@link #prepareDataHandler(DataHandler)} should be used.
     * All the aforementioned methods take into account the settings defined in
     * {@link OMOutputFormat} to determine whether the binary data should be optimized or not.
     * Therefore, there is not need for this method anymore.
     */
    public boolean isOptimizedThreshold(OMText node){
        // The optimize argument is set to true for compatibility. Indeed, older versions
        // left it to the caller to check OMText#isOptimized().
        try {
            return optimizationPolicy.isOptimized((DataHandler)node.getDataHandler(), true);
        } catch (IOException ex) {
            return true;
        }
    }
    
    /**
     * Prepare a {@link DataHandler} for serialization without using the {@link DataHandlerWriter}
     * API. The method first determines whether the binary data represented by the
     * {@link DataHandler} should be optimized or inlined. If the data should not be optimized, then
     * the method returns <code>null</code> and the caller is expected to use
     * {@link #writeCharacters(String)} or {@link #writeCharacters(char[], int, int)} to write the
     * base64 encoded data to the stream. If the data should be optimized, then the method returns a
     * content ID and the caller is expected to generate an <tt>xop:Include</tt> element referring
     * to that content ID.
     * <p>
     * This method should only be used to integrate Axiom with third party libraries that support
     * XOP. In all other cases,
     * {@link XMLStreamWriterUtils#writeDataHandler(XMLStreamWriter, DataHandler, String, boolean)}
     * or
     * {@link XMLStreamWriterUtils#writeDataHandler(XMLStreamWriter, DataHandlerProvider, String, boolean)}
     * should be used to write base64Binary values and the application code should never generate
     * <tt>xop:Include</tt> elements itself.
     * 
     * @param dataHandler
     *            the {@link DataHandler} that the caller intends to write to the stream
     * @return the content ID that the caller must use in the <tt>xop:Include</tt> element or
     *         <code>null</code> if the base64 encoded data should not be optimized
     */
    public String prepareDataHandler(DataHandler dataHandler) {
        boolean doOptimize;
        try {
            doOptimize = optimizationPolicy.isOptimized(dataHandler, true);
        } catch (IOException ex) {
            doOptimize = true;
        }
        if (doOptimize) {
            String contentID = getNextContentId();
            otherParts.add(new Part(contentID, dataHandler));
            return contentID;
        } else {
            return null;
        }
    }
    
    public void setXmlStreamWriter(XMLStreamWriter xmlWriter) {
        this.xmlWriter = xmlWriter;
    }

    public XMLStreamWriter getXmlStreamWriter() {
        return xmlWriter;
    }

    public String getMimeBoundary() {
        return format.getMimeBoundary();
    }

    public String getRootContentId() {
        return format.getRootContentId();
    }

    public String getNextContentId() {
        return format.getNextContentId();
    }

    /**
     * Returns the character set encoding scheme. If the value of the charSetEncoding is not set
     * then the default will be returned.
     *
     * @return Returns encoding.
     */
    public String getCharSetEncoding() {
        return format.getCharSetEncoding();
    }

    public void setCharSetEncoding(String charSetEncoding) {
        format.setCharSetEncoding(charSetEncoding);
    }

    public String getXmlVersion() {
        return format.getXmlVersion();
    }

    public void setXmlVersion(String xmlVersion) {
        format.setXmlVersion(xmlVersion);
    }

    public void setSoap11(boolean b) {
        format.setSOAP11(b);
    }

    public boolean isIgnoreXMLDeclaration() {
        return format.isIgnoreXMLDeclaration();
    }

    public void setIgnoreXMLDeclaration(boolean ignoreXMLDeclaration) {
        format.setIgnoreXMLDeclaration(ignoreXMLDeclaration);
    }

    public void setDoOptimize(boolean b) {
        format.setDoOptimize(b);
    }

    /**
     * Get the output format used by this writer.
     * <p>
     * The caller should use the returned instance in a read-only way, i.e.
     * he should not modify the settings of the output format. Any attempt
     * to do so will lead to unpredictable results.
     * 
     * @return the output format used by this writer
     */
    public OMOutputFormat getOutputFormat() {
        return format;
    }

    public void setOutputFormat(OMOutputFormat format) {
        this.format = format;
    }
    
    /**
     * Get the underlying {@link OutputStream} for this writer, if available. This method allows a
     * node (perhaps an {@link org.apache.axiom.om.OMSourcedElement}) to write its content directly
     * to the byte stream.
     * <p>
     * <b>WARNING:</b> This method should be used with extreme care. The caller must be prepared to
     * handle the following issues:
     * <ul>
     * <li>The caller must use the right charset encoding when writing to the stream.
     * <li>The caller should avoid writing byte order marks to the stream.
     * <li>The caller must be aware of the fact that a default namespace might have been set in the
     * context where the byte stream is requested. If the XML data written to the stream contains
     * unqualified elements, then the caller must make sure that the default namespace is redeclared
     * as appropriate.
     * </ul>
     * 
     * @return the underlying byte stream, or <code>null</code> if the stream is not accessible
     */
    public OutputStream getOutputStream() throws XMLStreamException {  
        
        if (xmlStreamWriterFilter != null) {
            if (log.isDebugEnabled()) {
                log.debug("getOutputStream returning null due to presence of XMLStreamWriterFilter " + 
                        xmlStreamWriterFilter);
            }
            return null;
        }
        
        OutputStream os = null;
        if (rootPartOutputStream != null) {
            os = rootPartOutputStream;
        } else {
            os = outStream;
        }
        
        if (log.isDebugEnabled()) {
            if (os == null) {
                log.debug("Direct access to the output stream is not available.");
            } else if (rootPartOutputStream != null) {
                log.debug("Returning access to the buffered xml stream: " + rootPartOutputStream);
            } else {
                log.debug("Returning access to the original output stream: " + os);
            }
        }
       
        if (os != null) {
            // Flush the state of the writer..Many times the 
            // write defers the writing of tag characters (>)
            // until the next write.  Flush out this character
            this.writeCharacters(""); 
            this.flush();
        }
        return os;
    }
    
    public void setFilter(XMLStreamWriterFilter filter) {
        if (filter != null) {
            if (log.isDebugEnabled()) {
                log.debug("setting filter " + filter.getClass());
            }
            xmlStreamWriterFilter = filter;
            filter.setDelegate(xmlWriter);
            xmlWriter = filter;
        }
    }
    
    public XMLStreamWriterFilter removeFilter() {
        XMLStreamWriterFilter filter = null;
        if (xmlStreamWriterFilter != null) {
            filter = xmlStreamWriterFilter;
            if (log.isDebugEnabled()) {
                log.debug("removing filter " + filter.getClass());
            }
            xmlWriter = xmlStreamWriterFilter.getDelegate();
            filter.setDelegate(null);
            xmlStreamWriterFilter = (xmlWriter instanceof XMLStreamWriterFilter) ? 
                        (XMLStreamWriterFilter) xmlWriter : 
                                null;
        }
        return filter;
    }
}
