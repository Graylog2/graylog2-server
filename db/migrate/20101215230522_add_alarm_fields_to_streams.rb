class AddAlarmFieldsToStreams < ActiveRecord::Migration
  def self.up
    add_column :streams, :alarm_active, :boolean
    add_column :streams, :alarm_force, :boolean
    add_column :streams, :alarm_limit, :integer
    add_column :streams, :alarm_timespan, :integer
  end

  def self.down
    remove_column :streams, :alarm_active
    remove_column :streams, :alarm_force
    remove_column :streams, :alarm_limit
    remove_column :streams, :alarm_timespam
  end
end
