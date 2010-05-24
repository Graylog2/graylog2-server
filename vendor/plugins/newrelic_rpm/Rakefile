require 'rubygems'
require 'rake'
require "#{File.dirname(__FILE__)}/lib/new_relic/version.rb"
require 'rake/testtask'

GEM_NAME = "newrelic_rpm"
GEM_VERSION = NewRelic::VERSION::STRING
AUTHOR = "Bill Kayser"
EMAIL = "support@newrelic.com"
HOMEPAGE = "http://www.github.com/newrelic/rpm"
SUMMARY = "New Relic Ruby Performance Monitoring Agent"
URGENT_README = "README-#{GEM_VERSION}"
# See http://www.rubygems.org/read/chapter/20 
begin
  require 'jeweler'
  Jeweler::Tasks.new do |gem|
    gem.name = GEM_NAME
    gem.summary = SUMMARY
    gem.description = <<-EOF
New Relic RPM is a Ruby performance management system, developed by
New Relic, Inc (http://www.newrelic.com).  RPM provides you with deep
information about the performance of your Ruby on Rails or Merb
application as it runs in production. The New Relic Agent is
dual-purposed as a either a Rails plugin or a Gem, hosted on
http://github.com/newrelic/rpm/tree/master.
    EOF
    gem.email = EMAIL
    gem.homepage = HOMEPAGE
    gem.author = AUTHOR
    gem.version = GEM_VERSION
    gem.files = FileList['**/*'].to_a - ['init.rb']
    gem.test_files = [] # You can't really run the tests unless the gem is installed.
    gem.rdoc_options << "--line-numbers" << "--inline-source" << "--title" << "New Relic RPM"
    gem.files.reject! { |fn| fn =~ /Rakefile|pkg\/|rdoc\// }
    gem.add_development_dependency "jeweler"
    gem.extra_rdoc_files = %w[CHANGELOG LICENSE]
    if File.exists?(URGENT_README)
      gem.post_install_message = File.read(URGENT_README)
    else
      gem.post_install_message = <<-EOF

Please see http://support.newrelic.com/faqs/docs/ruby-agent-release-notes
for a complete description of the features and enhancements available
in version #{GEM_VERSION.split('.')[0..1].join('.')} of the Ruby Agent.

For details on this specific release, refer to the CHANGELOG file.

      EOF
    end
  end
  Jeweler::GemcutterTasks.new
rescue LoadError
  puts "Jeweler (or a dependency) not available. Install it with: gem install jeweler"
end


load "#{File.dirname(__FILE__)}/lib/tasks/all.rb"

task :manifest do
  puts "Manifest task is no longer used since switching to jeweler."
end

task :test => Rake::Task['test:newrelic']

begin
  require 'rcov/rcovtask'
  Rcov::RcovTask.new do |test|
    test.libs << 'test'
    test.pattern = 'test/**/test_*.rb'
    test.verbose = true
  end
rescue LoadError
  task :rcov do
    abort "RCov is not available. In order to run rcov, you must: sudo gem install spicycode-rcov"
  end
end

task :test => :check_dependencies

task :default => :test

require 'rake/rdoctask'
Rake::RDocTask.new do |rdoc|
  rdoc.rdoc_dir = 'rdoc'
  rdoc.title = SUMMARY
  rdoc.rdoc_files.include('LICENSE')
  rdoc.rdoc_files.include('CHANGELOG')
  rdoc.rdoc_files.include('lib/**/*.rb')
end

begin
  require 'sdoc_helpers'
rescue LoadError
  puts "sdoc support not enabled. Please gem install sdoc-helpers."
end
