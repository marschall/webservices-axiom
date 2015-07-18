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

package org.apache.axiom.om.impl.dom;

import static org.apache.axiom.dom.DOMExceptionUtil.newDOMException;

import org.apache.axiom.core.CoreChildNode;
import org.apache.axiom.core.CoreModelException;
import org.apache.axiom.core.NodeMigrationPolicy;
import org.apache.axiom.dom.DOMDocument;
import org.apache.axiom.dom.DOMExceptionUtil;
import org.apache.axiom.om.OMCloneOptions;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.dom.DOMMetaFactory;
import org.apache.axiom.om.impl.common.AxiomDocument;
import org.apache.axiom.om.impl.common.OMDocumentHelper;
import org.apache.axiom.om.impl.common.OMNamespaceImpl;
import org.apache.axiom.om.impl.common.serializer.push.OutputException;
import org.apache.axiom.om.impl.common.serializer.push.Serializer;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class DocumentImpl extends RootNode implements DOMDocument, AxiomDocument {
    private String xmlVersion;

    private String xmlEncoding;
    
    private boolean xmlStandalone = false;
    
    private String charEncoding;

    private Vector idAttrs;

    protected Hashtable identifiers;
    
    public DocumentImpl(OMXMLParserWrapper parserWrapper, OMFactory factory) {
        super(factory);
        coreSetBuilder(parserWrapper);
    }

    public DocumentImpl(OMFactory factory) {
        super(factory);
        coreSetState(COMPLETE);
    }

    public Document getOwnerDocument() {
        return null;
    }

    public void internalSerialize(Serializer serializer, OMOutputFormat format, boolean cache) throws OutputException {
        internalSerialize(serializer, format, cache, !format.isIgnoreXMLDeclaration());
    }

    // /org.w3c.dom.Document methods
    // /

    public Comment createComment(String data) {
        CommentImpl comment = new CommentImpl(data, getOMFactory());
        comment.coreSetOwnerDocument(this);
        return comment;
    }

    public DocumentFragment createDocumentFragment() {
        DocumentFragmentImpl fragment = new DocumentFragmentImpl(getOMFactory());
        fragment.coreSetOwnerDocument(this);
        return fragment;
    }

    public Element createElement(String tagName) throws DOMException {
        ElementImpl element = new ElementImpl(null, tagName, null, null, getOMFactory(), false);
        element.coreSetOwnerDocument(this);
        return element;
    }

    public Element createElementNS(String namespaceURI, String qualifiedName)
            throws DOMException {

        if (namespaceURI != null && namespaceURI.length() == 0) {
            namespaceURI = null;
        }

        String localName = DOMUtil.getLocalName(qualifiedName);
        String prefix = DOMUtil.getPrefix(qualifiedName);
        DOMUtil.validateElementName(namespaceURI, localName, prefix);
        
        OMNamespaceImpl namespace;
        if (namespaceURI == null) {
            namespace = null;
        } else {
            namespace = new OMNamespaceImpl(namespaceURI, prefix == null ? "" : prefix);
        }
        ElementImpl element = new ElementImpl(null, localName, namespace, null, getOMFactory(), false);
        element.coreSetOwnerDocument(this);
        return element;
    }

    public EntityReference createEntityReference(String name) throws DOMException {
        EntityReferenceImpl node = new EntityReferenceImpl(name, null, getOMFactory());
        node.coreSetOwnerDocument(this);
        return node;
    }

    public DocumentType getDoctype() {
        Iterator it = getChildren();
        while (it.hasNext()) {
            Object child = it.next();
            if (child instanceof DocumentType) {
                return (DocumentType)child;
            } else if (child instanceof Element) {
                // A doctype declaration can only appear before the root element. Stop here.
                return null;
            }
        }
        return null;
    }

    public Element getElementById(String elementId) {

        //If there are no id attrs
        if (this.idAttrs == null) {
            return null;
        }

        Enumeration attrEnum = this.idAttrs.elements();
        while (attrEnum.hasMoreElements()) {
            Attr tempAttr = (Attr) attrEnum.nextElement();
            if (tempAttr.getValue().equals(elementId)) {
                return tempAttr.getOwnerElement();
            }
        }

        //If we reach this point then, there's no such attr 
        return null;
    }

    public DOMImplementation getImplementation() {
        return ((DOMMetaFactory)getOMFactory().getMetaFactory()).getDOMImplementation();
    }

    public Node importNode(Node importedNode, boolean deep) throws DOMException {

        short type = importedNode.getNodeType();
        Node newNode = null;
        switch (type) {
            case Node.ELEMENT_NODE: {
                Element newElement;
                if (importedNode.getLocalName() == null) {
                    newElement = this.createElement(importedNode.getNodeName());
                } else {
                    
                    String ns = importedNode.getNamespaceURI();
                    ns = (ns != null) ? ns.intern() : null;
                    newElement = createElementNS(ns, importedNode.getNodeName());
                }

                // Copy element's attributes, if any.
                NamedNodeMap sourceAttrs = importedNode.getAttributes();
                if (sourceAttrs != null) {
                    int length = sourceAttrs.getLength();
                    for (int index = 0; index < length; index++) {
                        try {
                            ((ElementImpl)newElement).coreAppendAttribute((AttrImpl)importNode(sourceAttrs.item(index), true), NodeMigrationPolicy.MOVE_ALWAYS);
                        } catch (CoreModelException ex) {
                            throw DOMExceptionUtil.translate(ex);
                        }
                    }
                }
                newNode = newElement;
                break;
            }

            case Node.ATTRIBUTE_NODE: {
                if (importedNode.getLocalName() == null) {
                    newNode = createAttribute(importedNode.getNodeName());
                } else {
                    String ns = importedNode.getNamespaceURI();
                    ns = (ns != null) ? ns.intern() : null;
                    newNode = createAttributeNS(ns ,
                                                importedNode.getNodeName());
                }
                ((Attr) newNode).setValue(importedNode.getNodeValue());
                break;
            }

            case Node.TEXT_NODE: {
                newNode = createTextNode(importedNode.getNodeValue());
                break;
            }

            case Node.COMMENT_NODE: {
                newNode = createComment(importedNode.getNodeValue());
                break;
            }
                
            case Node.DOCUMENT_FRAGMENT_NODE: {
                newNode = createDocumentFragment();
                // No name, kids carry value
                break;
            }

            case Node.CDATA_SECTION_NODE:
                newNode = createCDATASection(importedNode.getNodeValue());
                break;
            
            case Node.ENTITY_REFERENCE_NODE:
            case Node.ENTITY_NODE:
            case Node.PROCESSING_INSTRUCTION_NODE:
            case Node.DOCUMENT_TYPE_NODE:
            case Node.NOTATION_NODE:
                throw new UnsupportedOperationException("TODO : Implement handling of org.w3c.dom.Node type == " + type );

            case Node.DOCUMENT_NODE: // Can't import document nodes
            default:
                throw newDOMException(DOMException.NOT_SUPPORTED_ERR);
        }

        // If deep, replicate and attach the kids.
        if (deep && !(importedNode instanceof Attr)) {
            for (Node srckid = importedNode.getFirstChild(); srckid != null;
                 srckid = srckid.getNextSibling()) {
                newNode.appendChild(importNode(srckid, true));
            }
        }

        return newNode;

    }

    // /
    // /OMDocument Methods
    // /
    public String getCharsetEncoding() {
        return this.charEncoding;
    }

    public String getXMLVersion() {
        return this.xmlVersion;
    }

    public String isStandalone() {
        return (this.xmlStandalone) ? "yes" : "no";
    }

    public void setCharsetEncoding(String charsetEncoding) {
        this.charEncoding = charsetEncoding;
    }

    public void setOMDocumentElement(OMElement documentElement) {
        if (documentElement == null) {
            throw new IllegalArgumentException("documentElement must not be null");
        }
        OMElement existingDocumentElement = getOMDocumentElement();
        if (existingDocumentElement == null) {
            addChild(documentElement);
        } else {
            OMNode nextSibling = existingDocumentElement.getNextOMSibling();
            existingDocumentElement.detach();
            if (nextSibling == null) {
                addChild(documentElement);
            } else {
                nextSibling.insertSiblingBefore(documentElement);
            }
        }
    }

    public void setStandalone(String isStandalone) {
        this.xmlStandalone = "yes".equalsIgnoreCase(isStandalone);
    }

    public void setXMLVersion(String version) {
        this.xmlVersion = version;
    }

    public String getXMLEncoding() {
        return xmlEncoding;
    }

    public void setXMLEncoding(String encoding) {
        this.xmlEncoding = encoding;
    }
    
    protected void addIdAttr(Attr attr) {
        if (this.idAttrs == null) {
            this.idAttrs = new Vector();
        }
        this.idAttrs.add(attr);
    }

    protected void removeIdAttr(Attr attr) {
        if (this.idAttrs != null) {
            this.idAttrs.remove(attr);
        }

    }

    /*
    * DOM-Level 3 methods
    */

    public Node adoptNode(Node node) throws DOMException {
        if (node instanceof NodeImpl) {
            NodeImpl childNode = (NodeImpl)node;
            if (childNode instanceof CoreChildNode && ((CoreChildNode)childNode).coreHasParent()) {
                ((OMNode)childNode).detach();
            }
            childNode.coreSetOwnerDocument(this);
            if (node instanceof AttrImpl) {
                ((AttrImpl)node).coreSetSpecified(true);
            }
            return childNode;
        } else {
            return null;
        }
    }

    public String getDocumentURI() {
        // TODO TODO
        throw new UnsupportedOperationException("TODO");
    }

    public String getInputEncoding() {
        return charEncoding;
    }

    public boolean getStrictErrorChecking() {
        // TODO TODO
        throw new UnsupportedOperationException("TODO");
    }

    public String getXmlEncoding() {
        return xmlEncoding;
    }

    public boolean getXmlStandalone() {
        return this.xmlStandalone;
    }

    public String getXmlVersion() {
        return getXMLVersion();
    }

    public Node renameNode(Node node, String namespaceURI, String qualifiedName)
            throws DOMException {
        // TODO TODO
        throw new UnsupportedOperationException("TODO");
    }

    public void setDocumentURI(String documentURI) {
        // TODO TODO
        throw new UnsupportedOperationException("TODO");
    }

    public void setStrictErrorChecking(boolean strictErrorChecking) {
        // TODO TODO
        throw new UnsupportedOperationException("TODO");
    }

    public void setXmlStandalone(boolean standalone) throws DOMException {
        this.xmlStandalone = standalone;
    }

    public void setXmlVersion(String version) throws DOMException {
        setXMLVersion(version);
    }

    protected void internalSerialize(Serializer serializer, OMOutputFormat format,
            boolean cache, boolean includeXMLDeclaration) throws OutputException {
        OMDocumentHelper.internalSerialize(this, serializer, format, cache, includeXMLDeclaration);
    }

    ParentNode shallowClone(OMCloneOptions options, ParentNode targetParent, boolean namespaceRepairing) {
        DocumentImpl clone;
        if (options.isPreserveModel()) {
            clone = createClone(options);
        } else {
            clone = new DocumentImpl(getOMFactory());
        }
        clone.xmlVersion = xmlVersion;
        clone.xmlEncoding = xmlEncoding;
        clone.xmlStandalone = xmlStandalone;
        clone.charEncoding = charEncoding;
        return clone;
    }

    protected DocumentImpl createClone(OMCloneOptions options) {
        return new DocumentImpl(getOMFactory());
    }
    
    public final void setComplete(boolean complete) {
        coreSetState(complete ? COMPLETE : INCOMPLETE);
    }

    public final void build() {
        defaultBuild();
    }

    public final void checkChild(OMNode child) {
        if (child instanceof OMElement) {
            if (getOMDocumentElement() != null) {
                throw new OMException("Document element already exists");
            } else {
                checkDocumentElement((OMElement)child);
            }
        }
    }

    protected void checkDocumentElement(OMElement element) {
    }

    public final void setPrefix(String prefix) throws DOMException {
        throw newDOMException(DOMException.NAMESPACE_ERR);
    }
}
