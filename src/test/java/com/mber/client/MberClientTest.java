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
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.Date;
import java.util.Stack;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ErrorCollector;
import org.junit.Test;
import wiremock.org.skyscreamer.jsonassert.JSONCompareMode;

public class MberClientTest extends MberTest
{
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(8089);

  @Rule
  public ErrorCollector collector = new ErrorCollector();

  @Before
  public void setupWireMock()
  {
    JSONObject request = null;
    JSONObject response = null;

    // Provide a generic NotAuthorized catch all for OAuth.
    response = new JSONObject();
    response.put("status", "NotAuthorized");
    response.put("error", "invalid_grant");

    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/service/json/oauth/accesstoken/"))
      .atPriority(5)
      .willReturn(WireMock.aResponse()
        .withStatus(479)
        .withBody(response.toString())
      )
    );

    // Provide an explicit Success when requesting a token with the correct password.
    request = new JSONObject();
    request.put("username", getMberUsername());
    request.put("password", getMberPassword());
    request.put("grant_type", "password");
    request.put("client_id", getMberApplicationId());

    response = new JSONObject();
    response.put("status", "Success");
    response.put("access_token", "MOCKACCESSTOKEN");

    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/service/json/oauth/accesstoken/"))
      .withRequestBody(WireMock.equalToJson(request.toString(), JSONCompareMode.LENIENT))
      .atPriority(1)
      .willReturn(WireMock.aResponse()
        .withStatus(200)
        .withBody(response.toString())
      )
    );

    // Provide a generic Failed catch all for directory creation.
    response = new JSONObject();
    response.put("status", "Failed");
    response.put("message", "Found 0 bytes");

    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/service/json/data/directory/"))
      .atPriority(5)
      .willReturn(WireMock.aResponse()
        .withStatus(406)
        .withBody(response.toString())
      )
    );

    // Provide an explicit Success for directory creation when the mock token is used.
    request = new JSONObject();
    request.put("access_token", "MOCKACCESSTOKEN");

    response = new JSONObject();
    response.put("status", "Success");
    response.put("directoryId", "MOCKDIRECTORYID_AAAAAA");

    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/service/json/data/directory/"))
      .withRequestBody(WireMock.equalToJson(request.toString(), JSONCompareMode.LENIENT))
      .withRequestBody(WireMock.containing("\"alias\":\"jenkins"))
      .atPriority(1)
      .willReturn(WireMock.aResponse()
        .withStatus(200)
        .withBody(response.toString())
      )
    );

    // Provide an explicit Success for directory read when the mock token and directory are used.
    response = new JSONObject();
    response.put("status", "Success");
    response.put("directoryId", "MOCKDIRECTORYID_AAAAAA");

    WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/service/json/data/directory/MOCKDIRECTORYID_AAAAAA/?access_token=MOCKACCESSTOKEN"))
      .atPriority(1)
      .willReturn(WireMock.aResponse()
        .withStatus(200)
        .withBody(response.toString())
      )
    );

    // Provide a generic Failed catch all for project creation.
    response = new JSONObject();
    response.put("status", "Failed");
    response.put("message", "Found 0 bytes");

    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/service/json/build/project/"))
      .atPriority(5)
      .willReturn(WireMock.aResponse()
        .withStatus(406)
        .withBody(response.toString())
      )
    );

    // Provide an explicit Success for project creation when the mock token is used.
    request = new JSONObject();
    request.put("access_token", "MOCKACCESSTOKEN");

    response = new JSONObject();
    response.put("status", "Success");
    response.put("projectId", "MOCKPROJECTID");

    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/service/json/build/project/"))
      .withRequestBody(WireMock.equalToJson(request.toString(), JSONCompareMode.LENIENT))
      .atPriority(1)
      .willReturn(WireMock.aResponse()
        .withStatus(200)
        .withBody(response.toString())
      )
    );

    // Provide a generic Failed catch all for build creation.
    response = new JSONObject();
    response.put("status", "Failed");
    response.put("message", "Found 0 bytes");

    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/service/json/build/build/"))
      .atPriority(5)
      .willReturn(WireMock.aResponse()
        .withStatus(406)
        .withBody(response.toString())
      )
    );

    // Provide an explicit Success for build creation when the mock token and project are used.
    request = new JSONObject();
    request.put("access_token", "MOCKACCESSTOKEN");
    request.put("projectId", "MOCKPROJECTID");

    response = new JSONObject();
    response.put("status", "Success");
    response.put("buildId", "MOCKBUILDID");

    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/service/json/build/build/"))
      .withRequestBody(WireMock.equalToJson(request.toString(), JSONCompareMode.LENIENT))
      .atPriority(1)
      .willReturn(WireMock.aResponse()
        .withStatus(200)
        .withBody(response.toString())
      )
    );

    // Provide a generic Failed catch all for build update.
    response = new JSONObject();
    response.put("status", "Failed");
    response.put("message", "Found 0 bytes");

    WireMock.stubFor(WireMock.put(WireMock.urlMatching("/service/json/build/build/*"))
      .atPriority(5)
      .willReturn(WireMock.aResponse()
        .withStatus(406)
        .withBody(response.toString())
      )
    );

    // Provide an explicit Success for build creation when the mock token and build are used.
    request = new JSONObject();
    request.put("access_token", "MOCKACCESSTOKEN");

    response = new JSONObject();
    response.put("status", "Success");

    WireMock.stubFor(WireMock.put(WireMock.urlEqualTo("/service/json/build/build/MOCKBUILDID/"))
      .withRequestBody(WireMock.equalToJson(request.toString(), JSONCompareMode.LENIENT))
      .atPriority(1)
      .willReturn(WireMock.aResponse()
        .withStatus(200)
        .withBody(response.toString())
      )
    );

    // Provide a generic Failed catch all for data upload.
    response = new JSONObject();
    response.put("status", "Failed");
    response.put("message", "Uploaded 0 bytes");

    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/service/json/data/upload/"))
      .atPriority(5)
      .willReturn(WireMock.aResponse()
        .withStatus(406)
        .withBody(response.toString())
      )
    );

    // Provide an explicit NotFound for data upload when an invalid directory ID is used.
    request = new JSONObject();
    request.put("access_token", "MOCKACCESSTOKEN");
    request.put("directoryId", "AAAAAAAAAAAAAAAAAAAAAA");

    response = new JSONObject();
    response.put("status", "NotFound");
    response.put("message", "Uploaded 0 bytes");

    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/service/json/data/upload/"))
      .withRequestBody(WireMock.equalToJson(request.toString(), JSONCompareMode.LENIENT))
      .atPriority(1)
      .willReturn(WireMock.aResponse()
        .withStatus(404)
        .withBody(response.toString())
      )
    );

    // Provide an explicit Success for data upload when the mock token and directory are used.
    request = new JSONObject();
    request.put("access_token", "MOCKACCESSTOKEN");
    request.put("directoryId", "MOCKDIRECTORYID_AAAAAA");

    response = new JSONObject();
    response.put("status", "Success");
    response.put("url", getMberUrl() + "/uploads/");

    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/service/json/data/upload/"))
      .inScenario("Uploads")
      .whenScenarioStateIs(Scenario.STARTED)
      .withRequestBody(WireMock.equalToJson(request.toString(), JSONCompareMode.LENIENT))
      .atPriority(1)
      .willReturn(WireMock.aResponse()
        .withStatus(200)
        .withBody(response.toString())
      ).willSetStateTo("Duplicate")
    );

    response = new JSONObject();
    response.put("status", "Duplicate");
    response.put("url", getMberUrl() + "/uploads/");

    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/service/json/data/upload/"))
      .inScenario("Uploads")
      .whenScenarioStateIs("Duplicate")
      .withRequestBody(WireMock.equalToJson(request.toString(), JSONCompareMode.LENIENT))
      .atPriority(1)
      .willReturn(WireMock.aResponse()
        .withStatus(200)
        .withBody(response.toString())
      )
    );

    // Provide an explicit success for the upload itself.
    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/uploads/"))
      .atPriority(1)
      .willReturn(WireMock.aResponse()
        .withStatus(200)
      )
    );

    // Provide a generic Failed catch all for document creation.
    response = new JSONObject();
    response.put("status", "Failed");
    response.put("message", "Found 0 bytes");

    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/service/json/data/document/"))
      .atPriority(5)
      .willReturn(WireMock.aResponse()
        .withStatus(406)
        .withBody(response.toString())
      )
    );

    // Provide an explicit Success for document creation when the mock token is used.
    request = new JSONObject();
    request.put("access_token", "MOCKACCESSTOKEN");

    response = new JSONObject();
    response.put("status", "Success");
    response.put("documentId", "MOCKDOCUMENTID");

    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/service/json/data/document/"))
      .withRequestBody(WireMock.equalToJson(request.toString(), JSONCompareMode.LENIENT))
      .atPriority(1)
      .willReturn(WireMock.aResponse()
        .withStatus(200)
        .withBody(response.toString())
      )
    );

    // Provide a generic Failed catch all for app event creation.
    response = new JSONObject();
    response.put("status", "Failed");
    response.put("message", "Failed to create an app event");

    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/service/json/eventstream/appevent/"))
      .atPriority(5)
      .willReturn(WireMock.aResponse()
        .withStatus(406)
        .withBody(response.toString())
      )
    );

    // Provide an explicit Success for app event creation when the mock token is used.
    request = new JSONObject();
    request.put("access_token", "MOCKACCESSTOKEN");

    response = new JSONObject();
    response.put("status", "Success");

    WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/service/json/eventstream/appevent/"))
      .withRequestBody(WireMock.equalToJson(request.toString(), JSONCompareMode.LENIENT))
      .atPriority(1)
      .willReturn(WireMock.aResponse()
        .withStatus(200)
        .withBody(response.toString())
      )
    );

    // Provide explicit counts for app events.
    JSONArray counts = new JSONArray();
    counts.add(1);
    counts.add(0);
    counts.add(2);
    counts.add(3);
    response = new JSONObject();
    response.put("results", counts);
    response.put("status", "Success");

    WireMock.stubFor(WireMock.get(WireMock.urlMatching("/service/json/metrics/countovertime.*"))
      .atPriority(1)
      .willReturn(WireMock.aResponse()
        .withStatus(200)
        .withBody(response.toString())
      )
    );
  }

  @Test
  public void decodesApplicationIDs() throws Exception
  {
    // If we have an alias without a quote, insert the quote.
    MberClient aliasWithoutQuote = new MberClient("", "IsThisAnAlias?");
    Assert.assertEquals("Failed to quote application alias", "'IsThisAnAlias?", aliasWithoutQuote.getApplication());

    // If we have an alias with a quote, keep the quote.
    MberClient aliasWithQuote = new MberClient("", "'AlsoAnAlias");
    Assert.assertEquals("Failed to keep the quote on application alias", "'AlsoAnAlias", aliasWithQuote.getApplication());

    // If we have a UUID, don't change it.
    MberClient uuid = new MberClient("", "ABCDEFGHIJKLMNOPQRSTUV");
    Assert.assertEquals("Failed to keep the UUID", "ABCDEFGHIJKLMNOPQRSTUV", uuid.getApplication());

    // If we have something that's kind of like an UUID, treat it as an alias.
    MberClient almostUuid = new MberClient("", "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    Assert.assertEquals("Failed to quote the application alias", "'ABCDEFGHIJKLMNOPQRSTUVWXYZ", almostUuid.getApplication());
  }

  @Test
  public void resolvesUrls() throws Exception
  {
    // Append a slash to the path if there's not already one there.
    String pathWithoutSlash = MberClient.baseUrlWithPath("http://this.is.mber", "noodles");
    Assert.assertEquals("Didn't add trailing slash to URL", "http://this.is.mber/noodles/", pathWithoutSlash);

    // Don't add an extra slash on the path if it already has one.
    String pathWithSlash = MberClient.baseUrlWithPath("http://this.is.mber/", "noodles/");
    Assert.assertEquals("Added extra slash to URL", "http://this.is.mber/noodles/", pathWithSlash);

    // Removes double slashes if the URL ends with one and the parth starts with one.
    String doubleSlashUrl = MberClient.baseUrlWithPath("http://this.is.mber/", "/noodles");
    Assert.assertEquals("Put double slash in URL", "http://this.is.mber/noodles/", doubleSlashUrl);

    // Removes query args from the URL.
    String urlWithQuery = MberClient.baseUrlWithPath("http://this.is.mber/?a=b", "noodles");
    Assert.assertEquals("Didn't remove query args from URL", "http://this.is.mber/noodles/", urlWithQuery);

    // Preserves the port number in the URL.
    String urlWithPort = MberClient.baseUrlWithPath("http://this.is.mber:80", "noodles");
    Assert.assertEquals("Didn't keep the port in the URL", "http://this.is.mber:80/noodles/", urlWithPort);
  }

  @Test
  public void logsIntoMber() throws Exception
  {
    checkMberVariables();

    // Valid logins should succeed.
    MberClient mber = new MberClient(getMberUrl(), getMberApplicationId());
    JSONObject success = mber.login(getMberUsername(), getMberPassword());
    Assert.assertEquals("Failed to log into Mber", "Success", success.getString("status"));

    // Invalid logins should fail with an error message.
    JSONObject fail = mber.login("noodles", "noodles");
    Assert.assertEquals("Logged into Mber unexpectedly", "NotAuthorized", fail.getString("status"));
    assertNotEmpty("No error message found", fail.getString("error"));

    // Login calls should be recorded.
    Assert.assertEquals("Login calls where not recorded", 2, mber.getCallHistory().size());
  }

  @Test
  public void createsFolders() throws Exception
  {
    checkMberVariables();
    MberClient mber = new MberClient(getMberUrl(), getMberApplicationId());

    Stack<JSONObject> results = new Stack<JSONObject>();

    try {
      // Fails with an error message unless logged in.
      results.push(mber.mkpath("jenkins-mber-plugin/test/create"));
      Assert.assertEquals("Created folder unexpectedly", "Failed", results.peek().getString("status"));
      assertNotEmpty("No error message found", results.peek().getString("error"));

      // There should be two calls.
      // * 1 to check the alias
      // * 1 to create the folder
      Assert.assertEquals("Failed directory create calls where not recorded", 2, mber.getCallHistory().size());

      // Succeeds with a directory ID when logged in.
      results.push(mber.login(getMberUsername(), getMberPassword()));
      results.push(mber.mkpath("jenkins-mber-plugin/test/create"));
      Assert.assertEquals("Failed to create folder", "Success", results.peek().getString("status"));
      assertNotEmpty("No directory ID found", results.peek().getString("directoryId"));

      // There should be nine calls.
      // * 2 from the previous failed attempt
      // * 1 to get the access token
      // * 3 to check the aliases
      // * 3 to create the folders
      Assert.assertEquals("Successful directory create calls where not recorded", 9, mber.getCallHistory().size());
    }
    finally {
      mberCleanup(results, mber.getURL(), "data/directory", "directoryId");
    }
  }

  @Test
  public void createsProjectsAndBuilds() throws Exception
  {
    checkMberVariables();
    MberClient mber = new MberClient(getMberUrl(), getMberApplicationId());

    Stack<JSONObject> results = new Stack<JSONObject>();

    try {
      // Fails with an error message unless logged in.
      results.push(mber.mkproject("noodles", null));
      Assert.assertEquals("Accidentally created a Project", "Failed", results.peek().getString("status"));
      assertNotEmpty("No error message found when creating a Project", results.peek().getString("error"));

      // Fails to create a build if we don't have a project.
      results.push(mber.mkbuild("noodles", null, MberClient.generateTransactionId()));
      Assert.assertEquals("Accidentally created a Build without a Project", "Failed", results.peek().getString("status"));
      assertNotEmpty("No error message found when creating a Build without a Project", results.peek().getString("error"));

      // Returns a project ID even if we don't have a description.
      results.push(mber.login(getMberUsername(), getMberPassword()));
      results.push(mber.mkproject("noodles", ""));
      Assert.assertEquals("Failed to create a Project without a description", "Success", results.peek().getString("status"));
      assertNotEmpty("No project ID found when creating a Project without a description", results.peek().getString("projectId"));

      // Returns a project ID if we have a description.
      results.push(mber.mkproject("noodles", "are delicious"));
      Assert.assertEquals("Failed to create a Project with a description", "Success", results.peek().getString("status"));
      assertNotEmpty("No project ID found found when creating a Project with a description", results.peek().getString("projectId"));

      // Fails to update a build if we don't have a build.
      results.push(mber.updateBuild("name", "description", BuildStatus.RUNNING));
      Assert.assertEquals("Accidentally set a Build status when updating a Build that doesn't exist", "Failed", results.peek().getString("status"));
      assertNotEmpty("No error message found when updating a Build that doesn't exist", results.peek().getString("error"));

      // Fails to set a build directory if we don't have a build.
      results.push(mber.setBuildDirectory("AAAAAAAAAAAAAAAAAAAAAA"));
      Assert.assertEquals("Accidentally set a Build directory when updating a Build that doesn't exist", "Failed", results.peek().getString("status"));
      assertNotEmpty("No error message found when setting a directory on a Build that doesn't exist", results.peek().getString("error"));

      // Returns a build ID even if we don't have a description.
      results.push(mber.mkbuild("pasta", "", MberClient.generateTransactionId()));
      Assert.assertEquals("Failed to create a Build without a description", "Success", results.peek().getString("status"));
      assertNotEmpty("No build ID found when creating a Build without a description", results.peek().getString("buildId"));

      // Returns a build ID if we have a description.
      results.push(mber.mkbuild("pasta", "is delicious", MberClient.generateTransactionId(), BuildStatus.RUNNING));
      Assert.assertEquals("Failed to create a Build with a description", "Success", results.peek().getString("status"));
      assertNotEmpty("No build ID found when creating a Build with a description", results.peek().getString("buildId"));

      // Lets us update a build if we do have a build.
      results.push(mber.updateBuild("pasta", "is delicious", BuildStatus.COMPLETED, BuildStatus.FAILURE));
      Assert.assertEquals("Failed to set a Build status when updating a Build", "Success", results.peek().getString("status"));

      // Lets us set a build directory if we have a build.
      results.push(mber.mkpath("jenkins-mber-plugin/test/upload"));
      results.push(mber.setBuildDirectory(results.peek().getString("directoryId")));
      Assert.assertEquals("Failed to set a Build directory when updating a Build\n"+results.peek().toString(), "Success", results.peek().getString("status"));
    }
    finally {
      mberCleanup(results, mber.getURL(), "data/directory", "directoryId");
      mberCleanup(results, mber.getURL(), "build/project", "projectId");
    }
  }

  @Test
  public void uploadsFiles() throws Exception
  {
    checkMberVariables();
    MberClient mber = new MberClient(getMberUrl(), getMberApplicationId());

    Stack<JSONObject> results = new Stack<JSONObject>();

    try {
      File temp = File.createTempFile("jenkins-mber-plugin", "txt");
      BufferedWriter io = new BufferedWriter(new FileWriter(temp));
      io.write("This is an upload test for the Jenkins Mber Plugin.");
      io.close();

      String[] tags = { "test" };

      // Fails with an error message unless logged in.
      results.push(mber.upload(temp.getPath(), "jenkins-mber-plugin/test/upload", temp.getName(), tags, false));
      Assert.assertEquals("Uploaded file unexpectedly while not logged in", "Failed", results.peek().getString("status"));
      assertNotEmpty("No error message found when uploading a file while not logged in", results.peek().getString("error"));

      // Fails with an error message if the directory ID's invalid.
      results.push(mber.login(getMberUsername(), getMberPassword()));
      results.push(mber.upload(temp.getPath(), "AAAAAAAAAAAAAAAAAAAAAA", temp.getName(), tags, false));
      Assert.assertEquals("Uploaded file unexpectedly with an invalid directory", "NotFound", results.peek().getString("status"));
      assertNotEmpty("No error message found when uploading a file with an invalid directory", results.peek().getString("error"));

      // Succeeds when logged in and the directory ID's valid.
      results.push(mber.mkpath("jenkins-mber-plugin/test/upload"));
      String uploadFolder = results.peek().getString("directoryId");
      results.push(mber.upload(temp.getPath(), uploadFolder, temp.getName(), tags, false));
      Assert.assertEquals("Failed to upload file while logged in with a valid directory", "Success", results.peek().getString("status"));

      // Fails with an error message if the file already exists.
      results.push(mber.upload(temp.getPath(), uploadFolder, temp.getName(), tags, false));
      Assert.assertEquals("Updated file unexpectedly", "Duplicate", results.peek().getString("status"));
      assertNotEmpty("No error message found when uploading a file that already exists", results.peek().getString("error"));

      // Succeeds if the file already exists and is being overwritten.
      results.push(mber.upload(temp.getPath(), uploadFolder, temp.getName(), tags, true));
      Assert.assertEquals("Failed to update file when uploading a file that already exists", "Success", results.peek().getString("status"));

      // Uploads empty JSON as a raw document.
      results.push(mber.mkpath("jenkins-mber-plugin/test/upload"));
      results.push(mber.upload(new JSONObject(), results.peek().getString("directoryId"), "no-tests.json", tags));
      Assert.assertEquals("Failed to upload empty JSON", "Success", results.peek().getString("status"));

      // Uploads non-empty JSON as a raw document.
      JSONObject tests = new JSONObject();
      tests.put("some test", "data");
      results.push(mber.mkpath("jenkins-mber-plugin/test/upload"));
      results.push(mber.upload(tests, results.peek().getString("directoryId"), "some-tests.json", tags));
      Assert.assertEquals("Failed to upload non-empty JSON", "Success", results.peek().getString("status"));
    }
    finally {
      mberCleanup(results, mber.getURL(), "data/directory", "directoryId");
    }
  }

  @Test
  public void publishesTestResults() throws Exception
  {
    checkMberVariables();

    final MberClient mber = new MberClient(getMberUrl(), getMberApplicationId());
    final Date startTime = new Date();

    final Stack<JSONObject> results = new Stack<JSONObject>();
    try {
      // Create a build with an ID so we can push test data in.
      results.push(mber.login(getMberUsername(), getMberPassword()));
      results.push(mber.mkproject("test.socketio", ""));
      results.push(mber.mkbuild("test.socketio", "", MberClient.generateTransactionId()));
      String buildId = results.peek().getString("buildId");
      assertNotEmpty("Didn't get a build ID from Mber", buildId);

      // Push test results into Mber.
      JSONObject testResults = new JSONObject();
      testResults.put("failCount", 1);
      testResults.put("skipCount", 0);
      testResults.put("passCount", 2);
      testResults.put("totalCount", 3);
      results.push(mber.publishTestResults(testResults));
      assertSuccess("Failed to publish test results to Mber", results.peek());

      // Back up a few seconds to account for clock drift.
      final Date searchTime = new Date();
      searchTime.setTime(startTime.getTime() + (-60 * 1000));

      // Make sure we get those test results out as historical events for the build.
      assertSpin("Didn't get tests.failCount results back", new Callable<Boolean>() {
        public Boolean call() {
          JSONObject result = mber.getBuildCountSince("failCount", searchTime);
          return !MberJSON.getArray(result, "results").isEmpty();
        }
      });
      assertSpin("Didn't get tests.skipCount results back", new Callable<Boolean>() {
        public Boolean call() {
          JSONObject result = mber.getBuildCountSince("skipCount", searchTime);
          return !MberJSON.getArray(result, "results").isEmpty();
        }
      });
      assertSpin("Didn't get tests.passCount results back", new Callable<Boolean>() {
        public Boolean call() {
          JSONObject result = mber.getBuildCountSince("passCount", searchTime);
          return !MberJSON.getArray(result, "results").isEmpty();
        }
      });
      assertSpin("Didn't get tests.totalCount results back", new Callable<Boolean>() {
        public Boolean call() {
          JSONObject result = mber.getBuildCountSince("totalCount", searchTime);
          return !MberJSON.getArray(result, "results").isEmpty();
        }
      });
    }
    finally {
      mberCleanup(results, mber.getURL(), "build/project", "projectId");
    }
  }

  private void mberCleanup(final Stack<JSONObject> results, final String url, final String type, final String key)
  {
    String accessToken = null;
    ArrayList<String> uuids = new ArrayList<String>();

    for (JSONObject json : results) {
      if (json.has("access_token")) {
        accessToken = json.getString("access_token");
      }
      if (json.has(key)) {
        uuids.add(json.getString(key));
      }
    }

    for (String uuid : uuids) {
      mberDelete(url, accessToken, type, uuid);
    }
  }

  private void mberDelete(String url, final String accessToken, final String type, final String uuid)
  {
    if (!accessToken.equals("MOCKACCESSTOKEN")) {
      try {
        JSONObject data = new JSONObject();
        data.put("access_token", accessToken);
        data.put("transactionId", MberClient.generateTransactionId());

        url = MberClient.baseUrlWithPath(url, "service/json/"+type+"/"+uuid);
        String mberResponse = HTTParty.delete(url, data).body;

        JSONObject json = new JSONObject();
        try {
          json = (JSONObject)JSONSerializer.toJSON(mberResponse);
        }
        catch (JSONException e) {
          Assert.fail(mberResponse);
        }

        if (!json.has("status")) {
          Assert.fail("Failed to remove "+type+" "+uuid);
        }
        String status = json.getString("status");
        if (!status.equals("Success") && !status.equals("NotFound")) {
          Assert.fail("Failed to remove "+type+" "+uuid);
        }
      }
      catch (Throwable e) {
        collector.addError(e);
      }
    }
  }
}
