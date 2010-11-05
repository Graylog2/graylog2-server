class CreateStreamUserRelation < ActiveRecord::Migration
  def self.up
    create_table :streams_users, :id => false do |t|
      t.references :stream
      t.references :user
    end
  end

  def self.down
    drop_table :streams_users
  end
end
