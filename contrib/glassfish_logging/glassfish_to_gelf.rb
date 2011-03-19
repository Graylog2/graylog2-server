require "rubygems"
require "gelf"

# set this to the host where the graylog2 server is running on
gelf_host = "syslog"
gelf_port = 12201

# this is needed to work around a bug in the current gelf gem
module GELF
  LEVELS_MAPPING = {
    0 => 0, 1 => 1, 2 => 2, 3 => 3, 4 => 4, 5 => 5, 6 => 6, 7 => 7}
end

# get current hostname to add to the gelf messages
hostname = `hostname -f`.chomp

# initialize GELF sender class
sender = GELF::Notifier.new(gelf_host, gelf_port, 8192)

LEVELS = {"SEVERE" => 0,
	"WARNING" => 4,
        "CONFIG"=> 5,
        "INFO" => 6,
        "FINE" => 7,
	"FINER" => 7,
	"FINEST" => 7}

# this maps the glassfish severities to those of graylog2
def transform_level(level)
  LEVELS.has_key?(level) ? LEVELS[level] : 6
end

$stdin.each("#]\n\n") do |line|
  if line =~ /^\[#/ then
    fields = line.chomp.split("|")

    message = { "host" => hostname,
		"level" => transform_level(fields[2]),
		:Product => fields[3],
		:LoggerName => fields [4],
		:short_message => fields[6][0..100],
		:full_message => fields[5..-2].join("|")}

    sender.notify!(message)
  end
end

