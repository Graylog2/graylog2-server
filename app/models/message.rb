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
    by_message = Streamrule.get_message_condition_array stream_id
    if by_message.blank?
      # No messages to filter. Only add blacklist.
      (blacklist = BlacklistedTerm.get_all_as_condition_hash).blank? ? nil : conditions[:message] = blacklist;
    else
      # There are messages to filter. Combine with blacklist.
      blacklist = BlacklistedTerm.get_all_as_condition_hash true, nil, true
      if blacklist.blank?
        # Nothing on the blacklist. Just add message filter if exists.
        by_message.blank? ? nil : conditions[:message] = { '$in' => by_message }
      else
        # Blacklist and message filter set. Combine both.
        conditions[:message] = { '$nin' => blacklist, '$in' => by_message }
      end
    end

    # Filter by host.
    (by_host = Streamrule.get_host_condition_hash(stream_id)).blank? ? nil : conditions[:host] = by_host;

    # Filter by facility.
    (by_facility = Streamrule.get_facility_condition_hash(stream_id)).blank? ? nil : conditions[:facility] = by_facility;

     # Filter by severity.
    (by_severity = Streamrule.get_severity_condition_hash(stream_id)).blank? ? nil : conditions[:severity] = by_severity;

    RAILS_DEFAULT_LOGGER.info conditions[:message].inspect

    return self.all :limit => 100, :order => "_id DESC", :conditions => conditions
  end

end
