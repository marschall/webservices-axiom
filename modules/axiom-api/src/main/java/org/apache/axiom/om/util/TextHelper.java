/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.axiom.om.util;

import java.io.IOException;
import java.io.InputStream;

public class TextHelper {
    public static String toString(InputStream inStream) throws IOException {
        byte[] data;
        StringBuffer text = new StringBuffer();
        do {
            data = new byte[1023];
            int len;
            while ((len = inStream.read(data)) > 0) {
                Base64.encode(data, 0, len, text);
            }
        } while (inStream.available() > 0);
        return text.toString();
    }
}
