class MakeSettingsStiCompatible < ActiveRecord::Migration
  def self.up
    rename_column :settings, :type, :setting_type
  end

  def self.down
    rename_column :settings, :setting_type, :type
  end
end
