class AddRoleFieldToUser < ActiveRecord::Migration
  def self.up
    add_column :users, :role, :string
  end

  def self.down
    drop_column :users, :role
  end
end
