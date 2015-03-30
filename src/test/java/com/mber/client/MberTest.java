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

package com.mber.client;
import java.util.concurrent.Callable;
import java.util.Date;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Assume;

public class MberTest
{
  private final long spinTimeout = 5000;
  private final long spinPause = 10;

  public void checkMberVariables()
  {
    // If the user says they can't connect to Mber, skip tests that require connections.
    Assume.assumeFalse(isSkipMber());

    assertNotEmpty("No MBER_URL variable found", getMberUrl());
    assertNotEmpty("No MBER_USERNAME variable found", getMberUsername());
    assertNotEmpty("No MBER_PASSWORD variable found", getMberPassword());
    assertNotEmpty("No MBER_APPLICATION variable found", getMberApplicationId());
  }

  public void assertNotEmpty(final String message, final String value)
  {
    Assert.assertNotNull(message, value);
    Assert.assertFalse(message, value.isEmpty());
  }

  public void assertSuccess(final String message, final JSONObject json)
  {
    if (json.has("error")) {
      System.err.println(MberJSON.getString(json, "error"));
    }
    Assert.assertTrue(message, json.has("status"));
    Assert.assertEquals(message, "Success", json.getString("status"));
  }

  public void assertSpin(final String message, Callable<Boolean> predicate) throws Exception
  {
    assertSpin(spinTimeout, message, predicate);
  }

  public boolean isSkipMber()
  {
    String skip = getPropertyOrDefault("mber.skip", "false");
    return skip != null && (skip.equalsIgnoreCase("true") || skip.equals("1"));
  }

  public String getMberUrl()
  {
    return getPropertyOrDefault("mber.url", "http://localhost:8089");
  }

  public String getMberUsername()
  {
    return getPropertyOrDefault("mber.username", "username");
  }

  public String getMberPassword()
  {
    return getPropertyOrDefault("mber.password", "password");
  }

  public String getMberApplicationId()
  {
    return getPropertyOrDefault("mber.application", "'Mber");
  }

  private void assertSpin(final long timeout, final String message, Callable<Boolean> predicate) throws Exception
  {
    boolean success = false;
    long now = getTime();
    do {
      success = predicate.call();
      if (!success) {
        Thread.sleep(spinPause);
      }
    } while(getTime() - now < timeout && !success);
    Assert.assertTrue(message, success);
  }

  private long getTime()
  {
    return (new Date()).getTime();
  }

  private String getPropertyOrDefault(final String property, final String fallback)
  {
    String prop = System.getProperty(property);
    if (prop != null && !prop.isEmpty()) {
      return prop;
    }
    return fallback;
  }
}
