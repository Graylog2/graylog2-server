class CreateStreams < ActiveRecord::Migration
  def self.up
    create_table :streams do |t|
      t.string :title
      t.string :filter_host
      t.string :filter_message
      t.integer :filter_severity
      t.integer :filter_level
      t.timestamps
    end
  end

  def self.down
    drop_table :streams
  end
end
