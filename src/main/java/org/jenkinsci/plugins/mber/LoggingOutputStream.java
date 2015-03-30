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
import hudson.model.BuildListener;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

public class LoggingOutputStream extends OutputStream
{
  private final OutputStream output;
  private final BuildListener listener;
  private final Timer timer = new Timer();
  private final long expectedBytes;
  private final String inputName;
  private long bytesWritten;

  public LoggingOutputStream(OutputStream output, BuildListener listener, long expectedBytes, String inputName)
  {
    this.output = output;
    this.listener = listener;
    this.expectedBytes = expectedBytes;
    this.inputName = inputName;
    this.bytesWritten = 0;
    initTimer();
  }

  @Override
  public void close() throws IOException
  {
    this.timer.cancel();
    this.output.close();
    if (this.listener != null) {
      int percent = Math.round(this.bytesWritten * 100 / this.expectedBytes);
      this.listener.getLogger().println("Uploaded "+percent+"% of "+this.inputName);
    }
  }

  @Override
  public void flush() throws IOException
  {
    this.output.flush();
  }

  @Override
  public void write(byte[] b) throws IOException
  {
    log(b.length);
    this.output.write(b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException
  {
    log(len);
    this.output.write(b, off, len);
  }

  @Override
  public void write(int b) throws IOException
  {
    log(1);
    this.output.write(b);
  }

  private void log(int length)
  {
    this.bytesWritten += length;
  }

  private void initTimer()
  {
    long timeout = 1 * 60 * 1000;
    this.timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run()
      {
        if (listener != null) {
          int percent = Math.round(bytesWritten * 100 / expectedBytes);
          listener.getLogger().println("Uploaded "+percent+"% of "+inputName);
        }
      }
    }, timeout, timeout);
  }
}
