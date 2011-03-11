class AddLastUpdateCheckToUser < ActiveRecord::Migration
  def self.up
    add_column :users, :last_version_check, :integer
  end

  def self.down
    remove_column :users, :last_version_check
  end
end
