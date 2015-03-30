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
import hudson.model.BuildListener;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Iterator;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jenkinsci.plugins.mber.LoggingFileEntity;

public class HTTParty
{
  private final static String MBER_VERSION = "0.1.x";

  static public class Call
  {
    public final String method;
    public final URI uri;
    public final int code;
    public final String body;
    public Call(final String method, final URI uri, final int code, final String body) {
      this.method = method;
      this.uri = uri;
      this.code = code;
      this.body = body;
    }
  }

  public static Call get(final String url) throws IOException
  {
    return get(url, null);
  }

  public static Call get(String url, final JSONObject args) throws IOException
  {
    if (args != null) {
      url += toQuery(args);
    }

    HttpGet request = new HttpGet(url);
    return execute(request);
  }

  public static Call put(final String url, final JSONObject data) throws UnsupportedEncodingException, IOException
  {
    HttpPut request = new HttpPut(url);
    request.setEntity(toStringEntity(data));

    return execute(request);
  }

  public static Call put(final String url, final File file) throws IOException
  {
    FileEntity entity = new FileEntity(file);
    entity.setContentType("application/octet-stream");

    HttpPut request = new HttpPut(url);
    request.setEntity(entity);

    return execute(request);
  }

  public static Call put(final String url, final File file, final BuildListener listener) throws IOException
  {
    LoggingFileEntity entity = new LoggingFileEntity(file, listener);
    entity.setContentType("application/octet-stream");

    HttpPut request = new HttpPut(url);
    request.setEntity(entity);

    return execute(request);
  }

  public static Call delete(String url, final JSONObject args) throws IOException
  {
    if (args != null) {
      url += toQuery(args);
    }

    HttpDelete request = new HttpDelete(url);
    return execute(request);
  }

  public static Call post(final String url, final JSONObject data) throws UnsupportedEncodingException, IOException
  {
    HttpPost request = new HttpPost(url);
    request.setEntity(toStringEntity(data));

    return execute(request);
  }

  private static Call execute(final HttpUriRequest request) throws IOException
  {
    HttpClient client = new DefaultHttpClient();
    try {
      request.addHeader("REST-API-Version", MBER_VERSION);
      HttpResponse response = client.execute(request);
      String body = toString(response.getEntity().getContent());
      return new Call(request.getMethod(), request.getURI(), response.getStatusLine().getStatusCode(), body);
    }
    finally {
      client.getConnectionManager().shutdown();
    }
  }

  private static String toString(final InputStream input) throws IOException
  {
    StringWriter writer = new StringWriter();
    IOUtils.copy(input, writer, "UTF-8");
    return writer.toString();
  }

  private static StringEntity toStringEntity(final JSONObject json) throws UnsupportedEncodingException
  {
    StringEntity entity = new StringEntity(json.toString(), "UTF-8");
    entity.setContentType("application/json; charset=utf-8");
    return entity;
  }

  private static String toQuery(final JSONObject json) throws UnsupportedEncodingException
  {
    String query = "";
    Iterator<String> keys = json.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      String value = json.getString(key);
      if (!key.isEmpty() && !value.isEmpty()) {
        if (query.isEmpty()) {
          query += "?";
        }
        else {
          query += "&";
        }
        query += URLEncoder.encode(key, "UTF-8")+"="+URLEncoder.encode(value, "UTF-8");
      }
    }
    return query;
  }
}
