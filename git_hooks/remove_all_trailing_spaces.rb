#!/usr/bin/env ruby

FILES_AND_EXTS = %w(Gemfile Rakefile Capfile rb css js erb haml yml rake html rhtml rxml builder markdown textile rdoc)
DIR = File.expand_path(File.dirname(__FILE__) + '/../')

(Dir["#{DIR}/**/*"] + Dir["#{DIR}/*"]).each do |filename|
  filename.chomp!
  next if filename =~ /vendor\/plugins/
  next unless File.file?(filename)
  next unless FILES_AND_EXTS.any? { |ext| filename.end_with?(ext) }
  puts filename
  data = []
  File.open(filename, 'r').each_line { |line| data << line.rstrip.chomp }
  File.open(filename, 'w+') { |file| file << data.join("\n") + "\n" }
end
