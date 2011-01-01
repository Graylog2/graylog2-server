class CreateFacilities < ActiveRecord::Migration
  def self.up
    create_table :facilities do |t|
      t.integer :number
      t.string :title
      t.timestamps
    end
  end

  def self.down
    drop_table :facilities
  end
end
