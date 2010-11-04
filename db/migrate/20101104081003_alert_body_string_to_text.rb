class AlertBodyStringToText < ActiveRecord::Migration
  def self.up
    change_column "alerts", "body", :text
  end

  def self.down
    change_column "alerts", "body", :string
  end
end
