class Hostgroup < ActiveRecord::Base
  has_many :hostgroup_hosts

  def message_count
    count = 0

    self.hostgroup_hosts.each do |hostdescription|
      host = Host.find_by_host hostdescription.hostname
      next if host.blank?
      count += host.message_count
    end

    return count
  end

  def get_hostnames
    names = Array.new

    self.hostgroup_hosts.each do |hostdescription|
      host = Host.find_by_host hostdescription.hostname
      next if host.blank?
      names << host.host
    end

    return names
  end
end
