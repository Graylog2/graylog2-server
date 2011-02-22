class CreateStreamcategories < ActiveRecord::Migration
  def self.up
    create_table :streamcategories do |t|
      t.string :title
      t.integer :sorting
      t.timestamps
    end

    add_column :streams, :streamcategory_id, :integer
  end

  def self.down
    drop_table :streamcategories
  end
end
