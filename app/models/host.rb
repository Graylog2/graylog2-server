class Host
  include Mongoid::Document

  key :host, String
  key :message_count, Float

  def self.all_of_group(hostgroup)
    return Host.all :conditions => { :host.in => hostgroup.all_conditions }
  end

end
