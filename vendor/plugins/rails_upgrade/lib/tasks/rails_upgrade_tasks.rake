$:.unshift(File.dirname(__FILE__) + "/../../lib")
require 'routes_upgrader'
require 'gemfile_generator'
require 'application_checker'
require 'new_configuration_generator'

require 'fileutils'

namespace :rails do
  namespace :upgrade do
    desc "Runs a battery of checks on your Rails 2.x app and generates a report on required upgrades for Rails 3"
    task :check do
      checker = Rails::Upgrading::ApplicationChecker.new
      checker.run
    end
  
    desc "Generates a Gemfile for your Rails 3 app out of your config.gem directives"
    task :gems do
      generator = Rails::Upgrading::GemfileGenerator.new
      new_gemfile = generator.generate_new_gemfile
    
      puts new_gemfile
    end
  
    desc "Create a new, upgraded route file from your current routes.rb"
    task :routes do
      upgrader = Rails::Upgrading::RoutesUpgrader.new
      new_routes = upgrader.generate_new_routes
    
      puts new_routes
    end
    
    desc "Extracts your configuration code so you can create a new config/application.rb"
    task :configuration do
      upgrader = Rails::Upgrading::NewConfigurationGenerator.new
      new_config = upgrader.generate_new_application_rb
      
      puts new_config
    end
    
    CLEAR      = "\e[0m"
    CYAN       = "\e[36m"
    WHITE      = "\e[37m"
    
    desc "Backs up your likely modified files so you can run the Rails 3 generator on your app with little risk"
    task :backup do
      files = [".gitignore",
      "app/controllers/application_controller.rb",
      "app/helpers/application_helper.rb",
      "config/routes.rb",
      "config/environment.rb",
      "config/environments/development.rb",
      "config/environments/production.rb",
      "config/environments/staging.rb",
      "config/database.yml",
      "config.ru",
      "doc/README_FOR_APP",
      "test/test_helper.rb"]
      
      puts
      files.each do |f|
        if File.exist?(f)
          puts "#{CYAN}* #{CLEAR}backing up #{WHITE}#{f}#{CLEAR} to #{WHITE}#{f}.rails2#{CLEAR}"
          FileUtils.cp(f, "#{f}.rails2")
        end
      end
      
      puts
      puts "This is a list of the files analyzed and backed up (if they existed);\nyou will probably not want the generator to replace them since\nyou probably modified them (but now they're safe if you accidentally do!)."
      puts
      
      files.each do |f|
        puts "#{CYAN}- #{CLEAR}#{f}"
      end
      puts
    end
  end
end
