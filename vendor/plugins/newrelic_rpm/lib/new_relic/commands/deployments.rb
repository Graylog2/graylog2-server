# This is a class for executing commands related to deployment 
# events.  It runs without loading the rails environment

$LOAD_PATH << File.expand_path(File.join(File.dirname(__FILE__),"..",".."))
require 'yaml'
require 'net/http'
require 'rexml/document'

# We need to use the Control object but we don't want to load 
# the rails/merb environment.  The defined? clause is so that
# it won't load it twice, something it does when run inside a test
require 'new_relic/control' unless defined? NewRelic::Control

module NewRelic
  module Commands
    # Capture a failure to execute the command.
    # Ask it for a return status to exit the vm with,
    # if appropriate.
    class CommandFailure < StandardError
      attr_reader :exit_code
      def initialize message, return_status=nil
        super message
        @exit_code = return_status || 0
      end
    end
    
    class Deployments
      
      attr_reader :config
      def self.command; "deployments"; end 
      
      # Initialize the deployment uploader with command line args.
      # Use -h to see options.
      # When command_line_args is a hash, we are invoking directly and
      # it's treated as an options with optional sttring values for
      # :user, :description, :appname, :revision, :environment,
      # and :changes.
      #
      # Will throw CommandFailed exception if there's any error.
      # 
      def initialize command_line_args
        @config = NewRelic::Control.instance
        @user = ENV['USER']
        if Hash === command_line_args
          # command line args is an options hash
          command_line_args.each do | key, value |
            if %w[user environment description appname revision changelog].include? key.to_s
              instance_variable_set "@#{key}", value.to_s if value
            else
              raise "Unrecognized option #{key}=#{value}"
            end
          end
        else
          # parse command line args.  Throw an exception on a bad arg.
          @description = options.parse(command_line_args).join " "
        end
        config.env = @environment if @environment
        @appname ||= config.app_names[0] || config.env || 'development'
      end
      
      # Run the Deployment upload in RPM via Active Resource.
      # Will possibly print errors and exit the VM
      def run
        begin
          @description = nil if @description && @description.strip.empty?
          create_params = {}
          {
            :application_id => @appname, 
            :host => Socket.gethostname, 
            :description => @description,
            :user => @user,
            :revision => @revision,
            :changelog => @changelog
          }.each do |k, v|
            create_params["deployment[#{k}]"] = v unless v.nil? || v == ''
          end
          http = config.http_connection(config.api_server)
          
          uri = "/deployments.xml"
          
          raise "license_key was not set in newrelic.yml for #{config.env}" if config['license_key'].nil?
          request = Net::HTTP::Post.new(uri, {'x-license-key' => config['license_key']})
          request.content_type = "application/octet-stream"
          
          request.set_form_data(create_params)
          
          response = http.request(request)
          
          if response.is_a? Net::HTTPSuccess
            info "Recorded deployment to '#{@appname}' (#{@description || Time.now })"
          else
            err_string = [ "Unexpected response from server: #{response.code}: #{response.message}" ]
            begin
              doc = REXML::Document.new(response.body)
              doc.elements.each('errors/error') do |error|
                err_string << "Error: #{error.text}"
              end
            rescue
            end
            raise CommandFailure.new(err_string.join("\n"), -1)
          end 
        rescue SystemCallError, SocketError => e
          # These include Errno connection errors 
          err_string = "Transient error attempting to connect to #{config.api_server} (#{e})"
          raise CommandFailure.new(err_string, -1)
        rescue CommandFailure
          raise
        rescue Exception => e
          err "Unexpected error attempting to connect to #{config.api_server}"
          info "#{e}: #{e.backtrace.join("\n   ")}"
          raise CommandFailure.new(e.to_s, -1)
        end
      end
      
      private
      
      def options
        OptionParser.new %Q{Usage: #{$0} [OPTIONS] ["description"] }, 40 do |o|
          o.separator "OPTIONS:"
          o.on("-a", "--appname=NAME", String,
             "Set the application name.",
             "Default is app_name setting in newrelic.yml") { | e | @appname = e }
          o.on("-e", "--environment=name", String,
               "Override the (RAILS|MERB|RUBY|RACK)_ENV setting",
               "currently: #{config.env}") { | e | @environment = e }
          o.on("-u", "--user=USER", String,
             "Specify the user deploying, for information only",
             "Default: #{@user || '<none>'}") { | u | @user = u }
          o.on("-r", "--revision=REV", String,
             "Specify the revision being deployed") { | r | @revision = r }
          o.on("-c", "--changes", 
             "Read in a change log from the standard input") { @changelog = STDIN.read }
          o.on("-h", "--help", "Print this help") { raise CommandFailure.new(o.help, 0) }
        end
      end
      
      def info message
        STDOUT.puts message
      end
      def err message
        STDERR.puts message
      end  
    end
  end
end