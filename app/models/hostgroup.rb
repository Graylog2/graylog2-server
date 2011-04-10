class Hostgroup

  include Mongoid::Document

  references_many :hostgroup_hosts do
    def simple
      @target.select { |host| host.simple? }
    end

    def regex
      @target.select { |host| host.regex? }
    end
  end

  validates_presence_of :name

  field :name, :type => String

  def self.find_by_id(_id)
    _id = $1 if /^([0-9a-f]+)-/ =~ _id
    first(:conditions => {:_id => BSON::ObjectId(_id)});
  end

  def all_conditions
    hostname_conditions | regex_conditions
  end

  def hostname_conditions(with_id = false)
    fetch_payload(:simple, with_id)
  end

  def regex_conditions(with_id = false)
    fetch_payload(:regex, with_id)
  end

  def to_param
    name.nil? ? "#{id}" : "#{id}-#{name.parameterize}"
  end

  private

  def fetch_payload(type, with_id = false)
    case type
    when :simple then
      hosts = hostgroup_hosts.simple
    when :regex then
      hosts = hostgroup_hosts.regex
    end

    if with_id then
      hosts.collect {|host| {:id => host.id, :value => host.hostname}}
    else
      hosts.collect &:to_condition
    end
  end
end
