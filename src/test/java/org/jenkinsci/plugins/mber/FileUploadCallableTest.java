/*
The Jenkins Mber Plugin is free software distributed under the terms of the MIT
license (http://opensource.org/licenses/mit-license.html) reproduced here:

Copyright (c) 2013-2015 Mber

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package org.jenkinsci.plugins.mber;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class FileUploadCallableTest
{
  @Test
  public void handlesInvalidUploads() throws Exception
  {
    File temp = File.createTempFile("jenkins-mber-plugin", "txt");
    BufferedWriter io = new BufferedWriter(new FileWriter(temp));
    io.write("This is an upload test for the Jenkins Mber Plugin.");
    io.close();

    JSONObject result;

    // Fails with an error if the URL passed to the uploader is invalid.
    result = (new FileUploadCallable("")).invoke(temp, null);
    Assert.assertEquals("Uploaded file unexpectedly", "Failed", result.getString("status"));
    assertNotEmpty("No error message found", result.getString("error"));

    // Fails with an error if the file passed to the uploader is invalid.
    result = (new FileUploadCallable("http://this.is.mber")).invoke(null, null);
    Assert.assertEquals("Uploaded file unexpectedly", "Failed", result.getString("status"));
    assertNotEmpty("No error message found", result.getString("error"));

    // Fails with an error if the file passed to the uploader is a folder.
    result = (new FileUploadCallable("http://this.is.mber")).invoke(makeTempDir(), null);
    Assert.assertEquals("Uploaded folder unexpectedly", "Failed", result.getString("status"));
    assertNotEmpty("No error message found", result.getString("error"));
  }

  private void assertNotEmpty(final String message, final String value)
  {
    Assert.assertNotNull(message, value);
    Assert.assertFalse(message, value.isEmpty());
  }

  private File makeTempDir() throws IOException
  {
    final File temp = Files.createTempDirectory("temp" + Long.toString(System.nanoTime())).toFile();
    Assert.assertTrue("Directory isn't a folder: " + temp.getAbsolutePath(), temp.isDirectory());
    return temp;
  }
}
