class CreateFilteredTerms < ActiveRecord::Migration
  def self.up
    create_table :filtered_terms do |t|
      t.string :term
      t.timestamps
    end
  end

  def self.down
    drop_table :filtered_terms
  end
end
