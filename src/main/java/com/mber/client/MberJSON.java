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
import java.io.PrintWriter;
import java.io.StringWriter;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class MberJSON
{
  public static String getString(final JSONObject json, final String key)
  {
    if (json.has(key)) {
      return json.getString(key);
    }
    return "";
  }

  public static boolean getBooleanOrFalse(final JSONObject json, final String key)
  {
    if (json.has(key)) {
      return json.getBoolean(key);
    }
    return false;
  }

  public static JSONObject toObject(String json)
  {
    try {
      int offset = json.indexOf("{");
      if (offset > 0) {
        json = json.substring(offset);
      }
      return (JSONObject)JSONSerializer.toJSON(json);
    }
    catch (JSONException e) {
      return new JSONObject();
    }
  }

  public static JSONObject getObject(final JSONObject json, final String key)
  {
    if (json.has(key)) {
      return json.getJSONObject(key);
    }
    return new JSONObject();
  }

  public static JSONArray getArray(final JSONObject json, final String key)
  {
    if (json.has(key)) {
      return json.getJSONArray(key);
    }
    return new JSONArray();
  }

  public static JSONObject success()
  {
    JSONObject json = new JSONObject();
    json.put("status", "Success");
    return json;
  }

  public static JSONObject failed(final String error)
  {
    JSONObject json = new JSONObject();
    json.put("status", "Failed");
    json.put("error", error);
    return json;
  }

  public static JSONObject failed(final JSONArray errors)
  {
    return failed(errors.join("\n"));
  }

  public static JSONObject failed(final Exception e)
  {
    StringWriter writer = new StringWriter();
    PrintWriter printer = new PrintWriter(writer);
    printer.println(e.getMessage());
    e.printStackTrace(printer);
    return failed(writer.toString());
  }
}
