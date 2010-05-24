class CreateBlacklistedTerms < ActiveRecord::Migration
  def self.up
    create_table :blacklisted_terms do |t|
      t.string :term
      t.integer :blacklist_id 
      t.timestamps
    end
  end

  def self.down
    drop_table :blacklisted_terms
  end
end
