class StreamAlert < ActiveRecord::Migration
  def self.up
    add_column "streams", "alertable", :boolean, :default => false
  end

  def self.down
    remove_column "streams", "alertable"
  end
end
