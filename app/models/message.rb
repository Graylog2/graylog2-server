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
end
