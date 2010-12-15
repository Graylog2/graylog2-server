class NewStreamSubscriptions < ActiveRecord::Migration
  def self.up
    remove_column :streams, :alertable
    
    create_table :subscribed_streams do |t|
      t.integer :stream_id
      t.integer :user_id
      t.timestamps
    end
  end

  def self.down
    add_column :streams, :alertable, :boolean

    remove_table :subscribed_streama
  end
end
