class Message
  include MongoMapper::Document

  set_database_name 'graylog2'

  key :message, String
  key :date, String
  key :host, String
  key :level, Integer
  key :facility, Integer

  def self.all_of_blacklist id
    conditions = Hash.new

    (blacklist = BlacklistedTerm.get_all_as_condition_hash(false, id)).blank? ? nil : conditions[:message] = blacklist;

    return self.all :limit => 100, :order => "_id DESC", :conditions => conditions
  end

  def self.all_with_blacklist limit = 100
    conditions = Hash.new

    (blacklist = BlacklistedTerm.get_all_as_condition_hash).blank? ? nil : conditions[:message] = blacklist;

    return self.all :limit => limit, :order => "_id DESC", :conditions => conditions
  end

  def self.all_of_stream stream_id
    conditions = Hash.new

    # Filter by message.
    (by_message = Streamrule.get_message_condition_hash(stream_id)).blank? ? nil : conditions[:message] = by_message;

    # Filter by host.
    (by_host = Streamrule.get_host_condition_hash(stream_id)).blank? ? nil : conditions[:host] = by_host;

    return self.all :limit => 100, :order => "_id DESC", :conditions => conditions
  end

  def self.all_of_host host
    return self.all :limit => 100, :order => "_id DESC", :conditions => { "host" => host }
  end

  def self.delete_all_of_host host
    self.delete_all :conditions => { "host" => host }
  end

end
