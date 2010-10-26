class RenameHostgrouphostsHostIdToName < ActiveRecord::Migration
  def self.up
    rename_column :hostgroup_hosts, :host_id, :hostname
  end

  def self.down
    rename_column :hostgroup_hosts, :hostname, :host_id
  end
end
