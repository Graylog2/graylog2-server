class Message
  include MongoMapper::Document

  set_database_name 'graylog2'

  key :message, String
  key :date, String
  key :host, String
  key :level, Integer
  key :facility, Integer

  LIMIT = 100

  def self.all_of_blacklist id, page = 1
    page = 1 if page.blank?
    
    conditions = Hash.new

    (blacklist = BlacklistedTerm.get_all_as_condition_hash(false, id)).blank? ? nil : conditions[:message] = blacklist;

    return self.all :limit => LIMIT, :order => "_id DESC", :conditions => conditions, :offset => self.get_offset(page)
  end

  def self.all_with_blacklist page = 1
    page = 1 if page.blank?

    conditions = Hash.new

    (blacklist = BlacklistedTerm.get_all_as_condition_hash).blank? ? nil : conditions[:message] = blacklist;

    return self.all :limit => LIMIT, :order => "_id DESC", :conditions => conditions, :offset => self.get_offset(page)
  end

  def self.all_of_stream stream_id, page = 1
    page = 1 if page.blank?
    conditions = Hash.new

    # Filter by message.
    (by_message = Streamrule.get_message_condition_hash(stream_id)).blank? ? nil : conditions[:message] = by_message;

    # Filter by host.
    (by_host = Streamrule.get_host_condition_hash(stream_id)).blank? ? nil : conditions[:host] = by_host;

    return self.all :limit => LIMIT, :order => "_id DESC", :conditions => conditions, :offset => self.get_offset(page)
  end

  def self.all_of_host host, page
    page = 1 if page.blank?
    return self.all :limit => LIMIT, :order => "_id DESC", :conditions => { "host" => host }, :offset => self.get_offset(page)
  end

  def self.delete_all_of_host host
    self.delete_all :conditions => { "host" => host }
  end

  private

  def self.get_offset page
    if page.to_i <= 1
      return 0
    else
      return (LIMIT*page.to_i)
    end
  end


end
