class ChangeHostgrouphostsHostnameToString < ActiveRecord::Migration
  def self.up
    change_column :hostgroup_hosts, :hostname, :string
  end

  def self.down
    change_column :hostgroup_hosts, :hostname, :integer
  end
end
