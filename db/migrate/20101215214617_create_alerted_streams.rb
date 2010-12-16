class CreateAlertedStreams < ActiveRecord::Migration
  def self.up
    add_column :streams, :last_alert_check, :datetime

    create_table :alerted_streams do |t|
      t.integer :stream_id
      t.integer :user_id
      t.timestamps
    end
  end

  def self.down
    remove_column :streams, :last_alert_check
    drop_table :alerted_streams
  end
end
