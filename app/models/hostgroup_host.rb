class HostgroupHost < ActiveRecord::Base

  validates_presence_of :hostgroup_id, :ruletype, :hostname

  TYPE_SIMPLE = 0
  TYPE_REGEX = 1

end
