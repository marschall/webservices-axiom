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

package org.apache.axiom.soap.impl.dom;

import org.apache.axiom.om.OMCloneOptions;
import org.apache.axiom.om.OMDataSource;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.impl.dom.NSAwareElement;
import org.apache.axiom.om.impl.dom.ParentNode;
import org.apache.axiom.soap.SOAPCloneOptions;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axiom.soap.impl.common.AxiomSOAPHeaderBlock;

public abstract class SOAPHeaderBlockImpl extends NSAwareElement implements AxiomSOAPHeaderBlock {
    public SOAPHeaderBlockImpl(OMFactory factory) {
        super(factory);
    }

    public SOAPHeaderBlockImpl(ParentNode parentNode, String localName, OMNamespace ns,
            OMXMLParserWrapper builder, OMFactory factory, boolean generateNSDecl) {
        super(parentNode, localName, ns, builder, factory, generateNSDecl);
    }

    public OMDataSource getDataSource() {
        throw new UnsupportedOperationException();
    }

    public boolean isExpanded() {
        return true;
    }

    public OMDataSource setDataSource(OMDataSource dataSource) {
        throw new UnsupportedOperationException();
    }

    public Object getObject(Class dataSourceClass) {
        throw new UnsupportedOperationException();
    }

    protected final void copyData(OMCloneOptions options, SOAPHeaderBlock targetSHB) {
        // Copy the processed flag.  The other SOAPHeaderBlock information 
        // (e.g. role, mustUnderstand) are attributes on the tag and are copied elsewhere.
        Boolean processedFlag = options instanceof SOAPCloneOptions ? ((SOAPCloneOptions)options).getProcessedFlag() : null;
        if ((processedFlag == null && isProcessed()) || (processedFlag != null && processedFlag.booleanValue())) {
            targetSHB.setProcessed();
        }
    }
}
