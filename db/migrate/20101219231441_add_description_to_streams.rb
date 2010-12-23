class AddDescriptionToStreams < ActiveRecord::Migration
  def self.up
    add_column :streams, :description, :text
  end

  def self.down
    remove_column :streams, :description
  end
end
