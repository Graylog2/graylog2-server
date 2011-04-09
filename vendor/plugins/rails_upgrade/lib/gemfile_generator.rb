module Rails
  module Upgrading
    class GemfileGenerator
      def generate_new_gemfile
        if has_environment?
          generate_gemfile
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
      
      def generate_gemfile
        environment_file = environment_code
        
        # Get each line that starts with config.gem
        gem_lines = environment_file.split("\n").select {|l| l =~ /^\s*config\.gem/}
        
        # Toss those lines to a generator class; the lines are evaluated in the 
        # context of that instance.
        config = GemfileGenerator.new
        config.instance_eval(gem_lines.join("\n"))
        
        config.output
      end
    end
    
    class GemfileGenerator
      # Creates a target for the config.gem calls
      def config
        self
      end
      
      def initialize
        @gems = []
      end
      
      # Receive a call to add a gem to the list
      def gem(name, options={})
        data = {}
        
        # Add new keys from old keys
        data[:require] = options[:lib] if options[:lib]
        data[:source] = options[:source] if options[:source]
        
        version = options[:version]
        @gems << [name, version, data]
      end
      
      # Generate the Gemfile output
      def output
        # Generic preamble, taken from Yehuda Katz's blog
        preamble = <<STR
# Edit this Gemfile to bundle your application's dependencies.
# This preamble is the current preamble for Rails 3 apps; edit as needed.
source 'http://rubygems.org'

gem 'rails', '3.0.6'

STR
        preamble + generate_upgraded_code
      end
      
      # Get Gemfile call for all the gems
      def generate_upgraded_code   
        code = @gems.map do |name, version, data|
          version_string = (version ? "'#{version}'" : nil)
          source = data.delete(:source)
          
          data_string = nil
          unless data.empty?
            data_string = data.to_a.map {|k, v| ":#{k} => '#{v}'"}.join(", ")
          end
          
          # If we have a source, generate a call to +source+ then output the
          # gem call.  Otherwise, just generate the gem requirement.
          if source
            str = ["'#{name}'", version_string, data_string].compact.join(", ")
            "source '#{source}'\ngem #{str}"
          else
            str = ["'#{name}'", version_string, data_string].compact.join(", ")
            "gem #{str}"
          end
        end.join("\n")
      end
    end
  end
end
