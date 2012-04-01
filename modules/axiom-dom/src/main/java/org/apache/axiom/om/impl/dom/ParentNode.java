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

import org.apache.axiom.om.OMComment;
import org.apache.axiom.om.OMDocType;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMProcessingInstruction;
import org.apache.axiom.om.OMSourcedElement;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.OMXMLStreamReaderConfiguration;
import org.apache.axiom.om.impl.OMContainerEx;
import org.apache.axiom.om.impl.OMNodeEx;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.common.OMChildrenLocalNameIterator;
import org.apache.axiom.om.impl.common.OMChildrenNamespaceIterator;
import org.apache.axiom.om.impl.common.OMChildrenQNameIterator;
import org.apache.axiom.om.impl.common.OMContainerHelper;
import org.apache.axiom.om.impl.common.OMDescendantsIterator;
import org.apache.axiom.om.impl.dom.factory.OMDOMFactory;
import org.apache.axiom.om.impl.jaxp.OMSource;
import org.apache.axiom.om.impl.traverse.OMChildrenIterator;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.sax.SAXSource;

import java.util.Iterator;

public abstract class ParentNode extends ChildNode implements OMContainerEx {

    protected ChildNode firstChild;

    protected ChildNode lastChild;

    /** @param ownerDocument  */
    protected ParentNode(DocumentImpl ownerDocument, OMFactory factory) {
        super(ownerDocument, factory);
    }

    protected ParentNode(OMFactory factory) {
        super(factory);
    }

    // /
    // /OMContainer methods
    // /

    public OMXMLParserWrapper getBuilder() {
        return this.builder;
    }

    public void addChild(OMNode omNode) {
        if (omNode.getOMFactory() instanceof OMDOMFactory) {
            Node domNode = (Node) omNode;
            if (ownerDocument() != null && !domNode.getOwnerDocument().equals(ownerDocument())) {
                this.appendChild(ownerDocument().importNode(domNode, true));
            } else {
                this.appendChild(domNode);
            }
        } else {
            addChild(importNode(omNode));
        }

    }

    public void buildNext() {
        if (builder != null) {
            builder.next();
        }
    }

    public Iterator getChildren() {
        return new OMChildrenIterator(getFirstOMChild());
    }

    public Iterator getDescendants(boolean includeSelf) {
        return new OMDescendantsIterator(this, includeSelf);
    }

    /**
     * Returns an iterator of child nodes having a given qname.
     *
     * @see org.apache.axiom.om.OMContainer#getChildrenWithName (javax.xml.namespace.QName)
     */
    public Iterator getChildrenWithName(QName elementQName) throws OMException {
        return new OMChildrenQNameIterator(getFirstOMChild(), elementQName);
    }
    
    public Iterator getChildrenWithLocalName(String localName) {
        return new OMChildrenLocalNameIterator(getFirstOMChild(),
                                               localName);
    }


    public Iterator getChildrenWithNamespaceURI(String uri) {
        return new OMChildrenNamespaceIterator(getFirstOMChild(),
                                               uri);
    }

    /**
     * Returns the first OMElement child node.
     *
     * @see org.apache.axiom.om.OMContainer#getFirstChildWithName (javax.xml.namespace.QName)
     */
    public OMElement getFirstChildWithName(QName elementQName)
            throws OMException {
        Iterator children = new OMChildrenQNameIterator(getFirstOMChild(),
                                                        elementQName);
        while (children.hasNext()) {
            OMNode node = (OMNode) children.next();

            // Return the first OMElement node that is found
            if (node instanceof OMElement) {
                return (OMElement) node;
            }
        }
        return null;
    }

    public OMNode getFirstOMChild() {
        while ((firstChild == null) && !done) {
            buildNext();
        }
        return (OMNode)firstChild;
    }

    public OMNode getFirstOMChildIfAvailable() {
        return (OMNode)firstChild;
    }

    public void setFirstChild(OMNode omNode) {
        if (firstChild != null) {
            ((OMNodeEx) omNode).setParent(this);
        }
        this.firstChild = (ChildNode) omNode;
    }

    /**
     * Forcefully set the last child
     * @param omNode
     */
    public void setLastChild(OMNode omNode) {
        this.lastChild = (ChildNode) omNode;
    }

    // /
    // /DOM Node methods
    // /

    public NodeList getChildNodes() {
        if (!this.done) {
            this.build();
        }
        return new NodeListImpl() {
            protected Iterator getIterator() {
                return getChildren();
            }
        };
    }

    public Node getFirstChild() {
        return (Node) this.getFirstOMChild();
    }

    public Node getLastChild() {
        if (!this.done) {
            this.build();
        }
        return this.lastChild;
    }

    public boolean hasChildNodes() {
        while ((firstChild == null) && !done) {
            buildNext();
        }
        return this.firstChild != null;
    }

    /**
     * Inserts newChild before the refChild. If the refChild is null then the newChild is made the
     * last child.
     */
    public Node insertBefore(Node newChild, Node refChild) throws DOMException {

        ChildNode newDomChild = (ChildNode) newChild;
        ChildNode refDomChild = (ChildNode) refChild;

        if (ownerDocument() != newDomChild.ownerDocument()) {
            throw new DOMException(DOMException.WRONG_DOCUMENT_ERR,
                                   DOMMessageFormatter.formatMessage(
                                           DOMMessageFormatter.DOM_DOMAIN,
                                           DOMException.WRONG_DOCUMENT_ERR, null));
        }

        if (isAncestorOrSelf(newChild)) {
            throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,
                                   DOMMessageFormatter.formatMessage(
                                           DOMMessageFormatter.DOM_DOMAIN,
                                           DOMException.HIERARCHY_REQUEST_ERR, null));
        }

        if (newDomChild.parentNode() != null) {
            //If the newChild is already in the tree remove it
            newDomChild.parentNode().removeChild(newDomChild);
        }

        if (this instanceof Document) {
            if (newDomChild instanceof ElementImpl) {
                if (((DocumentImpl) this).getOMDocumentElement(false) != null) {
                    // Throw exception since there cannot be two document elements
                    throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,
                                           DOMMessageFormatter.formatMessage(
                                                   DOMMessageFormatter.DOM_DOMAIN,
                                                   DOMException.HIERARCHY_REQUEST_ERR, null));
                }
                if (newDomChild.parentNode() == null) {
                    newDomChild.setParent(this);
                }
            } else if (!(newDomChild instanceof CommentImpl
                    || newDomChild instanceof ProcessingInstructionImpl
                    || newDomChild instanceof DocumentFragmentImpl
                    || newDomChild instanceof DocumentTypeImpl)) {
                throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,
                        DOMMessageFormatter.formatMessage(
                                DOMMessageFormatter.DOM_DOMAIN,
                                DOMException.HIERARCHY_REQUEST_ERR, null));
            }
        }
        
        if (refChild == null) { // Append the child to the end of the list
            // if there are no children
            if (this.lastChild == null && firstChild == null) {
                this.lastChild = newDomChild;
                this.firstChild = newDomChild;
                this.firstChild.isFirstChild(true);
                newDomChild.setParent(this);
            } else {
                this.lastChild.nextSibling = newDomChild;
                newDomChild.previousSibling = this.lastChild;
                this.lastChild = newDomChild;
                this.lastChild.nextSibling = null;
            }
            if (newDomChild.parentNode() == null) {
                newDomChild.setParent(this);
            }
        } else {
            Iterator children = this.getChildren();
            boolean found = false;
            while (children.hasNext()) {
                ChildNode tempNode = (ChildNode) children.next();

                if (tempNode.equals(refChild)) {
                    // RefChild found
                    if (this.firstChild == tempNode) { // If the refChild is the
                        // first child

                        if (newChild instanceof DocumentFragmentImpl) {
                            // The new child is a DocumentFragment
                            DocumentFragmentImpl docFrag =
                                    (DocumentFragmentImpl) newChild;
                            
                            ChildNode child = docFrag.firstChild;
                            while (child != null) {
                                child.setParent(this);
                                child = child.nextSibling;
                            }
                            
                            this.firstChild = docFrag.firstChild;
                            docFrag.lastChild.nextSibling = refDomChild;
                            refDomChild.previousSibling =
                                    docFrag.lastChild.nextSibling;

                            docFrag.firstChild = null;
                            docFrag.lastChild = null;
                        } else {

                            // Make the newNode the first Child
                            this.firstChild = newDomChild;

                            newDomChild.nextSibling = refDomChild;
                            refDomChild.previousSibling = newDomChild;

                            this.firstChild.isFirstChild(true);
                            refDomChild.isFirstChild(false);
                            newDomChild.previousSibling = null; // Just to be
                            // sure :-)

                        }
                    } else { // If the refChild is not the fist child
                        ChildNode previousNode = refDomChild.previousSibling;

                        if (newChild instanceof DocumentFragmentImpl) {
                            // the newChild is a document fragment
                            DocumentFragmentImpl docFrag =
                                    (DocumentFragmentImpl) newChild;

                            ChildNode child = docFrag.firstChild;
                            while (child != null) {
                                child.setParent(this);
                                child = child.nextSibling;
                            }
                            
                            previousNode.nextSibling = docFrag.firstChild;
                            docFrag.firstChild.previousSibling = previousNode;

                            docFrag.lastChild.nextSibling = refDomChild;
                            refDomChild.previousSibling = docFrag.lastChild;

                            docFrag.firstChild = null;
                            docFrag.lastChild = null;
                        } else {

                            previousNode.nextSibling = newDomChild;
                            newDomChild.previousSibling = previousNode;

                            newDomChild.nextSibling = refDomChild;
                            refDomChild.previousSibling = newDomChild;
                        }

                    }
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new DOMException(DOMException.NOT_FOUND_ERR,
                                       DOMMessageFormatter.formatMessage(
                                               DOMMessageFormatter.DOM_DOMAIN,
                                               DOMException.NOT_FOUND_ERR, null));
            }

            if (newDomChild.parentNode() == null) {
                newDomChild.setParent(this);
            }

        }
        
        if (!newDomChild.isComplete() && !(newDomChild instanceof OMSourcedElement)) {
            setComplete(false);
        }
        
        return newChild;
    }

    /** Replaces the oldChild with the newChild. */
    public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
        ChildNode newDomChild = (ChildNode) newChild;
        ChildNode oldDomChild = (ChildNode) oldChild;

        if (newChild == null) {
            return this.removeChild(oldChild);
        }

        if (isAncestorOrSelf(newChild)) {
            throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,
                                   DOMMessageFormatter.formatMessage(
                                           DOMMessageFormatter.DOM_DOMAIN,
                                           DOMException.HIERARCHY_REQUEST_ERR, null));
        }

        if (newDomChild != null &&
                //This is the case where this is an Element in the document
                (ownerDocument() != null && !ownerDocument().equals(newDomChild.ownerDocument())) ||
                //This is the case where this is the Document itself
                (ownerDocument() == null && !this.equals(ownerDocument()))) {
            throw new DOMException(DOMException.WRONG_DOCUMENT_ERR,
                                   DOMMessageFormatter.formatMessage(
                                           DOMMessageFormatter.DOM_DOMAIN,
                                           DOMException.WRONG_DOCUMENT_ERR, null));
        }

        Iterator children = this.getChildren();
        boolean found = false;
        while (!found && children.hasNext()) {
            ChildNode tempNode = (ChildNode) children.next();
            if (tempNode.equals(oldChild)) {
                if (newChild instanceof DocumentFragmentImpl) {
                    DocumentFragmentImpl docFrag =
                            (DocumentFragmentImpl) newDomChild;
                    ChildNode child = (ChildNode) docFrag.getFirstChild();
                    this.replaceChild(child, oldChild);
                    
                    //set the parent of all kids to me
                    while(child != null) {
                        child.setParent(this);
                        child = child.nextSibling;
                    }

                    this.lastChild = (ChildNode)docFrag.getLastChild();
                    
                } else {
                    if (this.firstChild == oldDomChild) {

                        if (this.firstChild.nextSibling != null) {
                            this.firstChild.nextSibling.previousSibling = newDomChild;
                            newDomChild.nextSibling = this.firstChild.nextSibling;
                        }

                        //Cleanup the current first child
                        this.firstChild.setParent(null);
                        this.firstChild.nextSibling = null;

                        //Set the new first child
                        this.firstChild = newDomChild;
                        

                    } else {
                        newDomChild.nextSibling = oldDomChild.nextSibling;
                        newDomChild.previousSibling = oldDomChild.previousSibling;

                        oldDomChild.previousSibling.nextSibling = newDomChild;

                        // If the old child is not the last
                        if (oldDomChild.nextSibling != null) {
                            oldDomChild.nextSibling.previousSibling = newDomChild;
                        } else {
                            this.lastChild = newDomChild;
                        }

                    }

                    newDomChild.setParent(this);
                }
                found = true;

                // remove the old child's references to this tree
                oldDomChild.nextSibling = null;
                oldDomChild.previousSibling = null;
                oldDomChild.setParent(null);
            }
        }

        if (!found)
            throw new DOMException(DOMException.NOT_FOUND_ERR,
                                   DOMMessageFormatter.formatMessage(
                                           DOMMessageFormatter.DOM_DOMAIN, DOMException.NOT_FOUND_ERR,
                                           null));

        return oldChild;
    }

    /** Removes the given child from the DOM Tree. */
    public Node removeChild(Node oldChild) throws DOMException {
        if (oldChild.getParentNode() == this) {
            ((ChildNode)oldChild).detach();
            return oldChild;
        } else {
            throw new DOMException(DOMException.NOT_FOUND_ERR,
                                   DOMMessageFormatter.formatMessage(
                                           DOMMessageFormatter.DOM_DOMAIN, DOMException.NOT_FOUND_ERR,
                                           null));
        }
    }

    /**
     * Checks if the given node is an ancestor (or identical) to this node.
     * 
     * @param node
     *            the node to check
     * @return <code>true</code> if the node is an ancestor or indentical to this node
     */
    private boolean isAncestorOrSelf(Node node) {
        Node currentNode = this;
        do {
            if (currentNode == node) {
                return true;
            }
            currentNode = currentNode.getParentNode();
        } while (currentNode != null);
        return false;
    }

    public Node cloneNode(boolean deep) {

        ParentNode newnode = (ParentNode) super.cloneNode(deep);

        // set parent and owner document
        newnode.setParent(null);

        // Need to break the association w/ original kids
        newnode.firstChild = null;
        newnode.lastChild = null;

        // Then, if deep, clone the kids too.
        if (deep) {
            for (ChildNode child = firstChild; child != null;
                 child = child.nextSibling) {
                newnode.appendChild(child.cloneNode(true));
            }
        }
        return newnode;
    }

    /**
     * This method is intended only to be used by Axiom intenals when merging Objects from different
     * Axiom implementations to the DOOM implementation.
     *
     * @param child
     */
    protected OMNode importNode(OMNode child) {
        int type = child.getType();
        switch (type) {
            case (OMNode.ELEMENT_NODE): {
                OMElement childElement = (OMElement) child;
                OMElement newElement = (new StAXOMBuilder(this.factory,
                                                          childElement.getXMLStreamReader()))
                        .getDocumentElement();
                newElement.build();
                return (OMNode)ownerDocument().importNode((Element) newElement,
                                                          true);
            }
            case (OMNode.TEXT_NODE): {
                OMText importedText = (OMText) child;
                OMText newText;
                if (importedText.isBinary()) {
                    boolean isOptimize = importedText.isOptimized();
                    newText = this.factory.createOMText(importedText
                            .getDataHandler(), isOptimize);
                } else if (importedText.isCharacters()) {
                    newText = new TextImpl((DocumentImpl) this.getOwnerDocument(),
                                           importedText.getTextCharacters(), this.factory);
                } else {
                    newText = new TextImpl((DocumentImpl) this.getOwnerDocument(),
                                           importedText.getText(), this.factory);
                }
                return newText;
            }

            case (OMNode.PI_NODE): {
                OMProcessingInstruction importedPI = (OMProcessingInstruction) child;
                OMProcessingInstruction newPI = this.factory
                        .createOMProcessingInstruction(this,
                                                       importedPI.getTarget(),
                                                       importedPI.getValue());
                return newPI;
            }
            case (OMNode.COMMENT_NODE): {
                OMComment importedComment = (OMComment) child;
                OMComment newComment = this.factory.createOMComment(this,
                                                                    importedComment.getValue());
                DocumentImpl doc;
                if (this instanceof DocumentImpl) {
                    doc = (DocumentImpl) this;
                } else {
                    doc = (DocumentImpl) getOwnerDocument();
                }
                newComment = new CommentImpl(doc, importedComment.getValue(),
                                             this.factory);
                return newComment;
            }
            case (OMNode.DTD_NODE): {
                OMDocType importedDocType = (OMDocType) child;
                OMDocType newDocType = this.factory.createOMDocType(this,
                                                                    importedDocType.getValue());
                return newDocType;
            }
            default: {
                throw new UnsupportedOperationException(
                        "Not Implemented Yet for the given node type");
            }
        }
    }
    
    public String getTextContent() throws DOMException {
        Node child = getFirstChild();
        if (child != null) {
            Node next = child.getNextSibling();
            if (next == null) {
                return hasTextContent(child) ? ((NodeImpl)child).getTextContent() : "";
            }
            StringBuffer buf = new StringBuffer();
            getTextContent(buf);
            return buf.toString();
        } else {
            return "";
        }
    }

    void getTextContent(StringBuffer buf) throws DOMException {
        Node child = getFirstChild();
        while (child != null) {
            if (hasTextContent(child)) {
                ((NodeImpl)child).getTextContent(buf);
            }
            child = child.getNextSibling();
        }
    }
    
    // internal method returning whether to take the given node's text content
    private static boolean hasTextContent(Node child) {
        return child.getNodeType() != Node.COMMENT_NODE &&
            child.getNodeType() != Node.PROCESSING_INSTRUCTION_NODE /* &&
            (child.getNodeType() != Node.TEXT_NODE ||
             ((TextImpl) child).isIgnorableWhitespace() == false)*/;
    }
    
    public void setTextContent(String textContent) throws DOMException {
        // get rid of any existing children
        // TODO: there is probably a better way to remove all children
        Node child;
        while ((child = getFirstChild()) != null) {
            removeChild(child);
        }
        // create a Text node to hold the given content
        if (textContent != null && textContent.length() != 0) {
            addChild(factory.createOMText(textContent));
        }
    }

    public XMLStreamReader getXMLStreamReaderWithoutCaching() {
        return getXMLStreamReader(false);
    }

    public XMLStreamReader getXMLStreamReader() {
        return getXMLStreamReader(true);
    }

    public XMLStreamReader getXMLStreamReader(boolean cache) {
        return OMContainerHelper.getXMLStreamReader(this, cache);
    }
    
    public XMLStreamReader getXMLStreamReader(boolean cache, OMXMLStreamReaderConfiguration configuration) {
        return OMContainerHelper.getXMLStreamReader(this, cache, configuration);
    }

    public SAXSource getSAXSource(boolean cache) {
        return new OMSource(this);
    }

    void notifyChildComplete() {
        if (!this.done && builder == null) {
            Iterator iterator = getChildren();
            while (iterator.hasNext()) {
                OMNode node = (OMNode) iterator.next();
                if (!node.isComplete()) {
                    return;
                }
            }
            this.setComplete(true);
        }
    }

    void normalize(DOMConfigurationImpl config) {
        OMNode child = getFirstOMChild();
        while (child != null) {
            ((NodeImpl)child).normalize(config);
            child = child.getNextOMSibling();
        }
    }
}
