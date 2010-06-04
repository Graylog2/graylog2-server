class CreateStreamrules < ActiveRecord::Migration
  def self.up
    create_table :streamrules do |t|
      t.integer :stream_id
      t.integer :rule_type
      t.string :value
      t.timestamps
    end
  end

  def self.down
    drop_table :streamrules
  end
end
