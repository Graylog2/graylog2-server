require 'open3'

module Rails
  module Upgrading
    class ApplicationChecker
      def initialize
        @issues = []

        raise NotInRailsAppError unless in_rails_app?
      end

      def in_rails_app?
        File.exist?("config/environment.rb")
      end

      # Run all the check methods
      def run
        # Ruby 1.8 returns method names as strings whereas 1.9 uses symbols
        the_methods = (self.public_methods - Object.methods) - [:run, :initialize, "run", "initialize"]

        the_methods.each {|m| send m }
      end

      # Check for deprecated ActiveRecord calls
      def check_ar_methods
        files = []
        ["find(:all", "find(:first", "find.*:conditions =>", ":joins =>"].each do |v|
          lines = grep_for(v, "app/")
          files += extract_filenames(lines) || []
        end

        unless files.empty?
          alert(
            "Soon-to-be-deprecated ActiveRecord calls",
            "Methods such as find(:all), find(:first), finds with conditions, and the :joins option will soon be deprecated.",
            "http://m.onkey.org/2010/1/22/active-record-query-interface",
            files
          )
        end

        lines = grep_for("named_scope", "app/models/")
        files = extract_filenames(lines)

        unless files.empty?
          alert(
            "named_scope is now just scope",
            "The named_scope method has been renamed to just scope.",
            "http://github.com/rails/rails/commit/d60bb0a9e4be2ac0a9de9a69041a4ddc2e0cc914",
            files
          )
        end
      end
      
      def check_validation_on_methods
        files = []
        
        ["validate_on_create", "validate_on_update"].each do |v|
          lines = grep_for(v, "app/models/")
          files += extract_filenames(lines) || []
        end
        
        unless files.empty?
          alert(
            "Updated syntax for validate_on_* methods",
            "Validate-on-callback methods (validate_on_create/validate_on_destroy) have been changed to validate :x, :on => :create",
            "https://rails.lighthouseapp.com/projects/8994/tickets/3880-validate_on_create-and-validate_on_update-no-longer-seem-to-exist",
            files
          )
        end
      end
      
      def check_before_validation_on_methods
        files = []
        
        %w(before_validation_on_create before_validation_on_update).each do |v|
          lines = grep_for(v, "app/models/")
          files += extract_filenames(lines) || []
        end
        
        unless files.empty?
          alert(
            "Updated syntax for before_validation_on_* methods",
            "before_validation_on_* methods have been changed to before_validation(:on => :create/:update) { ... }",
            "https://rails.lighthouseapp.com/projects/8994/tickets/4699-before_validation_on_create-and-before_validation_on_update-doesnt-exist",
            files
          )
        end
      end

      # Check for deprecated router syntax
      def check_routes
        lines = ["map\\.", "ActionController::Routing::Routes", "\\.resources"].map do |v|
          grep_for(v, "config/routes.rb").empty? ? nil : true
        end.compact

        unless lines.empty?
          alert(
            "Old router API",
            "The router API has totally changed.",
            "http://yehudakatz.com/2009/12/26/the-rails-3-router-rack-it-up/",
            "config/routes.rb"
          )
        end
      end

      # Check for deprecated test_help require
      def check_test_help
        files = []

        # Hate to duplicate code, but we have to double quote this one...
        lines = grep_for("\'test_help\'", "test/", true)
        files += extract_filenames(lines) || []

        lines = grep_for("\"test_help\"", "test/")
        files += extract_filenames(lines) || []

        files.uniq!

        unless files.empty?
          alert(
            "Deprecated test_help path",
            "You now must require 'rails/test_help' not just 'test_help'.",
            "http://weblog.rubyonrails.org/2009/9/1/gem-packaging-best-practices",
            files
          )
        end
      end

      # Check for old (pre-application.rb) environment.rb file
      def check_environment
        unless File.exist?("config/application.rb")
          alert(
            "New file needed: config/application.rb",
            "You need to add a config/application.rb.",
            "http://omgbloglol.com/post/353978923/the-path-to-rails-3-approaching-the-upgrade",
            "config/application.rb"
          )
        end

        lines = grep_for("config.", "config/environment.rb")

        unless lines.empty?
          alert(
            "Old environment.rb",
            "environment.rb doesn't do what it used to; you'll need to move some of that into application.rb.",
            "http://omgbloglol.com/post/353978923/the-path-to-rails-3-approaching-the-upgrade",
            "config/environment.rb"
          )
        end
      end

      # Check for deprecated constants
      def check_deprecated_constants
        files = []
        ["RAILS_ENV", "RAILS_ROOT", "RAILS_DEFAULT_LOGGER"].each do |v|
          lines = grep_for(v, "app/")
          files += extract_filenames(lines) || []

          lines = grep_for(v, "lib/")
          files += extract_filenames(lines) || []
        end

        unless files.empty?
          alert(
            "Deprecated constant(s)",
            "Constants like RAILS_ENV, RAILS_ROOT, and RAILS_DEFAULT_LOGGER are now deprecated.",
            "http://litanyagainstfear.com/blog/2010/02/03/the-rails-module/",
            files.uniq
          )
        end
      end

      # Check for old-style config.gem calls
      def check_gems
        lines = grep_for("config.gem ", "config/*.rb")
        lines += grep_for("config.gem ", "config/**/*.rb")
        files = extract_filenames(lines)

        unless files.empty?
          alert(
            "Old gem bundling (config.gems)",
            "The old way of bundling is gone now.  You need a Gemfile for bundler.",
            "http://omgbloglol.com/post/353978923/the-path-to-rails-3-approaching-the-upgrade",
            files
          )
        end
      end

      # Checks for old mailer syntax in both mailer classes and those
      # classes utilizing the mailers
      def check_mailers
        lines = grep_for("deliver_", "app/models/ #{base_path}app/controllers/ #{base_path}app/observers/")
        files = extract_filenames(lines)

        unless files.empty?
          alert(
            "Deprecated ActionMailer API",
            "You're using the old ActionMailer API to send e-mails in a controller, model, or observer.",
            "http://lindsaar.net/2010/1/26/new-actionmailer-api-in-rails-3",
            files
          )
        end

        files = []
        ["recipients ", "attachment(?!s) ", "(?<!:)subject ", "(?<!:)from "].each do |v|
          lines = grep_for_with_perl_regex(v, "app/models/")
          files += extract_filenames(lines) || []
        end

        unless files.empty?
          alert(
            "Old ActionMailer class API",
            "You're using the old API in a mailer class.",
            "http://lindsaar.net/2010/1/26/new-actionmailer-api-in-rails-3",
            files
          )
        end
      end

      # Checks for old-style generators
      def check_generators
        generators = Dir.glob(base_path + "vendor/plugins/**/generators/**/")

        unless generators.empty?
          files = generators.reject do |g|
                    grep_for("def manifest", g).empty? 
                  end.compact

          unless files.empty?
            alert(
              "Old Rails generator API",
              "A plugin in the app is using the old generator API (a new one may be available at http://github.com/trydionel/rails3-generators).",
              "http://blog.plataformatec.com.br/2010/01/discovering-rails-3-generators/",
              files
            )
          end
        end
      end

      # Checks a list of known broken plugins and gems
      def check_plugins
        # This list is off the wiki; will need to be updated often, esp. since RSpec is working on it
        bad_plugins = ["rspec", "rspec-rails", "hoptoad", "authlogic", "nifty-generators",
           "restful_authentication", "searchlogic", "cucumber", "cucumber-rails", "devise",
           "inherited_resources"]

        bad_plugins = bad_plugins.map do |p|
                        p if File.exist?("#{base_path}vendor/plugins/#{p}") || !Dir.glob("#{base_path}vendor/gems/#{p}-*").empty?
                      end.compact

        unless bad_plugins.empty?
          alert(
            "Known broken plugins",
            "At least one plugin in your app is broken (according to the wiki).  Most of project maintainers are rapidly working towards compatability, but do be aware you may encounter issues.",
            "http://wiki.rubyonrails.org/rails/version3/plugins_and_gems",
            bad_plugins
          )
        end
      end

      # Checks for old-style ERb helpers
      def check_old_helpers

        lines = grep_for("<% .*content_tag.* do.*%>", "app/views/**/*")
        lines += grep_for("<% .*javascript_tag.* do.*%>", "app/views/**/*")
        lines += grep_for("<% .*form_for.* do.*%>", "app/views/**/*")
        lines += grep_for("<% .*form_tag.* do.*%>", "app/views/**/*")
        lines += grep_for("<% .*fields_for.* do.*%>", "app/views/**/*")
        lines += grep_for("<% .*field_set_tag.* do.*%>", "app/views/**/*")
        
        files = extract_filenames(lines)

        if !files.blank?
          alert(
            "Deprecated ERb helper calls",
            "Block helpers that use concat (e.g., form_for) should use <%= instead of <%.  The current form will continue to work for now, but you will get deprecation warnings since this form will go away in the future.",
            "http://weblog.rubyonrails.org/",
            files
          )
        end
      end

      # Checks for old-style AJAX helpers
      def check_old_ajax_helpers
        files = []
        ['link_to_remote','form_remote_tag','remote_form_for'].each do |type|
          lines = grep_for(type, "app/views/**/*")
          inner_files = extract_filenames(lines)
          files += inner_files unless inner_files.nil?
        end

        unless files.empty?
          alert(
            "Deprecated AJAX helper calls",
            "AJAX javascript helpers have been switched to be unobtrusive and use :remote => true instead of having a seperate function to handle remote requests.",
            "http://www.themodestrubyist.com/2010/02/24/rails-3-ujs-and-csrf-meta-tags/",
            files
          )
        end
      end

      # Checks for old cookie secret settings
      def check_old_cookie_secret
        lines = grep_for("ActionController::Base.cookie_verifier_secret = ", "config/**/*")
        files = extract_filenames(lines)

        unless files.empty?
          alert(
            "Deprecated cookie secret setting",
            "Previously, cookie secret was set directly on ActionController::Base; it's now config.secret_token.",
            "http://lindsaar.net/2010/4/7/rails_3_session_secret_and_session_store",
            files
          )
        end
      end

      def check_old_session_secret
        lines = grep_for("ActionController::Base.session = {", "config/**/*")
        files = extract_filenames(lines)

        unless files.empty?
          alert(
            "Deprecated session secret setting",
            "Previously, session secret was set directly on ActionController::Base; it's now config.secret_token.",
            "http://lindsaar.net/2010/4/7/rails_3_session_secret_and_session_store",
            files
          )
        end
      end

      # Checks for old session settings
      def check_old_session_setting
        lines = grep_for("ActionController::Base.session_store", "config/**/*")
        files = extract_filenames(lines)

        unless files.empty?
          alert(
            "Old session store setting",
            "Previously, session store was set directly on ActionController::Base; it's now config.session_store :whatever.",
            "http://lindsaar.net/2010/4/7/rails_3_session_secret_and_session_store",
            files
          )
        end
      end

    private
      def grep_for_with_perl_regex(text, where = "./", double_quote = false)
        grep_for(text, where, double_quote, true)
      end

      # Find a string in a set of files; calls +find_with_grep+ and +find_with_rak+
      # depending on platform.
      #
      # TODO: Figure out if this works on Windows.
      def grep_for(text, where = "./", double_quote = false, perl_regex = false)
        # If they're on Windows, they probably don't have grep.
        @probably_has_grep ||= (Config::CONFIG['host_os'].downcase =~ /mswin|windows|mingw/).nil?
        
        # protect against double root paths in Rails 3
        where.gsub!(Regexp.new(base_path),'')

        lines = if @probably_has_grep
          find_with_grep(text, base_path + where, double_quote, perl_regex)
        else
          find_with_rak(text, base_path + where, double_quote)
        end

        # ignore comments
        lines.gsub /^(\/[^:]+:)?\s*#.+$/m, ""
      end

      # Sets a base path for finding files; mostly for testing
      def base_path
        Dir.pwd + "/"
      end

      # Use the grep utility to find a string in a set of files
      def find_with_grep(text, where, double_quote, perl_regex = false)
        value = ""
        # Specifically double quote for finding 'test_help'
        command = if double_quote
                    "grep -rH #{"-P" if perl_regex} \"#{text}\" #{where} | grep -v \.svn"
                  else
                    "grep -rH #{"-P" if perl_regex} '#{text}' #{where} | grep -v \.svn"
                  end

        Open3.popen3(command) do |stdin, stdout, stderr|
          value = stdout.read
        end
        value
      end

      # Use the rak gem to grep the files (not yet implemented)
      def find_with_rak(text, where, double_quote)
        value = ""
        Open3.popen3("rak --nogroup -l '#{Regexp.escape(text)}' #{where}") do |stdin, stdout, stderr|
          value = stdout.read
        end
        value
      end

      # Extract the filenames from the grep output
      def extract_filenames(output)
        if @probably_has_grep
          filenames = extract_filenames_from_grep(output)
        else
          filenames = extract_filenames_from_rak(output)
        end

        filenames.compact.map do |f|
          f.gsub(base_path, "")
        end
      end

      def extract_filenames_from_grep(output)
        return [] if output.empty?

        output.split("\n").map do |fn|
          if m = fn.match(/^(.+?):/)
            m[1]
          end
        end.compact.uniq
      end

      def extract_filenames_from_rak(output)
        return [] if output.empty?

        output.split("\n").uniq
      end

      # Terminal colors, borrowed from Thor
      CLEAR      = "\e[0m"
      BOLD       = "\e[1m"
      RED        = "\e[31m"
      YELLOW     = "\e[33m"
      CYAN       = "\e[36m"
      WHITE      = "\e[37m"

      # Show an upgrade alert to the user
      def alert(title, text, more_info_url, culprits)
        if Config::CONFIG['host_os'].downcase =~ /mswin|windows|mingw/
          basic_alert(title, text, more_info_url, culprits)
        else
          color_alert(title, text, more_info_url, culprits)
        end
      end

      # Show an upgrade alert to the user.  If we're on Windows, we can't
      # use terminal colors, hence this method.
      def basic_alert(title, text, more_info_url, culprits)
        puts "** " + title
        puts text
        puts "More information: #{more_info_url}"
        puts
        puts "The culprits: "
        Array(culprits).each do |c|
          puts "\t- #{c}"
        end
        puts
      end

      # Show a colorful alert to the user
      def color_alert(title, text, more_info_url, culprits)
        puts "#{RED}#{BOLD}#{title}#{CLEAR}"
        puts "#{WHITE}#{text}"
        puts "#{BOLD}More information:#{CLEAR} #{CYAN}#{more_info_url}"
        puts
        puts "#{WHITE}The culprits: "
        Array(culprits).each do |c|
          puts "#{YELLOW}\t- #{c}"
        end
      ensure
        puts "#{CLEAR}"
      end
    end
  end
end
