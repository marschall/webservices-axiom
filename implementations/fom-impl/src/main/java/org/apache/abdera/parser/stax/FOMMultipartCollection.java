package org.apache.abdera.parser.stax;

import static org.apache.abdera.util.Constants.ACCEPT;
import static org.apache.abdera.util.Constants.ALTERNATE;
import static org.apache.abdera.util.Constants.LN_ALTERNATE_MULTIPART_RELATED;
import static org.apache.abdera.util.Constants.PRE_RFC_ACCEPT;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.activation.MimeType;

import org.apache.abdera.model.Collection;
import org.apache.abdera.model.Element;
import org.apache.abdera.util.MimeTypeHelper;
import org.apache.axiom.fom.AbderaElement;

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
public class FOMMultipartCollection extends FOMCollection {
    public boolean acceptsMultipart(String mediaType) {
        Map<String, String> accept = getAcceptMultiparted();
        if (accept.size() == 0)
            accept = Collections.singletonMap("application/atom+xml;type=entry", null);
        for (Map.Entry<String, String> entry : accept.entrySet()) {
            if (MimeTypeHelper.isMatch(entry.getKey(), mediaType) && entry.getValue() != null
                && entry.getValue().equals(LN_ALTERNATE_MULTIPART_RELATED))
                return true;
        }
        return false;
    }

    public boolean acceptsMultipart(MimeType mediaType) {
        return accepts(mediaType.toString());
    }

    public Map<String, String> getAcceptMultiparted() {
        Map<String, String> accept = new HashMap<String, String>();
        Iterator<AbderaElement> i = _getChildrenWithName(ACCEPT);
        if (i == null || !i.hasNext())
            i = _getChildrenWithName(PRE_RFC_ACCEPT);
        while (i.hasNext()) {
            Element e = i.next();
            String t = e.getText();
            if (t != null) {
                if (e.getAttributeValue(ALTERNATE) != null && e.getAttributeValue(ALTERNATE).trim().length() > 0) {
                    accept.put(t.trim(), e.getAttributeValue(ALTERNATE));
                } else {
                    accept.put(t.trim(), null);
                }
            }
        }
        return accept;
    }

    public Collection setAccept(String mediaRange, String alternate) {
        return setAccept(Collections.singletonMap(mediaRange, alternate));
    }

    public Collection setAccept(Map<String, String> mediaRanges) {
        if (mediaRanges != null && mediaRanges.size() > 0) {
            _removeChildren(ACCEPT, true);
            _removeChildren(PRE_RFC_ACCEPT, true);
            if (mediaRanges.size() == 1 && mediaRanges.keySet().iterator().next().equals("")) {
                addExtension(ACCEPT);
            } else {
                for (Map.Entry<String, String> entry : mediaRanges.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase("entry")) {
                        addSimpleExtension(ACCEPT, "application/atom+xml;type=entry");
                    } else {
                        try {
                            Element accept = addSimpleExtension(ACCEPT, new MimeType(entry.getKey()).toString());
                            if (entry.getValue() != null) {
                                accept.setAttributeValue(ALTERNATE, entry.getValue());
                            }
                        } catch (javax.activation.MimeTypeParseException e) {
                            throw new org.apache.abdera.util.MimeTypeParseException(e);
                        }
                    }
                }
            }
        } else {
            _removeChildren(ACCEPT, true);
            _removeChildren(PRE_RFC_ACCEPT, true);
        }
        return this;
    }

    public Collection addAccepts(String mediaRange, String alternate) {
        return addAccepts(Collections.singletonMap(mediaRange, alternate));
    }

    public Collection addAccepts(Map<String, String> mediaRanges) {
        if (mediaRanges != null) {
            for (Map.Entry<String, String> entry : mediaRanges.entrySet()) {
                if (!accepts(entry.getKey())) {
                    try {
                        Element accept = addSimpleExtension(ACCEPT, new MimeType(entry.getKey()).toString());
                        if (entry.getValue() != null) {
                            accept.setAttributeValue(ALTERNATE, entry.getValue());
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        return this;
    }
}
