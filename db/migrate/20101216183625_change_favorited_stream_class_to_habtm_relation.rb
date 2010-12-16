class ChangeFavoritedStreamClassToHabtmRelation < ActiveRecord::Migration
  def self.up
    remove_column :favorited_streams, :id
    remove_column :favorited_streams, :created_at
    remove_column :favorited_streams, :updated_at
    rename_table :favorited_streams, :favorite_streams
  end

  def self.down
    rename_table :favorite_streams, :favorited_streams
    add_column :favorited_streams, :id, :integer
    add_column :favorited_streams, :created_at, :timestamp
    add_column :favorited_streams, :updated_at, :timestamp
  end
end
