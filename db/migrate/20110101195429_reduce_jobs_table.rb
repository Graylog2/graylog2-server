class ReduceJobsTable < ActiveRecord::Migration
  def self.up
    remove_column :jobs, :id
    remove_column :jobs, :updated_at
  end

  def self.down
  end
end
