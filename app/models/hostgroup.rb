class Hostgroup < ActiveRecord::Base
  has_many :hostgroup_hosts, :dependent => :delete_all

  validates_presence_of :name

  def all_conditions
    hostname_conditions | regex_conditions
  end

  def hostname_conditions(with_id = false)
    fetch_payload(HostgroupHost::TYPE_SIMPLE, with_id)
  end

  def regex_conditions(with_id = false)
    fetch_payload(HostgroupHost::TYPE_REGEX, with_id)
  end

  private

  def fetch_payload(type, with_id = false)
    p = Array.new

    self.hostgroup_hosts.each do |hostdescription|
      next if hostdescription.ruletype != type

      # Skip if host does not exist anymore.
      if type == HostgroupHost::TYPE_SIMPLE
        host = Host.find_by_host hostdescription.hostname
        next if host.blank?
      end

      case type
        when HostgroupHost::TYPE_SIMPLE then
          if with_id
            p << { :id => hostdescription.id, :value => hostdescription.hostname }
          else
            p << hostdescription.hostname
          end
        when HostgroupHost::TYPE_REGEX then
          if with_id
            p << { :id => hostdescription.id, :value => /#{hostdescription.hostname}/ }
          else
            p << /#{hostdescription.hostname}/ 
          end
      end
    end
    
    return p
end
end
