module Rails
  module Upgrading
    class NewConfigurationGenerator
      def generate_new_configurations
        if has_environment?
          generate_new_application_rb
        else
          raise FileNotFoundError, "Can't find environment.rb [config/environment.rb]!"
        end
      end
      
      def has_environment?
        File.exists?("config/environment.rb")
      end
      
      def environment_code
        File.open("config/environment.rb").read
      end
      
      def generate_new_application_rb
        environment_file = environment_code
        
        initializer_code = ""
        if matches = environment_file.match(/Rails\:\:Initializer\.run do \|config\|\n(.*)\nend/m)
          initializer_code = matches[1]
        else
          raise "There doesn't seem to be a real environment.rb in your app.  Are you sure config/environment.rb has the right contents?"
        end
        
        frame = "# Put this in config/application.rb
require File.expand_path('../boot', __FILE__)

module #{app_name.classify}
  class Application < Rails::Application
%s
  end
end"
        
        frame % [indent(initializer_code)]        
      end
      
      def indent(text)
        text.split("\n").map {|l| "  #{l}"}.join("\n")
      end
      
      def app_name
        File.basename(Dir.pwd)
      end
    end
  end
end