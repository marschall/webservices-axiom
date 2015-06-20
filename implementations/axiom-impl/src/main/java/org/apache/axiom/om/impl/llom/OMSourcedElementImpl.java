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

package org.apache.axiom.om.impl.llom;

import org.apache.axiom.core.CoreChildNode;
import org.apache.axiom.om.OMCloneOptions;
import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMDataSource;
import org.apache.axiom.om.OMDataSourceExt;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMSourcedElement;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.OMXMLStreamReaderConfiguration;
import org.apache.axiom.om.QNameAwareOMDataSource;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.common.OMDataSourceUtil;
import org.apache.axiom.om.impl.common.OMNamespaceImpl;
import org.apache.axiom.om.impl.common.serializer.push.OutputException;
import org.apache.axiom.om.impl.common.serializer.push.Serializer;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

/**
 * <p>Element backed by an arbitrary data source. When necessary, this element will be expanded by
 * creating a parser from the data source.</p>
 * <p/>
 * <p>Whenever methods are added to the base {@link OMElementImpl}
 * class the corresponding methods must be added to this class (there's a unit test to verify that
 * this has been done, just to make sure nothing gets accidentally broken). If the method only
 * requires the element name and/or namespace information, the base class method can be called
 * directly. Otherwise, the element must be expanded into a full OM tree (by calling the {@link
 * #forceExpand()} method) before the base class method is called. This will typically involve a
 * heavy overhead penalty, so should be avoided if possible.</p>
 */
public class OMSourcedElementImpl extends OMElementImpl implements OMSourcedElement {
    
    /** Data source for element data. */
    private OMDataSource dataSource;

    /** Namespace for element, needed in order to bypass base class handling. */
    private OMNamespace definedNamespace = null;
    
    /**
     * Flag indicating whether the {@link #definedNamespace} attribute has been set. If this flag is
     * <code>true</code> and {@link #definedNamespace} is <code>null</code> then the element has no
     * namespace. If this flag is set to <code>false</code> (in which case {@link #definedNamespace}
     * is always <code>null</code>) then the namespace is not known and needs to be determined
     * lazily. The flag is used only if {@link #isExpanded} is <code>false</code>.
     */
    private boolean definedNamespaceSet;

    /** Flag for parser provided to base element class. */
    private boolean isExpanded;

    private static final Log log = LogFactory.getLog(OMSourcedElementImpl.class);
    
    private static final Log forceExpandLog = LogFactory.getLog(OMSourcedElementImpl.class.getName() + ".forceExpand");
    
    private static OMNamespace getOMNamespace(QName qName) {
        return qName.getNamespaceURI().length() == 0 ? null
                : new OMNamespaceImpl(qName.getNamespaceURI(), qName.getPrefix());
    }
    
    public OMSourcedElementImpl(OMFactory factory, OMDataSource source) {
        super(factory);
        dataSource = source;
        isExpanded = false;
    }
    
    /**
     * Constructor.
     *
     * @param localName
     * @param ns
     * @param factory
     * @param source
     */
    public OMSourcedElementImpl(String localName, OMNamespace ns, OMFactory factory,
                                OMDataSource source) {
        super(null, localName, null, null, factory, false);
        if (source == null) {
            throw new IllegalArgumentException("OMDataSource can't be null");
        }
        dataSource = source;
        isExpanded = false;
        // Normalize the namespace. Note that this also covers the case where the
        // namespace URI is empty and the prefix is null (in which case we know that
        // the actual prefix must be empty)
        if (ns != null && ns.getNamespaceURI().length() == 0) {
            ns = null;
        }
        if (ns == null || !(isLossyPrefix(dataSource) || ns.getPrefix() == null)) {
            // Believe the prefix and create a normal OMNamespace
            definedNamespace = ns;
        } else {
            // Create a deferred namespace that forces an expand to get the prefix
            String uri = ns.getNamespaceURI();
            definedNamespace = new DeferredNamespace(uri);
        }
        definedNamespaceSet = true;
    }

    /**
     * Constructor that takes a QName instead of the local name and the namespace seperately
     *
     * @param qName
     * @param factory
     * @param source
     */
    public OMSourcedElementImpl(QName qName, OMFactory factory, OMDataSource source) {
        //create a namespace
        super(null, qName.getLocalPart(), null, null, factory, false);
        if (source == null) {
            throw new IllegalArgumentException("OMDataSource can't be null");
        }
        dataSource = source;
        isExpanded = false;
        if (!isLossyPrefix(dataSource)) {
            // Believe the prefix and create a normal OMNamespace
            definedNamespace = getOMNamespace(qName);
        } else {
            // Create a deferred namespace that forces an expand to get the prefix
            String uri = qName.getNamespaceURI();
            definedNamespace = uri.length() == 0 ? null : new DeferredNamespace(uri);
        }
        definedNamespaceSet = true;
    }

    public OMSourcedElementImpl(String localName, OMNamespace ns, OMContainer parent, OMFactory factory) {
        super(parent, localName, null, null, factory, false);
        dataSource = null;
        definedNamespace = ns;
        isExpanded = true;
        if (ns != null) {
            this.setNamespace(ns);
        }
    }

    public OMSourcedElementImpl(OMContainer parent, String localName, OMNamespace ns,
            OMXMLParserWrapper builder, OMFactory factory, boolean generateNSDecl) {
        super(parent, localName, ns, builder, factory, generateNSDecl);
        definedNamespace = ns;
        isExpanded = true;
    }

    /**
     * The namespace uri is immutable, but the OMDataSource may change
     * the value of the prefix.  This method queries the OMDataSource to 
     * see if the prefix is known.
     * @param source
     * @return true or false
     */
    private boolean isLossyPrefix(OMDataSource source) {
        Object lossyPrefix = null;
        if (source instanceof OMDataSourceExt) {
            lossyPrefix = 
                ((OMDataSourceExt) source).getProperty(OMDataSourceExt.LOSSY_PREFIX);
                        
        }
        return lossyPrefix == Boolean.TRUE;
    }

    /**
     * Generate element name for output.
     *
     * @return name
     */
    private String getPrintableName() {
        if (isExpanded || (definedNamespaceSet && internalGetLocalName() != null)) {
            String uri = null;
            if (getNamespace() != null) {
                uri = getNamespace().getNamespaceURI();
            }
            if (uri == null || uri.length() == 0) {
                return getLocalName();
            } else {
                return "{" + uri + '}' + getLocalName();
            }
        } else {
            return "<unknown>";
        }
    }

    /**
     * Set parser for OM, if not previously set. Since the builder is what actually constructs the
     * tree on demand, this first creates a builder
     */
    public void forceExpand() {
        // The dataSource != null is required because this method may be called indirectly
        // by the constructor before the data source is set. After the constructor has completed,
        // isExpanded is always true if dataSource is null.
        if (!isExpanded && dataSource != null) {

            if (log.isDebugEnabled()) {
                log.debug("forceExpand: expanding element " +
                        getPrintableName());
                if(forceExpandLog.isDebugEnabled()){
                	// When using an OMSourcedElement, it can be particularly difficult to
                	// determine why an expand occurs... a stack trace should help debugging this
                	Exception e = new Exception("Debug Stack Trace");
                	forceExpandLog.debug("forceExpand stack", e);
                }
            }

            if (OMDataSourceUtil.isPushDataSource(dataSource)) {
                // Set this before we start expanding; otherwise this would result in an infinite recursion
                isExpanded = true;
                try {
                    dataSource.serialize(new PushOMBuilder(this));
                } catch (XMLStreamException ex) {
                    throw new OMException("Failed to expand data source", ex);
                }
            } else {
                // Get the XMLStreamReader
                XMLStreamReader readerFromDS;
                try {
                    readerFromDS = dataSource.getReader();  
                } catch (XMLStreamException ex) {
                    throw new OMException("Error obtaining parser from data source for element " + getPrintableName(), ex);
                }
                
                // Advance past the START_DOCUMENT to the start tag.
                // Remember the character encoding.
                String characterEncoding = readerFromDS.getCharacterEncodingScheme();
                if (characterEncoding != null) {
                    characterEncoding = readerFromDS.getEncoding();
                }
                try {
                    if (readerFromDS.getEventType() != XMLStreamConstants.START_ELEMENT) {
                        while (readerFromDS.next() != XMLStreamConstants.START_ELEMENT) ;
                    }
                } catch (XMLStreamException ex) {
                    throw new OMException("Error parsing data source document for element " + getLocalName(), ex);
                }
    
                validateName(readerFromDS.getPrefix(), readerFromDS.getLocalName(), readerFromDS.getNamespaceURI());
    
                // Set the builder for this element. Note that the StAXOMBuilder constructor will also
                // update the namespace of the element, so we don't need to do that here.
                isExpanded = true;
                StAXOMBuilder builder = new StAXOMBuilder(getOMFactory(), readerFromDS, this, characterEncoding);
                builder.setAutoClose(true);
                coreSetBuilder(builder);
                setComplete(false);
            }
        }
    }
    
    /**
     * Validates that the actual name of the element obtained from StAX matches the information
     * specified when the sourced element was constructed or retrieved through the
     * {@link QNameAwareOMDataSource} interface. Also updates the local name if necessary. Note that
     * the namespace information is not updated; this is the responsibility of the builder (and is
     * done at the same time as namespace repairing).
     * 
     * @param staxPrefix
     * @param staxLocalName
     * @param staxNamespaceURI
     */
    void validateName(String staxPrefix, String staxLocalName, String staxNamespaceURI) {
        if (internalGetLocalName() == null) {
            // The local name was not known in advance; initialize it from the reader
            internalSetLocalName(staxLocalName);
        } else {
            // Make sure element local name and namespace matches what was expected
            if (!staxLocalName.equals(internalGetLocalName())) {
                throw new OMException("Element name from data source is " +
                        staxLocalName + ", not the expected " + internalGetLocalName());
            }
        }
        if (definedNamespaceSet) {
            if (staxNamespaceURI == null) {
                staxNamespaceURI = "";
            }
            String namespaceURI = definedNamespace == null ? "" : definedNamespace.getNamespaceURI();
            if (!staxNamespaceURI.equals(namespaceURI)) {
                throw new OMException("Element namespace from data source is " +
                        staxNamespaceURI + ", not the expected " + namespaceURI);
            }
            if (!(definedNamespace instanceof DeferredNamespace)) {
                if (staxPrefix == null) {
                    staxPrefix = "";
                }
                String prefix = definedNamespace == null ? "" : definedNamespace.getPrefix();
                if (!staxPrefix.equals(prefix)) {
                    throw new OMException("Element prefix from data source is '" +
                            staxPrefix + "', not the expected '" + prefix + "'");
                }
            }
        }
    }
    
    /**
     * Check if element has been expanded into tree.
     *
     * @return <code>true</code> if expanded, <code>false</code> if not
     */
    public boolean isExpanded() {
        return isExpanded;
    }

    public XMLStreamReader getXMLStreamReader(boolean cache) {
        return getXMLStreamReader(cache, new OMXMLStreamReaderConfiguration());
    }
    
    public XMLStreamReader getXMLStreamReader(boolean cache, OMXMLStreamReaderConfiguration configuration) {
        if (log.isDebugEnabled()) {
            log.debug("getting XMLStreamReader for " + getPrintableName()
                    + " with cache=" + cache);
        }
        if (isExpanded) {
            return super.getXMLStreamReader(cache, configuration);
        } else {
            if ((cache && OMDataSourceUtil.isDestructiveRead(dataSource)) || OMDataSourceUtil.isPushDataSource(dataSource)) {
                forceExpand();
                return super.getXMLStreamReader(true, configuration);
            } else {
                try {
                    return dataSource.getReader();  
                } catch (XMLStreamException ex) {
                    throw new OMException("Error obtaining parser from data source for element " + getPrintableName(), ex);
                }
            }
        }
    }

    private void ensureLocalNameSet() {
        if (internalGetLocalName() == null) {
            if (dataSource instanceof QNameAwareOMDataSource) {
                internalSetLocalName(((QNameAwareOMDataSource)dataSource).getLocalName());
            }
            if (internalGetLocalName() == null) {
                forceExpand();
            }
        }
    }
    
    public String getLocalName() {
        ensureLocalNameSet();
        return super.getLocalName();
    }

    public void setLocalName(String localName) {
        // Need to expand the element so that the method actually overrides the the local name
        forceExpand();
        super.setLocalName(localName);
    }

    public OMNamespace getNamespace() throws OMException {
        if (isExpanded()) {
            return super.getNamespace();
        } else if (definedNamespaceSet) {
            return definedNamespace;
        } else {
            if (dataSource instanceof QNameAwareOMDataSource) {
                String namespaceURI = ((QNameAwareOMDataSource)dataSource).getNamespaceURI();
                if (namespaceURI != null) {
                    if (namespaceURI.length() == 0) {
                        // No namespace case. definedNamespace is already null, so we only need
                        // to set definedNamespaceSet to true. Note that we don't need to retrieve
                        // the namespace prefix because a prefix can't be bound to the empty
                        // namespace URI.
                        definedNamespaceSet = true;
                    } else {
                        String prefix = ((QNameAwareOMDataSource)dataSource).getPrefix();
                        if (prefix == null) {
                            // Prefix is unknown
                            definedNamespace = new DeferredNamespace(namespaceURI);
                        } else {
                            definedNamespace = new OMNamespaceImpl(namespaceURI, prefix);
                        }
                        definedNamespaceSet = true;
                    }
                }
            }
            if (definedNamespaceSet) {
                return definedNamespace;
            } else {
                // We have no information about the namespace of the element. Need to expand
                // the element to get it.
                forceExpand();
                return super.getNamespace();
            }
        }
    }

    public void setNamespace(OMNamespace namespace) {
        forceExpand();
        super.setNamespace(namespace);
    }

    public void setNamespaceWithNoFindInCurrentScope(OMNamespace namespace) {
        forceExpand();
        super.setNamespaceWithNoFindInCurrentScope(namespace);
    }

    public QName getQName() {
        if (isExpanded()) {
            return super.getQName();
        } else if (getNamespace() != null) {
            // always ignore prefix on name from sourced element
            return new QName(getNamespace().getNamespaceURI(), getLocalName());
        } else {
            return new QName(getLocalName());
        }
    }

    public String toStringWithConsume() throws XMLStreamException {
        if (isExpanded()) {
            return super.toStringWithConsume();
        } else {
            StringWriter writer = new StringWriter();
            XMLStreamWriter writer2 = StAXUtils.createXMLStreamWriter(writer);
            dataSource.serialize(writer2);  // dataSource.serialize consumes the data
            writer2.flush();
            return writer.toString();
        }
    }
    
    OMNode clone(OMCloneOptions options, OMContainer targetParent) {
        // If already expanded or this is not an OMDataSourceExt, then
        // create a copy of the OM Tree
        OMDataSource ds = getDataSource();
        if (!options.isCopyOMDataSources() ||
            ds == null || 
            isExpanded() || 
            !(ds instanceof OMDataSourceExt)) {
            return super.clone(options, targetParent);
        }
        
        // If copying is destructive, then copy the OM tree
        OMDataSourceExt sourceDS = (OMDataSourceExt) ds;
        if (sourceDS.isDestructiveRead() ||
            sourceDS.isDestructiveWrite()) {
            return super.clone(options, targetParent);
        }
        OMDataSourceExt targetDS = ((OMDataSourceExt) ds).copy();
        if (targetDS == null) {
            return super.clone(options, targetParent);
        }
        // Otherwise create a target OMSE with the copied DataSource
        OMSourcedElementImpl targetOMSE;
        if (options.isPreserveModel()) {
            targetOMSE = (OMSourcedElementImpl)createClone(options, targetDS);
        } else {
            targetOMSE = (OMSourcedElementImpl)getOMFactory().createOMElement(targetDS);
        }
        
        targetOMSE.internalSetLocalName(internalGetLocalName());
        targetOMSE.definedNamespaceSet = definedNamespaceSet;
        if (definedNamespace instanceof DeferredNamespace) {
            targetOMSE.definedNamespace = targetOMSE.new DeferredNamespace(definedNamespace.getNamespaceURI());
        } else {
            targetOMSE.definedNamespace = definedNamespace;
        }
        
        if (targetParent != null) {
            targetParent.addChild(targetOMSE);
        }
        return targetOMSE;
    }

    protected OMSourcedElement createClone(OMCloneOptions options, OMDataSource ds) {
        return getOMFactory().createOMElement(ds);
    }

    public void discard() throws OMException {
        // discard without expanding the tree
        setComplete(true);
        super.detach();
    }

    public void internalSerialize(Serializer serializer, OMOutputFormat format, boolean cache)
            throws OutputException {
        if (isExpanded()) {
            super.internalSerialize(serializer, format, cache);
        } else if (cache) {
            if (OMDataSourceUtil.isDestructiveWrite(dataSource)) {
                forceExpand();
                super.internalSerialize(serializer, format, true);
            } else {
                serializer.serialize(dataSource);
            }
        } else {
            serializer.serialize(dataSource); 
        }
    }

    public OMNode detach() throws OMException {
        // detach without expanding the tree
        boolean complete = isComplete();
        setComplete(true);
        OMNode result = super.detach();
        setComplete(complete);
        return result;
    }

    OMNamespace handleNamespace(QName qname) {
        forceExpand();
        return super.handleNamespace(qname);
    }

    public int getState() {
        if (isExpanded) {
            return super.getState();
        } else {
            return COMPLETE;
        }
    }

    public boolean isComplete() {
        if (isExpanded) {
            return super.isComplete();
        } else {
            return true;
        }
    }

    public String toString() {
        if (isExpanded) {
            return super.toString();
        } else if (OMDataSourceUtil.isDestructiveWrite(dataSource)) {
            forceExpand();
            return super.toString();
        } else {
            try {
                StringWriter writer = new StringWriter();
                OMOutputFormat format = new OMOutputFormat();
                dataSource.serialize(writer, format);
                String text = writer.toString();
                writer.close();
                return text;
            } catch (XMLStreamException e) {
                throw new RuntimeException("Cannot serialize OM Element " + this.getLocalName(), e);
            } catch (IOException e) {
                throw new RuntimeException("Cannot serialize OM Element " + this.getLocalName(), e);
            }
        }
    }

    public void buildWithAttachments() {
        
        // If not done, force the parser to build the elements
        if (getState() == INCOMPLETE) {
            this.build();
        }
        
        // If the OMSourcedElement is in in expanded form, then
        // walk the descendents to make sure they are built. 
        // If the OMSourcedElement is backed by a OMDataSource,
        // we don't want to walk the children (because this will result
        // in an unnecessary translation from OMDataSource to a full OM tree).
        if (isExpanded()) {
            Iterator iterator = getChildren();
            while (iterator.hasNext()) {
                OMNode node = (OMNode) iterator.next();
                node.buildWithAttachments();
            }
        }
    }

    /**
     * Provide access to the data source encapsulated in OMSourcedElement. 
     * This is usesful when we want to access the raw data in the data source.
     *
     * @return the internal datasource
     */
    public OMDataSource getDataSource() {
        return dataSource;
    }
    
    /**
     * setOMDataSource
     */
    public OMDataSource setDataSource(OMDataSource dataSource) {
        if (!isExpanded()) {
            OMDataSource oldDS = this.dataSource;
            this.dataSource = dataSource;
            return oldDS;  // Caller is responsible for closing the data source
        } else {
            // TODO
            // Remove the entire subtree and replace with 
            // new datasource.  There maybe a more performant way to do this.
            OMDataSource oldDS = this.dataSource;
            Iterator it = getChildren();
            while(it.hasNext()) {
                it.next();
                it.remove();
            }
            this.dataSource = dataSource;
            setComplete(false);
            isExpanded = false;
            coreSetBuilder(null);
            if (isLossyPrefix(dataSource)) {
                // Create a deferred namespace that forces an expand to get the prefix
                definedNamespace = new DeferredNamespace(definedNamespace.getNamespaceURI());
            }
            return oldDS;
        }
    }

    /**
     * setComplete override The OMSourcedElement has its own isolated builder/reader during the
     * expansion process. Thus calls to setCompete should stop here and not propogate up to the
     * parent (which may have a different builder or no builder).
     */
    public void setComplete(boolean complete) {
        coreSetState(complete ? COMPLETE : INCOMPLETE);
        if (complete && dataSource != null) {
            if (dataSource instanceof OMDataSourceExt) {
                ((OMDataSourceExt)dataSource).close();
            }
            dataSource = null;
        }
    }
    
    class DeferredNamespace implements OMNamespace {
        
        final String uri;
        
        DeferredNamespace(String ns) {
            this.uri = ns;
        }

        public boolean equals(String uri, String prefix) {
            String thisPrefix = getPrefix();
            return (this.uri.equals(uri) &&
                    (thisPrefix == null ? prefix == null :
                            thisPrefix.equals(prefix)));
        }

        public String getName() {
            return uri;
        }

        public String getNamespaceURI() {
            return uri;
        }

        public String getPrefix() {
            if (!isExpanded()) {
                forceExpand();
            }
            OMNamespace actualNS = getNamespace();
            return actualNS == null ? "" : actualNS.getPrefix();
        }
        
        public int hashCode() {
            String thisPrefix = getPrefix();
            return uri.hashCode() ^ (thisPrefix != null ? thisPrefix.hashCode() : 0);
        }
        
        public boolean equals(Object obj) {
            if (!(obj instanceof OMNamespace)) {
                return false;
            }
            OMNamespace other = (OMNamespace)obj;
            String otherPrefix = other.getPrefix();
            String thisPrefix = getPrefix();
            return (uri.equals(other.getNamespaceURI()) &&
                    (thisPrefix == null ? otherPrefix == null :
                            thisPrefix.equals(otherPrefix)));
        }
        
    }

    public Object getObject(Class dataSourceClass) {
        if (dataSource == null || isExpanded || !dataSourceClass.isInstance(dataSource)) {
            return null;
        } else {
            return ((OMDataSourceExt)dataSource).getObject();
        }
    }

    public CoreChildNode coreGetFirstChildIfAvailable() {
        forceExpand();
        return super.coreGetFirstChildIfAvailable();
    }
}
