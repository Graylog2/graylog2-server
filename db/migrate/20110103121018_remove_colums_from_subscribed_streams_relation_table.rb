class RemoveColumsFromSubscribedStreamsRelationTable < ActiveRecord::Migration
  def self.up
    remove_column :subscribed_streams, :id
    remove_column :subscribed_streams, :created_at
    remove_column :subscribed_streams, :updated_at
  end

  def self.down
  end
end
