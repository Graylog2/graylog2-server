# Author::    Michael Cowden
# Copyright:: MigraineLiving.com
# License::   Distributed under the same terms as Ruby

class FlotomaticGenerator < Rails::Generator::Base # :nodoc:
  def manifest
    record do |m|
      m.file 'flotomatic.css', "public/stylesheets/flotomatic.css"
      m.directory 'public/javascripts/flotomatic'
      %w(excanvas.js excanvas.min.js flotomatic.js jquery.colorhelpers.js jquery.colorhelpers.min.js jquery.flot.crosshair.js jquery.flot.crosshair.min.js jquery.flot.image.js jquery.flot.image.min.js jquery.flot.js jquery.flot.min.js jquery.flot.navigate.js jquery.flot.navigate.min.js jquery.flot.selection.js jquery.flot.selection.min.js jquery.flot.stack.js jquery.flot.stack.min.js jquery.flot.threshold.js jquery.flot.threshold.min.js jquery.js jquery.min.js).each do |file|
        m.file "flotomatic/#{file}", "public/javascripts/flotomatic/#{file}"
      end
    end
  end
end
