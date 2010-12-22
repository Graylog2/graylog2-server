class RenameLastAlertCheck < ActiveRecord::Migration
  def self.up
    rename_column :streams, :last_alert_check, :last_alarm_check
  end

  def self.down
    rename_column :streams, :last_alarm_check, :last_alert_check
  end
end
