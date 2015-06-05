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

import org.apache.axiom.om.OMCloneOptions;
import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.impl.common.AxiomProcessingInstruction;
import org.apache.axiom.om.impl.common.serializer.push.OutputException;
import org.apache.axiom.om.impl.common.serializer.push.Serializer;

public class OMProcessingInstructionImpl extends OMLeafNode implements AxiomProcessingInstruction {
    protected String target;
    protected String value;

    /**
     * Constructor OMProcessingInstructionImpl.
     *
     * @param parentNode
     * @param target
     * @param value
     */
    public OMProcessingInstructionImpl(OMContainer parentNode, String target,
                                       String value, OMFactory factory, boolean fromBuilder) {
        super(parentNode, factory, fromBuilder);
        this.target = target;
        this.value = value;
    }

    public final int getType() {
        return OMNode.PI_NODE;
    }

    public void internalSerialize(Serializer serializer, OMOutputFormat format, boolean cache) throws OutputException {
        serializer.writeProcessingInstruction(this.target + " ", this.value);
    }

    /**
     * Gets the value of this Processing Instruction.
     *
     * @return string
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the target of this Processing Instruction.
     *
     * @param target
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Gets the target of this Processing Instruction.
     *
     * @return Returns String.
     */
    public String getTarget() {
        return target;
    }

    /**
     * Sets the value of this Processing Instruction.
     *
     * @param text
     */
    public void setValue(String text) {
        this.value = text;
    }

    OMNode clone(OMCloneOptions options, OMContainer targetParent) {
        return getOMFactory().createOMProcessingInstruction(targetParent, target, value);
    }
}
