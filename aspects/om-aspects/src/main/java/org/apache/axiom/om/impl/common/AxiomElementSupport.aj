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
package org.apache.axiom.om.impl.common;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.core.CoreAttribute;
import org.apache.axiom.core.CoreParentNode;
import org.apache.axiom.core.ElementAction;
import org.apache.axiom.core.IdentityMapper;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMConstants;
import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.OMSourcedElement;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.common.serializer.push.OutputException;
import org.apache.axiom.om.impl.common.serializer.push.Serializer;
import org.apache.axiom.om.impl.intf.AxiomAttribute;
import org.apache.axiom.om.impl.intf.AxiomContainer;
import org.apache.axiom.om.impl.intf.AxiomElement;
import org.apache.axiom.om.impl.intf.AxiomNamespaceDeclaration;
import org.apache.axiom.om.impl.util.OMSerializerUtil;
import org.apache.axiom.util.namespace.MapBasedNamespaceContext;
import org.apache.axiom.util.stax.XMLStreamReaderUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class with default implementations for some of the methods defined by the
 * {@link OMElement} interface.
 */
public aspect AxiomElementSupport {
    private static final Log log = LogFactory.getLog(AxiomElementSupport.class);
    
    private int AxiomElement.lineNumber;
    
    public final void AxiomElement.initName(String localName, OMNamespace ns, boolean generateNSDecl) {
        internalSetLocalName(localName);
        internalSetNamespace(generateNSDecl ? NSUtil.handleNamespace(this, ns, false, true) : ns);
    }
    
    final void AxiomElement.beforeSetLocalName() {
        forceExpand();
    }
    
    public final int AxiomElement.getType() {
        return OMNode.ELEMENT_NODE;
    }
    
    public final void AxiomElement.setNamespaceWithNoFindInCurrentScope(OMNamespace namespace) {
        forceExpand();
        internalSetNamespace(namespace);
    }

    public final void AxiomElement.setNamespace(OMNamespace namespace, boolean decl) {
        forceExpand();
        internalSetNamespace(NSUtil.handleNamespace(this, namespace, false, decl));
    }

    public final OMElement AxiomElement.getFirstElement() {
        OMNode node = getFirstOMChild();
        while (node != null) {
            if (node.getType() == OMNode.ELEMENT_NODE) {
                return (OMElement) node;
            } else {
                node = node.getNextOMSibling();
            }
        }
        return null;
    }

    public final Iterator AxiomElement.getChildElements() {
        return new OMChildElementIterator(getFirstElement());
    }

    public final Iterator AxiomElement.getNamespacesInScope() {
        return new NamespaceIterator(this);
    }

    public NamespaceContext AxiomElement.getNamespaceContext(boolean detached) {
        if (detached) {
            Map namespaces = new HashMap();
            for (Iterator it = getNamespacesInScope(); it.hasNext(); ) {
                OMNamespace ns = (OMNamespace)it.next();
                namespaces.put(ns.getPrefix(), ns.getNamespaceURI());
            }
            return new MapBasedNamespaceContext(namespaces);
        } else {
            return new LiveNamespaceContext(this);
        }
    }
    
    public final QName AxiomElement.resolveQName(String qname) {
        int idx = qname.indexOf(':');
        if (idx == -1) {
            OMNamespace ns = getDefaultNamespace();
            return ns == null ? new QName(qname) : new QName(ns.getNamespaceURI(), qname, "");
        } else {
            String prefix = qname.substring(0, idx);
            OMNamespace ns = findNamespace(null, prefix);
            return ns == null ? null : new QName(ns.getNamespaceURI(), qname.substring(idx+1), prefix);
        }
    }

    // TODO: this is (incorrectly) overridden by the SOAPFaultReason implementations for SOAP 1.2
    public String AxiomElement.getText() {
        return coreGetCharacterData(ElementAction.SKIP).toString();
    }
    
    // Note: must not be final because it is (incorrectly) overridden in the SOAPFaultCode implementation for SOAP 1.2
    public QName AxiomElement.getTextAsQName() {
        String childText = getText().trim();
        return childText.length() == 0 ? null : resolveQName(childText);
    }

    public Reader AxiomElement.getTextAsStream(boolean cache) {
        // If the element is not an OMSourcedElement and has not more than one child, then the most
        // efficient way to get the Reader is to build a StringReader
        if (!(this instanceof OMSourcedElement) && (!cache || isComplete())) {
            OMNode child = getFirstOMChild();
            if (child == null) {
                return new StringReader("");
            } else if (child.getNextOMSibling() == null) {
                return new StringReader(child instanceof OMText ? ((OMText)child).getText() : "");
            }
        }
        // In all other cases, extract the data from the XMLStreamReader
        try {
            XMLStreamReader reader = getXMLStreamReader(cache);
            if (reader.getEventType() == XMLStreamReader.START_DOCUMENT) {
                reader.next();
            }
            return XMLStreamReaderUtils.getElementTextAsStream(reader, true);
        } catch (XMLStreamException ex) {
            throw new OMException(ex);
        }
    }
    
    public void AxiomElement.writeTextTo(Writer out, boolean cache) throws IOException {
        try {
            XMLStreamReader reader = getXMLStreamReader(cache);
            int depth = 0;
            while (reader.hasNext()) {
                switch (reader.next()) {
                    case XMLStreamReader.CHARACTERS:
                    case XMLStreamReader.CDATA:
                        if (depth == 1) {
                            out.write(reader.getText());
                        }
                        break;
                    case XMLStreamReader.START_ELEMENT:
                        depth++;
                        break;
                    case XMLStreamReader.END_ELEMENT:
                        depth--;
                }
            }
        } catch (XMLStreamException ex) {
            throw new OMException(ex);
        }
    }
    
    // Not final because overridden in Abdera
    public void AxiomElement.setText(String text) {
        coreSetCharacterData(text, Policies.DETACH_POLICY);
    }

    public final void AxiomElement.setText(QName qname) {
        removeChildren();
        // Add a new text node
        if (qname != null) {
            OMNamespace ns = handleNamespace(qname.getNamespaceURI(), qname.getPrefix());
            getOMFactory().createOMText(this,
                    ns == null ? qname.getLocalPart() : ns.getPrefix() + ":" + qname.getLocalPart());
        }
    }

    public final void AxiomElement.discard() {
        if (getState() == CoreParentNode.INCOMPLETE && getBuilder() != null) {
            ((StAXOMBuilder)getBuilder()).discard((OMContainer)this);
        }
        detach();
    }
    
    public void AxiomElement.detachAndDiscardParent() {
        internalUnsetParent(null);
        coreSetPreviousSibling(null);
        coreSetNextSibling(null);
    }
    
    public void AxiomElement.insertChild(Class[] sequence, int pos, OMNode newChild) {
        if (!sequence[pos].isInstance(newChild)) {
            throw new IllegalArgumentException();
        }
        OMNode child = getFirstOMChild();
        while (child != null) {
            if (child instanceof OMElement) {
                if (child == newChild) {
                    // The new child is already a child of the element and it is at
                    // the right position
                    return;
                }
                if (sequence[pos].isInstance(child)) {
                    // Replace the existing child
                    child.insertSiblingAfter(newChild);
                    child.detach();
                    return;
                }
                // isAfter indicates if the new child should be inserted after the current child
                boolean isAfter = false;
                for (int i=0; i<pos; i++) {
                    if (sequence[i].isInstance(child)) {
                        isAfter = true;
                        break;
                    }
                }
                if (!isAfter) {
                    // We found the right position to insert the new child
                    child.insertSiblingBefore(newChild);
                    return;
                }
            }
            child = child.getNextOMSibling();
        }
        // Else, add the new child at the end
        addChild(newChild);
    }

    public final OMNamespace AxiomElement.handleNamespace(String namespaceURI, String prefix) {
        if (prefix.length() == 0 && namespaceURI.length() == 0) {
            OMNamespace namespace = getDefaultNamespace();
            if (namespace != null) {
                declareDefaultNamespace("");
            }
            return null;
        } else {
            OMNamespace namespace = findNamespace(namespaceURI,
                                                  prefix);
            if (namespace == null) {
                namespace = declareNamespace(namespaceURI, prefix.length() > 0 ? prefix : null);
            }
            return namespace;
        }
    }
    
    public final void AxiomElement.internalAppendAttribute(OMAttribute attr) {
        coreSetAttribute(Policies.ATTRIBUTE_MATCHER, (AxiomAttribute)attr, Policies.DETACH_POLICY);
    }
    
    public final OMAttribute AxiomElement.addAttribute(OMAttribute attr){
        // If the attribute already has an owner element then clone the attribute (except if it is owned
        // by the this element)
        OMElement owner = attr.getOwner();
        if (owner != null) {
            if (owner == this) {
                return attr;
            }
            attr = getOMFactory().createOMAttribute(attr.getLocalName(), attr.getNamespace(), attr.getAttributeValue());
        }
        NSUtil.handleNamespace(this, attr.getNamespace(), true, true);
        internalAppendAttribute(attr);
        return attr;
    }

    public final OMAttribute AxiomElement.addAttribute(String localName, String value, OMNamespace ns) {
        OMNamespace namespace = null;
        if (ns != null) {
            String namespaceURI = ns.getNamespaceURI();
            String prefix = ns.getPrefix();
            if (namespaceURI.length() > 0 || prefix != null) {
                namespace = findNamespace(namespaceURI, prefix);
                if (namespace == null || prefix == null && namespace.getPrefix().length() == 0) {
                    namespace = new OMNamespaceImpl(namespaceURI, prefix != null ? prefix : OMSerializerUtil.getNextNSPrefix());
                }
            }
        }
        return addAttribute(getOMFactory().createOMAttribute(localName, namespace, value));
    }

    private static final IdentityMapper<AxiomAttribute> attributeIdentityMapper = new IdentityMapper<AxiomAttribute>();
    
    @SuppressWarnings("rawtypes")
    public final Iterator AxiomElement.getAllAttributes() {
        return coreGetAttributesByType(AxiomAttribute.class, attributeIdentityMapper, Policies.DETACH_POLICY);
    }
    
    public final OMAttribute AxiomElement.getAttribute(QName qname) {
        return (AxiomAttribute)coreGetAttribute(Policies.ATTRIBUTE_MATCHER, qname.getNamespaceURI(), qname.getLocalPart());
    }

    // TODO: overridden in fom-impl
    public String AxiomElement.getAttributeValue(QName qname) {
        OMAttribute attr = getAttribute(qname);
        return attr == null ? null : attr.getAttributeValue();
    }

    // TODO: complete the implementation (i.e. support value == null and the no namespace case) and add the method to the OMElement API
    public final void AxiomElement._setAttributeValue(QName qname, String value) {
        OMAttribute attr = getAttribute(qname);
        if (attr != null) {
            attr.setAttributeValue(value);
        } else {
            addAttribute(qname.getLocalPart(), value, new OMNamespaceImpl(qname.getNamespaceURI(), qname.getLocalPart()));
        }
    }
    
    public final void AxiomElement.removeAttribute(OMAttribute attr) {
        if (attr.getOwner() != this) {
            throw new OMException("The attribute is not owned by this element");
        }
        ((AxiomAttribute)attr).coreRemove(Policies.DETACH_POLICY);
    }

    public final OMNamespace AxiomElement.addNamespaceDeclaration(String uri, String prefix) {
        OMNamespace ns = new OMNamespaceImpl(uri, prefix);
        AxiomNamespaceDeclaration decl = coreGetNodeFactory().createNode(AxiomNamespaceDeclaration.class);
        decl.setDeclaredNamespace(ns);
        coreAppendAttribute(decl);
        return ns;
    }
    
    public final void AxiomElement.addNamespaceDeclaration(OMNamespace ns) {
        AxiomNamespaceDeclaration decl = coreGetNodeFactory().createNode(AxiomNamespaceDeclaration.class);
        decl.setDeclaredNamespace(ns);
        coreSetAttribute(Policies.NAMESPACE_DECLARATION_MATCHER, decl, Policies.DETACH_POLICY);
    }
    
    @SuppressWarnings("rawtypes")
    public final Iterator AxiomElement.getAllDeclaredNamespaces() {
        return coreGetAttributesByType(AxiomNamespaceDeclaration.class, NamespaceDeclarationMapper.INSTANCE, Policies.DETACH_POLICY);
    }

    public final OMNamespace AxiomElement.declareNamespace(OMNamespace namespace) {
        String prefix = namespace.getPrefix();
        if (prefix == null) {
            prefix = OMSerializerUtil.getNextNSPrefix();
            namespace = new OMNamespaceImpl(namespace.getNamespaceURI(), prefix);
        }
        if (prefix.length() > 0 && namespace.getNamespaceURI().length() == 0) {
            throw new IllegalArgumentException("Cannot bind a prefix to the empty namespace name");
        }
        addNamespaceDeclaration(namespace);
        return namespace;
    }

    public final OMNamespace AxiomElement.declareNamespace(String uri, String prefix) {
        if ("".equals(prefix)) {
            log.warn("Deprecated usage of OMElement#declareNamespace(String,String) with empty prefix");
            prefix = OMSerializerUtil.getNextNSPrefix();
        }
        OMNamespaceImpl ns = new OMNamespaceImpl(uri, prefix);
        return declareNamespace(ns);
    }

    public final OMNamespace AxiomElement.declareDefaultNamespace(String uri) {
        OMNamespace elementNamespace = getNamespace();
        if (elementNamespace == null && uri.length() > 0
                || elementNamespace != null && elementNamespace.getPrefix().length() == 0 && !elementNamespace.getNamespaceURI().equals(uri)) {
            throw new OMException("Attempt to add a namespace declaration that conflicts with " +
                    "the namespace information of the element");
        }
        OMNamespace namespace = new OMNamespaceImpl(uri == null ? "" : uri, "");
        addNamespaceDeclaration(namespace);
        return namespace;
    }

    public final void AxiomElement.undeclarePrefix(String prefix) {
        addNamespaceDeclaration(new OMNamespaceImpl("", prefix));
    }

    public final OMNamespace AxiomElement.findNamespace(String uri, String prefix) {

        // check in the current element
        OMNamespace namespace = findDeclaredNamespace(uri, prefix);
        if (namespace != null) {
            return namespace;
        }

        // go up to check with ancestors
        OMContainer parent = getParent();
        if (parent != null) {
            //For the OMDocumentImpl there won't be any explicit namespace
            //declarations, so going up the parent chain till the document
            //element should be enough.
            if (parent instanceof OMElement) {
                namespace = ((OMElement) parent).findNamespace(uri, prefix);
                // If the prefix has been redeclared, then ignore the binding found on the ancestors
                if (namespace != null && findDeclaredNamespace(null, namespace.getPrefix()) != null) {
                    namespace = null;
                }
            }
        }

        return namespace;
    }

    private static final OMNamespace XMLNS = new OMNamespaceImpl(OMConstants.XMLNS_URI, OMConstants.XMLNS_PREFIX);

    /**
     * Checks for the namespace <B>only</B> in the current Element. This is also used to retrieve
     * the prefix of a known namespace URI.
     */
    private OMNamespace AxiomElement.findDeclaredNamespace(String uri, String prefix) {
        CoreAttribute attr = coreGetFirstAttribute();
        while (attr != null) {
            if (attr instanceof AxiomNamespaceDeclaration) {
                OMNamespace namespace = ((AxiomNamespaceDeclaration)attr).getDeclaredNamespace();
                if ((prefix == null || prefix.equals(namespace.getPrefix()))
                        && (uri == null || uri.equals(namespace.getNamespaceURI()))) {
                    return namespace;
                }
            }
            attr = attr.coreGetNextAttribute();
        }

        //If the prefix is available and uri is available and its the xml namespace
        if ((prefix == null || prefix.equals(OMConstants.XMLNS_PREFIX))
                && (uri == null || uri.equals(OMConstants.XMLNS_URI))) {
            return XMLNS;
        } else {
            return null;
        }
    }

    public final OMNamespace AxiomElement.findNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException();
        }
        CoreAttribute attr = coreGetFirstAttribute();
        while (attr != null) {
            if (attr instanceof AxiomNamespaceDeclaration) {
                AxiomNamespaceDeclaration nsDecl = (AxiomNamespaceDeclaration)attr;
                if (nsDecl.coreGetDeclaredPrefix().equals(prefix)) {
                    OMNamespace ns = nsDecl.getDeclaredNamespace();
                    if (ns.getNamespaceURI().length() == 0) {
                        // We are either in the prefix undeclaring case (XML 1.1 only) or the namespace
                        // declaration is xmlns="". In both cases we need to return null.
                        return null;
                    } else {
                        return ns;
                    }
                }
            }
            attr = attr.coreGetNextAttribute();
        }
        OMContainer parent = getParent();
        if (parent instanceof OMElement) {
            // try with the parent
            return ((OMElement)parent).findNamespaceURI(prefix);
        } else {
            return null;
        }
    }

    public final OMNamespace AxiomElement.getDefaultNamespace() {
        return findNamespaceURI("");
    }

    public void AxiomElement.internalSerialize(Serializer serializer, OMOutputFormat format,
            boolean cache) throws OutputException {
        defaultInternalSerialize(serializer, format, cache);
    }
    
    public final void AxiomElement.defaultInternalSerialize(Serializer serializer, OMOutputFormat format,
            boolean cache) throws OutputException {
        serializer.serializeStartpart(this);
        serializeChildren(serializer, format, cache);
        serializer.writeEndElement();
    }

    public final String AxiomElement.toStringWithConsume() throws XMLStreamException {
        StringWriter sw = new StringWriter();
        serializeAndConsume(sw);
        return sw.toString();
    }

    public final String AxiomElement.toString() {
        StringWriter sw = new StringWriter();
        try {
            serialize(sw);
        } catch (XMLStreamException ex) {
            throw new OMException("Failed to serialize node", ex);
        }
        return sw.toString();
    }

    public void AxiomElement.setComplete(boolean complete) {
        coreSetState(complete ? COMPLETE : INCOMPLETE);
        AxiomContainer parent = (AxiomContainer)coreGetParent();
        if (parent != null) {
            if (!complete) {
                parent.setComplete(false);
            } else {
                parent.notifyChildComplete();
            }
        }
    }

    public final OMElement AxiomElement.cloneOMElement() {
        return (OMElement)clone(null);
    }

    public final void AxiomElement.buildWithAttachments() {
        if (getState() == INCOMPLETE) {
            build();
        }
        if (isExpanded()) {
            OMNode child = getFirstOMChild();
            while (child != null) {
                child.buildWithAttachments();
                child = child.getNextOMSibling();
            }
        }
    }

    public void AxiomElement.checkChild(OMNode child) {
    }

    public final void AxiomElement.setNamespace(OMNamespace namespace) {
        setNamespace(namespace, true);
    }

    public final void AxiomElement.setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public final int AxiomElement.getLineNumber() {
        return lineNumber;
    }
}
