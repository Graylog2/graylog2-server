require 'optparse'

# Run the command given by the first argument.  Right
# now all we have is deployments. We hope to have other
# kinds of events here later.

libdir = File.expand_path(File.join(File.dirname(__FILE__), '..','..'))
command_list = Dir[File.join(libdir,'new_relic','commands','*.rb')].map{|command| command =~ /.*\/(.*)\.rb/ && $1}
command_list.delete 'new_relic_commands'
extra = []
options = ARGV.options do |opts|
  script_name = File.basename($0)
  opts.banner = "Usage: #{__FILE__} #{ command_list.join(" | ")} [options]"
  opts.separator "use -h to see detailed command options"
  opts
end
extra = options.order!
command = extra.shift
if !command_list.include?(command)
  STDERR.puts options
else
  require File.join(libdir, 'new_relic','commands', command + ".rb")
  command_class = NewRelic::Commands.const_get(command.capitalize) 
  begin
    command_class.new(extra).run
  rescue NewRelic::Commands::CommandFailure => failure
    STDERR.puts failure.message
    exit failure.exit_code
  end
end