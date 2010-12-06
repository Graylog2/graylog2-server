# Add your own tasks in files placed in lib/tasks ending in .rake,
# for example lib/tasks/capistrano.rake, and they will automatically be available to Rake.

require File.expand_path('../config/application', __FILE__)
require 'rake'
require 'metric_fu'

Graylog2WebInterface::Application.load_tasks

require 'ci/reporter/rake/test_unit'
