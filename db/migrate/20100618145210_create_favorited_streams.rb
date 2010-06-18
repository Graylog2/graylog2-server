class CreateFavoritedStreams < ActiveRecord::Migration
  def self.up
    create_table :favorited_streams do |t|
      t.integer :stream_id
      t.integer :user_id
      t.timestamps
    end
  end

  def self.down
    drop_table :favorited_streams
  end
end
