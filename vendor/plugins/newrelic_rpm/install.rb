require 'fileutils'
require 'erb'

def install_newrelic_config_file(license_key="PASTE_YOUR_LICENSE_KEY_HERE")
  # Install a newrelic.yml file into the local config directory.
  if File.directory? "config"
    dest_dir = "config"
  else
    dest_dir = File.join(ENV["HOME"],".newrelic") rescue nil
    FileUtils.mkdir(dest_dir) if dest_dir
  end
  
  src_config_file = File.join(File.dirname(__FILE__),"newrelic.yml")
  dest_config_file = File.join(dest_dir, "newrelic.yml") if dest_dir
  
  if !dest_dir
    STDERR.puts "Could not find a config or ~/.newrelic directory to locate the default newrelic.yml file"
  elsif File::exists? dest_config_file
    STDERR.puts "\nA config file already exists at #{dest_config_file}.\n"
  else
    generated_for_user = ""
    yaml = ERB.new(File.read(src_config_file)).result(binding)
    File.open( dest_config_file, 'w' ) do |out|
      out.puts yaml
    end
    
    puts <<-EOF

Installed a default configuration file in #{dest_dir}.

To monitor your application in production mode, sign up for an account
at www.newrelic.com, and replace the newrelic.yml file with the one
you receive upon registration.

Please review the README.md file for more information.

E-mail support@newrelic.com with any problems or questions.

    EOF

  end  
end

if __FILE__ == $0 || $0 =~ /script\/plugin/
  install_newrelic_config_file
end
