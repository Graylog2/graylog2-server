class RemoveIdFromSettings < ActiveRecord::Migration
  def self.up
    remove_column :settings, :id
  end

  def self.down
  end
end
