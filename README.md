# Jenkins Mber Plugin #

This is a [Jenkins][] plugin that allows you to notify [Mber][] about build
status.

## Building ##

This plugin depends on [Maven][] being installed.

~~~bash
mvn install
~~~

## Development ##

### Vagrant ###

If you have [VirtualBox][] and [Vagrant][] installed, you can run a local
Jenkins server for testing. The included Vagrantfile installs the latest version
of Jenkins. You can point a browser at 192.168.56.10:8080 to see your server.

~~~bash
vagrant up
~~~

Once the Jenkins server is up, you'll need to manually install the pugin.

### NetBeans ###

If you're using [NetBeans][] you can open and run the project directly. This
will start up a local Jenkins server on port 8080 for testing. The server will
already have the plugin installed.

# License and Copyright #

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


[Jenkins]: https://jenkins-ci.org/ "Jenkins is an extensible open source continuous integration server"
[Mber]: https://member.firepub.net/ "Mber is a platform for building, hosting, launching, and tracking games"
[Maven]: http://maven.apache.org/ "Maven is a software project management and comprehension tool."
[VirtualBox]: https://www.virtualbox.org/ "VirtualBox is x86 and AMD64 virtualization product."
[Vagrant]: http://www.vagrantup.com/ "Vagrant is a tool for creating and configuring lightweight, reproducible, and portable development environments."
[NetBeans]: https://netbeans.org/ "NetBeans is a free IDE for Java development."
