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

  def self.all_with_blacklist page = 1, limit = LIMIT
    page = 1 if page.blank?

    conditions = Hash.new

    (blacklist = BlacklistedTerm.get_all_as_condition_hash).blank? ? nil : conditions[:message] = blacklist;

    return self.all :limit => limit, :order => "_id DESC", :conditions => conditions, :offset => self.get_offset(page)
  end

  def self.all_by_quickfilter filters, page = 1, limit = LIMIT
    page = 1 if page.blank?

    conditions = Hash.new

    RAILS_DEFAULT_LOGGER.debug "MAMA: " + filters.inspect

    unless filters.blank?
      # Message
      filters[:message].blank? ? nil : conditions[:message] = /#{Regexp.escape(filters[:message])}/

      # Facility
      filters[:facility].blank? ? nil : conditions[:facility] = filters[:facility].to_i

      # Severity
      filters[:severity].blank? ? nil : conditions[:level] = filters[:severity].to_i

      # Host
      filters[:host].blank? ? nil : conditions[:host] = filters[:host]
    end

    return self.all :limit => limit, :order => "_id DESC", :conditions => conditions, :offset => self.get_offset(page)
  end

  def self.all_of_stream stream_id, page = 1, conditions_only = false
    page = 1 if page.blank?
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
    (by_severity = Streamrule.get_severity_condition_hash(stream_id)).blank? ? nil : conditions[:level] = by_severity;

    # Return only conditions hash if requested.
    return conditions if conditions_only === true

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
