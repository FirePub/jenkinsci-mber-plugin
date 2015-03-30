# -*- mode: ruby -*-
# vi: set ft=ruby :

# The Jenkins Mber Plugin is free software distributed under the terms of the MIT
# license (http://opensource.org/licenses/mit-license.html) reproduced here:
#
# Copyright (c) 2013-2015 Mber
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.

$script = <<SCRIPT
set -ex
sed -i 's/127.0.1.1/127.0.0.1/g' /etc/hosts
wget -q -O - http://pkg.jenkins-ci.org/debian/jenkins-ci.org.key | apt-key add -
echo deb http://pkg.jenkins-ci.org/debian binary/ | tee /etc/apt/sources.list.d/jenkins.list
apt-get -q update
apt-get -y install jenkins
SCRIPT

Vagrant.configure('2') do |config|
  config.vm.box = 'precise64'
  config.vm.box_url = 'http://files.vagrantup.com/precise64.box'

  config.vm.provider 'virtualbox' do |vbox|
    vbox.customize ['modifyvm', :id, '--memory', 2048]
    vbox.name = 'jenkins'
  end

  config.vm.define :jenkins do |jenkins|
    jenkins.vm.network :private_network, :ip => '192.168.56.10'
    jenkins.vm.provision :shell, :inline => $script
  end
end
