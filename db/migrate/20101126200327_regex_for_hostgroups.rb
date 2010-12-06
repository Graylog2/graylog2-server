class RegexForHostgroups < ActiveRecord::Migration
  def self.up
    add_column :hostgroup_hosts, :ruletype, :integer
  end

  def self.down
    remove_column :hostgroup_hosts, :ruletype
  end
end
