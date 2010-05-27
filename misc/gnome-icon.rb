#!/usr/bin/ruby

require 'gtk2'

icon = Gtk::StatusIcon.new
icon.pixbuf = Gdk::Pixbuf.new("/home/lennart/testicon.png", 20, 20);
icon.tooltip = "Graylog2: 426 log messages in the last 10 minutes"

Gtk::Menu.new
Gtk::ImageMenuItem.new(Gtk::Stock::QUIT);

Thread.new { Gtk.main }

sleep 4
icon.blinking = true

system "notify-send -u critical -i /usr/share/icons/gnome/32x32/status/gtk-dialog-warning.png 'Graylog2: Warning' '426 new log messages in the last 10 minutes'"

while true do
  puts "bla"
  sleep 1
end
