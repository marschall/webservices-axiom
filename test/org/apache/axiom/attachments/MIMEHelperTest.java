/*
 * Copyright 2004,2005 The Apache Software Foundation.
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
 */

package org.apache.axiom.attachments;

import java.awt.Image;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.activation.DataHandler;
import javax.imageio.ImageIO;

import org.apache.axiom.attachments.utils.ImageDataSource;
import org.apache.axiom.om.AbstractTestCase;

public class MIMEHelperTest extends AbstractTestCase {

    public MIMEHelperTest(String testName) {
        super(testName);
    }

    String inMimeFileName = "mtom/MTOMAttachmentStream.bin";
    String img1FileName = "mtom/img/test.jpg";
    String img2FileName = "mtom/img/test2.jpg";

    String contentTypeString = "multipart/related; boundary=MIMEBoundaryurn:uuid:A3ADBAEE51A1A87B2A11443668160701; type=\"application/xop+xml\"; start=\"<0.urn:uuid:A3ADBAEE51A1A87B2A11443668160702@apache.org>\"; start-info=\"application/soap+xml\"; charset=UTF-8;action=\"mtomSample\"";

    public void testMIMEHelper() {
    }

    public void testGetAttachmentSpecType() {
    }

    public void testSimultaneousStreamAccess() throws Exception {
        InputStream inStream; 
        MIMEHelper mimeHelper; 

        inStream = new FileInputStream(getTestResourceFile(inMimeFileName));
        mimeHelper = new MIMEHelper(inStream, contentTypeString);

        mimeHelper.getDataHandler("2.urn:uuid:A3ADBAEE51A1A87B2A11443668160994@apache.org");
        
        // This should throw an error
        try {
        	mimeHelper.getIncomingAttachmentStreams();
        	fail("No exception caught when attempting to access datahandler and stream at the same time");
        } catch (IllegalStateException ise) {
        	// Nothing
        }
        
        inStream.close();
        
        // Try the other way around.
        inStream = new FileInputStream(getTestResourceFile(inMimeFileName));
        mimeHelper = new MIMEHelper(inStream, contentTypeString);

        mimeHelper.getIncomingAttachmentStreams();

        // These should NOT throw error even though they are using part based access
        try {
            assertEquals("application/xop+xml; charset=UTF-8; type=\"application/soap+xml\";", mimeHelper.getSOAPPartContentType());
        } catch (IllegalStateException ise) {
        	fail("No exception expected when requesting SOAP part data");
        	ise.printStackTrace();
        }

        try {
            mimeHelper.getSOAPPartInputStream();
        } catch (IllegalStateException ise) {
        	fail("No exception expected when requesting SOAP part data");
        }
        
        // These should throw an error
        try {
            mimeHelper.getDataHandler("2.urn:uuid:A3ADBAEE51A1A87B2A11443668160994@apache.org");
        	fail("No exception caught when attempting to access stream and datahandler at the same time");
        } catch (IllegalStateException ise) {
        	// Nothing
        }
        
        // Additionally, we also need to ensure mutual exclusion if someone 
        // tries to access part data directly 

        try {
            mimeHelper.getAllContentIDs();
        	fail("No exception caught when attempting to access stream and contentids list at the same time");
        } catch (IllegalStateException ise) {
        	// Nothing
        }
        
        try {
            mimeHelper.getPart("2.urn:uuid:A3ADBAEE51A1A87B2A11443668160994@apache.org");
        	fail("No exception caught when attempting to access stream and part at the same time");
        } catch (IllegalStateException ise) {
        	// Nothing
        }
    }

    public void testGetInputAttachhmentStreams() throws Exception {

        Image expectedImage;
        IncomingAttachmentInputStream dataIs;
        ImageDataSource dataSource;
        InputStream expectedDataIs;
    	
        InputStream inStream = new FileInputStream(getTestResourceFile(inMimeFileName));
        MIMEHelper mimeHelper = new MIMEHelper(inStream, contentTypeString);

        // Since SOAP part operated independently of other streams, access it 
        // directly, and then get to the streams. If this sequence throws an 
        // error, something is wrong with the stream handling code.
        InputStream is = mimeHelper.getSOAPPartInputStream();
        while (is.read() != -1);
        
        // Get the inputstream container
        IncomingAttachmentStreams ias = mimeHelper.getIncomingAttachmentStreams();

        dataIs = ias.getNextStream();
        expectedImage = ImageIO.read(new FileInputStream(getTestResourceFile(img1FileName)));
        dataSource = new ImageDataSource("test1.jpg", expectedImage);
        expectedDataIs = dataSource.getInputStream();
        compareStreams(dataIs, expectedDataIs);

        dataIs = ias.getNextStream();
        expectedImage = ImageIO.read(new FileInputStream(getTestResourceFile(img2FileName)));
        dataSource = new ImageDataSource("test2.jpg", expectedImage);
        expectedDataIs = dataSource.getInputStream();
        compareStreams(dataIs, expectedDataIs);
        
        // Confirm that no more streams are left
        assertEquals(null, ias.getNextStream());

        // After all is done, we should *still* be able to access and 
        // re-consume the SOAP part stream, as it should be cached.. can we?
        is = mimeHelper.getSOAPPartInputStream();
        while (is.read() != -1);
    }

    private void compareStreams(InputStream data, InputStream expected) throws Exception {
        // Compare data across streams
        int i = 0, expectedData = 0;
        
        while ((i = data.read()) != -1 && (expectedData = expected.read()) != -1) {
        	if (i != expectedData) {
        		fail("Data streams do not match: " + i + " != " + expectedData);
        	}
        }
        
        // Ensure that *both* streams have ended
        if ((i == -1 && expected.read() != -1) || (expectedData == -1 && i != -1)) {
        	fail("Data streams do not match: " + i + " != " + expectedData);
        }
    }

    public void testGetDataHandler() throws Exception {

        InputStream inStream = new FileInputStream(getTestResourceFile(inMimeFileName));
        MIMEHelper mimeHelper = new MIMEHelper(inStream, contentTypeString);
        
        DataHandler dh = mimeHelper.getDataHandler("2.urn:uuid:A3ADBAEE51A1A87B2A11443668160994@apache.org");
        InputStream dataIs = dh.getDataSource().getInputStream();

        Image expectedImage = ImageIO.read(new FileInputStream(getTestResourceFile(img2FileName)));
        ImageDataSource dataSource = new ImageDataSource("test.jpg", expectedImage);
        InputStream expectedDataIs = dataSource.getInputStream();

        // Compare data across streams
        compareStreams(dataIs, expectedDataIs);
    }
}
