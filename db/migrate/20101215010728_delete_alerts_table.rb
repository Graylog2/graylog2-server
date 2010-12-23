class DeleteAlertsTable < ActiveRecord::Migration
  def self.up
   drop_table :alerts
   add_column :streams, :last_subscription_check, :datetime
  end

  def self.down
  end
end
