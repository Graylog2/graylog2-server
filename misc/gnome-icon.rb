#!/usr/bin/ruby

require 'gtk2'

TRAYICON = "/home/lennart/testicon.png"

NOTIFYICON = "/usr/share/icons/gnome/32x32/status/gtk-dialog-warning.png"
NOTIFYTITLE = "Graylog2: Warning"

class Graylog2Notification
  def self.notify message
    puts "notify-send -i #{NOTIFYICON} '#{NOTIFYTITLE}' '#{message}'"
  end
end

class Graylog2Icon
  def initialize
    @icon = Gtk::StatusIcon.new
    @icon.pixbuf = Gdk::Pixbuf.new(TRAYICON, 20, 20);
    @icon.tooltip = "Graylog"
  end

  def run
    Thread.new { Gtk.main }
  end

  def start_blinking secs = nil
    @icon.blinking = true

    # Reset blinking if requested.
    unless secs == nil
      Thread.new {
        # Reset blinking if requested.
        sleep secs
        @icon.blinking = false
      }
    end
  end

  def stop_blinking
    @icon.blinking = false
  end
end

icon = Graylog2Icon.new
icon.run

Graylog2Notification.notify "bla"

while true do
  puts "bla"
  sleep 1
end
