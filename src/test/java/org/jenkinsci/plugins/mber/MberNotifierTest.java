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
import hudson.model.FreeStyleProject;
import org.jvnet.hudson.test.JenkinsRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MberNotifierTest
{
  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();

  @Test
  public void testGlobalConfigDefaults() throws Exception
  {
    // Make sure the global config has a valid default URL after being saved.
    MberNotifier.DescriptorImpl before = getGlobalConfig();
    submitGlobalConfig();
    MberNotifier.DescriptorImpl after = getGlobalConfig();
    assertEquals(before.getDefaultMberUrl(), after.getMberUrl());
  }

  @Test
  public void testGlobalConfigRoundTrip() throws Exception
  {
    // Make sure the global config keeps an explicit URL after being saved.
    MberNotifier.DescriptorImpl before = getGlobalConfig();
    before.setMberUrl("http://this.is.mber");
    submitGlobalConfig();
    MberNotifier.DescriptorImpl after = getGlobalConfig();
    assertEquals(before.getMberUrl(), after.getMberUrl());
  }

  @Test
  public void testConfigDefaults() throws Exception
  {
    // Make sure a project's config has sensible defaults.
    MberNotifier notifier = new MberNotifier(null, null, null, null, null, false, false, null);
    assertEquals("Provide a deault upload folder", notifier.getDescriptor().getDefaultArtifactFolder(), notifier.getArtifactFolder());
    assertEquals("Don't upload artifacts if none are given", false, notifier.isUploadArtifacts());
    assertNull("Has upload artifacts when none where given", notifier.getBuildArtifacts());
  }

  @Test
  public void testConfigArtifactUploads() throws Exception
  {
    // Make sure the artifact uploads are set correctly.
    MberNotifier notifier = new MberNotifier(null, null, null, null, null, false, false, new UploadArtifactsBlock("files", "folder", "${BUILD_NUMBER}", false));
    assertEquals("Upload folder wasn't set", "folder", notifier.getArtifactFolder());
    assertEquals("Upload file list wasn't set", "files", notifier.getBuildArtifacts());
  }

  @Test
  public void testConfigRoundtrip() throws Exception
  {
    FreeStyleProject project = jenkinsRule.createFreeStyleProject();
    MberNotifier before = new MberNotifier("application", "username", "password", "build name", "build description", true, true, new UploadArtifactsBlock("files", "folder", "${BUILD_NUMBER}", true));
    project.getPublishersList().add(before);
    submitProjectConfig(project);
    MberNotifier after = project.getPublishersList().get(MberNotifier.class);
    assertSame(before, after);
  }

  private MberNotifier.DescriptorImpl getGlobalConfig()
  {
    return jenkinsRule.getInstance().getDescriptorByType(MberNotifier.DescriptorImpl.class);
  }

  private void submitGlobalConfig() throws Exception
  {
    jenkinsRule.submit(jenkinsRule.createWebClient().goTo("configure").getFormByName("config"));
  }

  private void submitProjectConfig(final FreeStyleProject p) throws Exception
  {
    jenkinsRule.submit(jenkinsRule.createWebClient().getPage(p, "configure").getFormByName("config"));
  }

  private void assertSame(final MberNotifier expected, final MberNotifier actual)
  {
    assertEquals("Application didn't match", expected.getApplication(), actual.getApplication());
    assertEquals("Username didn't match", expected.getUsername(), actual.getUsername());
    assertEquals("Password didn't match", expected.getPassword(), actual.getPassword());
    assertEquals("Will upload console log didn't match", expected.isUploadConsoleLog(), actual.isUploadConsoleLog());
    assertEquals("Will upload test results didn't match", expected.isUploadTestResults(), actual.isUploadTestResults());
    assertEquals("Will upload artifacts didn't match", expected.isUploadArtifacts(), actual.isUploadArtifacts());
    assertEquals("Upload folders didn't match", expected.getArtifactFolder(), actual.getArtifactFolder());
    assertEquals("Upload files didn't match", expected.getBuildArtifacts(), actual.getBuildArtifacts());
  }
}
