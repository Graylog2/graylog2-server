class CreateHostgroupHosts < ActiveRecord::Migration
  def self.up
    create_table :hostgroup_hosts do |t|
      t.integer :hostgroup_id
      t.integer :host_id
      t.timestamps
    end
  end

  def self.down
    drop_table :hostgroup_hosts
  end
end
