require 'fileutils'

directory = File.dirname(__FILE__)

FileUtils.rm_r File.join(directory, "..", "..", "..", "public", "javascripts", "flotomatic.js")
FileUtils.rm_r File.join(directory, "..", "..", "..", "public", "stylesheets", "flotomatic.css")

# Really?  Are you absolutely sure?
# Better safe than sorry, imho
# FileUtils.rm_r File.join(directory, "..", "..", "..", "public", "javascripts", "excanvas.pack.js")
# FileUtils.rm_r File.join(directory, "..", "..", "..", "public", "javascripts", "jquery.js")
# FileUtils.rm_r File.join(directory, "..", "..", "..", "public", "javascripts", "jquery-ui.js")
# FileUtils.rm_r File.join(directory, "..", "..", "..", "public", "javascripts", "jquery.flot.pack.js")
