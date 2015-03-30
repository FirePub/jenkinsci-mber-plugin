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
import com.mber.client.MberClient;
import com.mber.client.BuildStatus;
import com.mber.client.HTTParty;
import com.mber.client.MberJSON;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

public class MberNotifier extends Notifier
{
  private final String application;
  private final String username;
  private final Secret password;
  private final String buildName;
  private final String buildDescription;
  private final boolean uploadConsoleLog;
  private final boolean uploadTestResults;
  private final boolean uploadArtifacts;
  private final boolean overwriteExistingFiles;
  private String buildArtifacts;
  private String artifactFolder;
  private String artifactTags;
  private JSONObject mberConfig;
  private Map<String, List<HTTParty.Call>> callHistory;

  @DataBoundConstructor
  public MberNotifier(String application, String username, String password, String buildName, String buildDescription, boolean uploadTestResults, boolean uploadConsoleLog, UploadArtifactsBlock uploadArtifacts)
  {
    this.application = application;
    this.username = username;
    this.password = Secret.fromString(password);
    this.buildName = buildName;
    this.buildDescription = buildDescription;
    this.uploadConsoleLog = uploadConsoleLog;
    this.uploadTestResults = uploadTestResults;
    this.uploadArtifacts = (uploadArtifacts != null);
    if (uploadArtifacts != null) {
      this.buildArtifacts = uploadArtifacts.getBuildArtifacts();
      this.artifactFolder = uploadArtifacts.getArtifactFolder();
      this.artifactTags = uploadArtifacts.getArtifactTags();
      this.overwriteExistingFiles = uploadArtifacts.isOverwriteExistingFiles();
    } else {
      this.overwriteExistingFiles = false;
    }
  }

  public String getApplication()
  {
    return application;
  }

  public String getUsername()
  {
    return username;
  }

  public String getPassword()
  {
    return password.getEncryptedValue();
  }

  private String getDecryptedPassword()
  {
    return password.getPlainText();
  }

  public String getBuildName()
  {
    return buildName;
  }

  private String getMberBuildName(final AbstractBuild build, final BuildListener listener)
  {
    if (buildName != null && !buildName.isEmpty()) {
      return this.resolveEnvironmentVariables(build, listener, buildName);
    }
    return build.getDisplayName();
  }

  public String getBuildDescription()
  {
    return buildDescription;
  }

  private String getMberBuildDescription(final AbstractBuild build, final BuildListener listener)
  {
    if (buildDescription != null && !buildDescription.isEmpty()) {
      return this.resolveEnvironmentVariables(build, listener, buildDescription);
    }
    return build.getDescription();
  }

  public boolean isUploadConsoleLog()
  {
    return uploadConsoleLog;
  }

  public boolean isUploadTestResults()
  {
    return uploadTestResults;
  }

  public boolean isUploadArtifacts()
  {
    return uploadArtifacts;
  }

  public boolean isOverwriteExistingFiles()
  {
    return overwriteExistingFiles;
  }

  public String getBuildArtifacts()
  {
    return buildArtifacts;
  }

  public String getArtifactFolder()
  {
    if (artifactFolder == null || artifactFolder.isEmpty()) {
      return getDescriptor().getDefaultArtifactFolder();
    }
    return artifactFolder;
  }

  public String getArtifactTags()
  {
    if (artifactTags == null || artifactTags.isEmpty()) {
      return getDescriptor().getDefaultArtifactTags();
    }
    return artifactTags;
  }

  private void recordCallHistory(final AbstractBuild build, final MberClient mber) {
    if (this.callHistory == null) {
      this.callHistory = new HashMap();
    }
    String buildId = getCallHistoryId(build);
    List<HTTParty.Call> calls = getCallHistory(build);
    calls.addAll(mber.getCallHistory());
    this.callHistory.put(buildId, calls);
  }

  List<HTTParty.Call> getCallHistory(final AbstractBuild build) {
    String buildId = getCallHistoryId(build);
    if (this.callHistory != null && this.callHistory.containsKey(buildId)) {
      return this.callHistory.get(buildId);
    }
    return new ArrayList();
  }

  private void clearCallHistory(final AbstractBuild build) {
    if (this.callHistory != null) {
      String buildId = getCallHistoryId(build);
      this.callHistory.remove(buildId);
    }
  }

  private String getCallHistoryId(final AbstractBuild build) {
    // Use the URL to the build as a unique ID, since it's guaranteed to be unique,
    // unlike the getId() function which just returns the time the build started.
    return build.getUrl();
  }

  private String[] getUploadTags(final AbstractBuild build, final BuildListener listener, final FilePath file)
  {
    return getUploadTags(build, listener, file.getName());
  }

  private String[] getUploadTags(final AbstractBuild build, final BuildListener listener, final String name)
  {
    ArrayList<String> tags = new ArrayList<String>();
    tags.add(name);

    String[] userTags = getArtifactTags().split("\\s+");
    for (String tag : userTags) {
      String resolvedTag = this.resolveEnvironmentVariables(build, listener, tag);
      if (resolvedTag != null && !resolvedTag.isEmpty()) {
        tags.add(resolvedTag);
      }
    }

    return tags.toArray(new String[tags.size()]);
  }

  public void uploadLogFile(AbstractBuild build, BuildListener listener, final MberClient mber)
  {
    if (!isUploadConsoleLog()) {
      return;
    }

    File logFile = build.getLogFile();
    if (logFile == null || !logFile.exists() || logFile.length() <= 0) {
      return;
    }

    log(listener, "Uploading console output to Mber");

    JSONObject response = makeArtifactFolder(build, listener, mber, false);
    if (!response.getString("status").equals("Success")) {
      log(listener, response.getString("error"));
      return;
    }

    String uploadDirectoryId = response.getString("directoryId");
    String[] tags = getUploadTags(build, listener, "console.log");

    response = mber.upload(new FilePath(logFile), uploadDirectoryId, "console.log", tags, false);
    if (response.getString("status").equals("Duplicate")) {
      log(listener, "You already have a build artifact named \"console.log\". Please rename your build artifact.");
      return;
    }
    if (!response.getString("status").equals("Success")) {
      log(listener, response.getString("error"));
    }
  }

  private void writeCallHistory(final AbstractBuild build, final BuildListener listener, final MberClient mber)
  {
    // Only write debug information if the build failed.
    if (build.getResult().equals(Result.FAILURE)) {
      // Aggregate the call history for both prebuild and perform.
      recordCallHistory(build, mber);
      log(listener, "The following calls were made to Mber:");
      for (HTTParty.Call call : getCallHistory(build)) {
        // Don't show query strings as part of the URI. They can contain sensitive data like tokens.
        String url = call.uri.toString();
        int offset = url.indexOf('?');
        if (offset >= 0) {
          url = url.substring(0, offset);
        }
        log(listener, call.method+" "+url+" - "+call.code);
      }
    }
    // Clear the call history for this build so memory usage doesn't keep growing.
    clearCallHistory(build);
  }

  private JSONObject makeArtifactFolder(AbstractBuild build, BuildListener listener, final MberClient mber, boolean logging)
  {
    String resolvedArtifactFolder = resolveArtifactFolder(build, listener);
    if (resolvedArtifactFolder == null || resolvedArtifactFolder.isEmpty()) {
      return MberJSON.failed("Couldn't resolve environment variables in artifact folder "+getArtifactFolder());
    }

    if (logging) {
      log(listener, "Creating artifact folder "+resolvedArtifactFolder);
    }

    JSONObject response = mber.mkpath(resolvedArtifactFolder);
    if (!response.getString("status").equals("Success")) {
      return response;
    }

    String uploadDirectoryId = response.getString("directoryId");
    response = mber.setBuildDirectory(uploadDirectoryId);
    if (!response.getString("status").equals("Success")) {
      return response;
    }

    JSONObject success = MberJSON.success();
    success.put("directoryId", uploadDirectoryId);
    return success;
  }

  private String resolveArtifactFolder(AbstractBuild build, BuildListener listener)
  {
    return resolveEnvironmentVariables(build, listener, getArtifactFolder());
  }

  private FilePath[] findBuildArtifacts(final AbstractBuild build, final BuildListener listener)
  {
    String artifactGlob = resolveEnvironmentVariables(build, listener, getBuildArtifacts());
    if (artifactGlob == null || artifactGlob.isEmpty()) {
      return new FilePath[0];
    }

    try {
      ArrayList<FilePath> artifacts = new ArrayList<FilePath>();
      String[] globs = artifactGlob.split("\\s+");
      for (String glob : globs) {
        FilePath[] files = build.getWorkspace().list(glob);
        artifacts.addAll(Arrays.asList(files));
      }
      return artifacts.toArray(new FilePath[0]);
    }
    catch (Exception e) {
      return new FilePath[0];
    }
  }

  private Map<FilePath, String> findBuildArtifactFolders(final AbstractBuild build, final BuildListener listener, final FilePath[] artifacts)
  {
    // Artifact paths are relative to the workspace, and keep their folder structure when uploaded to Mber.
    // Since the slave and master might be running on different OSes, we normalize the folder name to slashes.
    File base = new File(resolveArtifactFolder(build, listener));
    String workspace = build.getWorkspace().getRemote();
    HashMap namedArtifacts = new HashMap();
    for (FilePath path : artifacts) {
      if (path != null) {
        String name = path.getRemote().replace(workspace, "");
        name = (new File(base, name)).getPath();
        name = name.replace("\\", "/");
        name = name.substring(0, name.lastIndexOf("/"));
        namedArtifacts.put(path, name);
      }
    }
    return namedArtifacts;
  }

  private String resolveEnvironmentVariables(final AbstractBuild build, final BuildListener listener, final String value)
  {
    try {
      return build.getEnvironment(listener).expand(value);
    }
    catch (Exception e) {
      return null;
    }
  }

  private MberClient makeMberClient()
  {
    if (mberConfig == null) {
      return new MberClient(getDescriptor().getMberUrl(), getApplication());
    }
    return new MberClient(this.mberConfig);
  }

  private void log(final BuildListener listener, final String message)
  {
    listener.getLogger().println(message);
  }

  private boolean fail(final AbstractBuild build, final BuildListener listener, final MberClient mber, final String message)
  {
    log(listener, message);
    build.setResult(Result.FAILURE);
    return done(build, listener, mber);
  }

  private boolean isFailedBuild(final AbstractBuild build)
  {
    return !build.getResult().equals(Result.SUCCESS);
  }

  private boolean done(final AbstractBuild build, final BuildListener listener, final MberClient mber)
  {
    JSONObject result;
    // Refetch the build name and description, since users might have bound them to environment variables.
    String mberBuildName = getMberBuildName(build, listener);
    String mberBuildDescription = getMberBuildDescription(build, listener);
    if (isFailedBuild(build)) {
      log(listener, "Setting Mber build status to "+BuildStatus.COMPLETED.toString()+" "+BuildStatus.FAILURE.toString());
      result = mber.updateBuild(mberBuildName, mberBuildDescription, BuildStatus.COMPLETED, BuildStatus.FAILURE);
    }
    else {
      log(listener, "Setting Mber build status to "+BuildStatus.COMPLETED.toString()+" "+BuildStatus.SUCCESS.toString());
      result = mber.updateBuild(mberBuildName, mberBuildDescription, BuildStatus.COMPLETED, BuildStatus.SUCCESS);
    }
    if (!result.getString("status").equals("Success")) {
      // Don't call fail() here, otherwise we end up in a retry loop if we can't connect to Mber.
      log(listener, result.getString("error"));
      build.setResult(Result.FAILURE);
    }
    uploadTestEvents(build, listener, mber);
    uploadLogFile(build, listener, mber);
    writeCallHistory(build, listener, mber);
    return true;
  }

  private JSONObject downloadTestResults(AbstractBuild build, AbstractTestResultAction action)
  {
    try {
      String url = build.getAbsoluteUrl() + action.getUrlName() + "/api/json";
      String response = HTTParty.get(url).body;
      return (JSONObject)JSONSerializer.toJSON(response);
    }
    catch (IOException e) {
      return new JSONObject();
    }
  }

  private void uploadTestEvents(AbstractBuild build, BuildListener listener, final MberClient mber)
  {
    if (!isUploadTestResults()) {
      return;
    }

    AbstractTestResultAction testResultAction = build.getTestResultAction();
    if (testResultAction == null) {
      return;
    }

    log(listener, "Getting test results from Jenkins");

    JSONObject testResults = downloadTestResults(build, testResultAction);

    log(listener, "Uploading test results to Mber");

    JSONObject response = makeArtifactFolder(build, listener, mber, false);
    if (!response.getString("status").equals("Success")) {
      log(listener, response.getString("error"));
      return;
    }

    String uploadDirectoryId = response.getString("directoryId");
    String[] tags = getUploadTags(build, listener, "tests.json");

    response = mber.upload(testResults, uploadDirectoryId, "tests.json", tags);
    if (response.getString("status").equals("Duplicate")) {
      log(listener, "You already have a build artifact named \"tests.json\". Please rename your build artifact.");
      return;
    }
    if (!response.getString("status").equals("Success")) {
      log(listener, response.getString("error"));
    }

    response = mber.publishTestResults(testResults);
    if (!response.getString("status").equals("Success")) {
      log(listener, response.getString("error"));
    }
  }

  @Override
  public boolean prebuild(AbstractBuild build, BuildListener listener)
  {
    // Clear the old call history. The notifier persists on a per-job basis.
    clearCallHistory(build);

    MberClient mber = makeMberClient();
    mber.setListener(listener);

    log(listener, "Connecting to Mber at "+mber.getURL());
    JSONObject response = mber.login(getUsername(), getDecryptedPassword());
    if (!response.getString("status").equals("Success")) {
      return fail(build, listener, mber, "Failed to connect to Mber. Check your configuration settings.");
    }

    log(listener, "Creating Mber project "+build.getProject().getDisplayName());
    response = mber.mkproject(build.getProject().getDisplayName(), build.getProject().getDescription());
    if (!response.getString("status").equals("Success")) {
      return fail(build, listener, mber, response.getString("error"));
    }

    String mberBuildName = getMberBuildName(build, listener);
    String mberBuildDescription = getMberBuildDescription(build, listener);
    log(listener, "Creating Mber build "+mberBuildName);
    log(listener, "Setting Mber build status to "+BuildStatus.RUNNING.toString());
    response = mber.mkbuild(mberBuildName, mberBuildDescription, build.getId(), BuildStatus.RUNNING);
    if (!response.getString("status").equals("Success")) {
      return fail(build, listener, mber, response.getString("error"));
    }

    this.mberConfig = mber.toJSON();
    recordCallHistory(build, mber);

    return true;
  }

  @Override
  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener)
  {
    MberClient mber = makeMberClient();
    mber.setListener(listener);

    if (!this.uploadArtifacts || isFailedBuild(build)) {
      return done(build, listener, mber);
    }

    FilePath[] artifacts = findBuildArtifacts(build, listener);
    if (artifacts.length == 0) {
      return fail(build, listener, mber, "No build artifacts found in "+getBuildArtifacts());
    }

    JSONObject response = makeArtifactFolder(build, listener, mber, true);
    if (!response.getString("status").equals("Success")) {
      return fail(build, listener, mber, response.getString("error"));
    }

    JSONArray errors = new JSONArray();

    Map<FilePath, String> buildArtifactFolders = findBuildArtifactFolders(build, listener, artifacts);
    Iterator<Map.Entry<FilePath, String>> folderItr = buildArtifactFolders.entrySet().iterator();
    while (folderItr.hasNext()) {
      Map.Entry<FilePath, String> artifact = folderItr.next();
      FilePath path = artifact.getKey();
      String folder = artifact.getValue();
      String[] tags = getUploadTags(build, listener, path);
      log(listener, "Uploading artifact "+path.getRemote());
      response = mber.mkpath(folder);
      String folderId = MberJSON.getString(response, "directoryId");
      if (!folderId.isEmpty()) {
        response = mber.upload(path, folderId, path.getName(), tags, isOverwriteExistingFiles());
      }
      if (!response.getString("status").equals("Success")) {
        errors.add(MberJSON.getString(response, "error"));
      }
    }

    if (!errors.isEmpty()) {
      return fail(build, listener, mber, errors.join("\n"));
    }

    return done(build, listener, mber);
  }

  @Override
  public boolean needsToRunAfterFinalized()
  {
    return true;
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService()
  {
    return BuildStepMonitor.NONE;
  }

  @Override
  public MberNotifier.DescriptorImpl getDescriptor()
  {
    return (MberNotifier.DescriptorImpl)super.getDescriptor();
  }

  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher>
  {
    private String mberUrl;

    public DescriptorImpl()
    {
      super(MberNotifier.class);
      load();
    }

    public FormValidation doValidateLogin(@QueryParameter String application, @QueryParameter String username, @QueryParameter String password)
    {
      MberClient mber = new MberClient(getMberUrl(), application);
      JSONObject response = mber.login(username, Secret.fromString(password).getPlainText());
      if (response.getString("status").equals("Success")) {
        return FormValidation.ok("Success!");
      }
      return FormValidation.error(response.getString("error"));
    }

    @Override
    public String getDisplayName()
    {
      return "Mber Notification";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException
    {
      req.bindJSON(this, formData);
      save();
      return super.configure(req, formData);
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass)
    {
      return true;
    }

    public FormValidation doCheckMberUrl(@QueryParameter String value)
    {
      if (MberClient.isMberURL(value)) {
        return FormValidation.ok();
      }
      return FormValidation.error("Invalid Mber URL. Clear the field and save the form to use the default value.");
    }

    public String getMberUrl()
    {
      if (mberUrl == null || mberUrl.isEmpty()) {
        return getDefaultMberUrl();
      }
      return mberUrl;
    }

    public void setMberUrl(final String url)
    {
      mberUrl = url;
    }

    public String getDefaultMberUrl()
    {
      return "https://member.firepub.net";
    }

    public String getDefaultArtifactFolder()
    {
      return "build/jenkins/${JOB_NAME}/${BUILD_NUMBER}";
    }

    public String getDefaultArtifactTags()
    {
      return "${JOB_NAME} ${BUILD_NUMBER}";
    }
  }
}
