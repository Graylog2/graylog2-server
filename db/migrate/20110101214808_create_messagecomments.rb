class CreateMessagecomments < ActiveRecord::Migration
  def self.up
    create_table :messagecomments do |t|
      t.string :title
      t.text :comment
      t.string :match
      t.integer :user_id
      t.timestamps
    end
  end

  def self.down
    drop_table :messagecomments
  end
end
