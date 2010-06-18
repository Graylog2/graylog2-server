require 'rake'
require 'spec/rake/spectask'
require 'rake/rdoctask'

desc 'Default: run unit tests.'
task :default => :spec

desc 'Test the flotomatic plugin.'
task :spec do
  system "spec spec/lib/*_spec.rb"
  system "spec spec/helper/*_spec.rb"
end
# Spec::Rake::SpecTask.new do |t|
#   t.spec_files = FileList['spec/lib/*_spec.rb'] # + FileList['spec/helper/*_spec.rb']
# end

desc 'Generate documentation for the flotomatic plugin.'
Rake::RDocTask.new(:rdoc) do |rdoc|
  rdoc.rdoc_dir = 'rdoc'
  rdoc.title    = 'Flotomatic'
  rdoc.options << '--line-numbers' << '--inline-source'
  rdoc.rdoc_files.include('README')
  rdoc.rdoc_files.include('lib/**/*.rb')
  rdoc.rdoc_files.include('app/helpers/*.rb')
end

desc 'Copy the necessary css and javascript files into your project.'
task :install do
  directory = File.dirname(__FILE__)
  generate  = File.join(directory, '..', '..', '..', 'script', 'generate')
  system "ruby #{generate} flotomatic"
end