class Message
  include MongoMapper::Document

  key :message, String
  key :date, String
  key :host, String
  key :level, Integer
  key :facility, Integer
  key :deleted, Boolean

  # GELF fields
  key :gelf, Boolean
  key :full_message, String
  key :type, Integer
  key :file, String
  key :line, Integer

  LIMIT = 100

  def self.get_conditions_from_date(timeframe)
    conditions = {}
    re = /^(from (.+)){0,1}?(to (.+))$/
    re2 = /^(from (.+))$/
    
    if (matches = (re.match(timeframe) or re2.match(timeframe)))
    
      from = matches[2]
      to = matches[4]
      
      conditions.merge!('$gt' => Chronic::parse(from).to_i) unless from.blank?
      conditions.merge!('$lt' => Chronic::parse(to).to_i) unless to.blank?
    end
    
    return conditions
  end
  
  def self.all_of_blacklist id, page = 1
    page = 1 if page.blank?
    
    conditions = Hash.new

    (blacklist = BlacklistedTerm.get_all_as_condition_hash(false, id)).blank? ? nil : conditions[:message] = blacklist;

    conditions[:deleted] = [false, nil]

    return self.all :limit => LIMIT, :order => "_id DESC", :conditions => conditions, :offset => self.get_offset(page), :fields => { :full_message => 0 }
  end

  def self.count_of_blacklist id
    conditions = Hash.new

    (blacklist = BlacklistedTerm.get_all_as_condition_hash(false, id)).blank? ? nil : conditions[:message] = blacklist;
    conditions[:deleted] = [false, nil]
    
    return self.count :conditions => conditions
  end

  def self.all_with_blacklist page = 1, limit = LIMIT
    page = 1 if page.blank?

    conditions = Hash.new

    (blacklist = BlacklistedTerm.get_all_as_condition_hash).blank? ? nil : conditions[:message] = blacklist;
    
    conditions[:deleted] = [false, nil]
    
    return self.all :limit => limit, :order => "_id DESC", :conditions => conditions, :offset => self.get_offset(page), :fields => { :full_message => 0 }
  end

  def self.all_by_quickfilter filters, page = 1, limit = LIMIT, conditions_only = false
    page = 1 if page.blank?

    conditions = self

    unless filters.blank?
      # Message
      conditions = (filters[:message].blank? ? conditions : conditions.where(:message => /#{Regexp.escape(filters[:message].strip)}/))

      # Time Frame
      conditions = (filters[:date].blank? ? conditions : conditions.where(:created_at => get_conditions_from_date(filters[:date])))
      
      #unless filters[:date_from].blank?
      #  from = Chronic::parse(filters[:date_from]).to_i
      #  conditions[:created_at] = {'$gt' => from}
      #end
      
      #unless filters[:date_to].blank?
      #  to = Chronic::parse(filters[:date_to]).to_i
      #  (conditions[:created_at] ||= {}).merge!({'$lt' => to})
      #end
      
      # Facility
      conditions = (filters[:facility].blank? ? conditions : conditions.where(:facility => filters[:facility].to_i))

      # Severity
      conditions = (filters[:severity].blank? ? conditions : conditions.where(:level => filters[:severity].to_i))

      # Host
      conditions = (filters[:host].blank? ? conditions : conditions.where(:host => filters[:host]))
    end

    conditions = conditions.where(:deleted => [false, nil])

    return (conditions_only ?
      conditions :
      conditions.limit(limit).offset(self.get_offset(page)).order("_id DESC").fields(:full_message => 0)
    )
  end

  def self.all_of_stream stream_id, page = 1, conditions_only = false
    throw "Missing stream_id" if stream_id.blank?
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
    
    # Filter by timeframe
    (by_timeframe = Streamrule.get_timeframe_condition_hash(stream_id)).blank? ? nil : conditions[:created_at] = by_timeframe;
    
    conditions[:deleted] = [false, nil]

    # Return only conditions hash if requested.
    return conditions if conditions_only === true

    return self.all :limit => LIMIT, :order => "_id DESC", :conditions => conditions, :offset => self.get_offset(page), :fields => { :full_message => 0 }
  end

  def self.count_stream stream_id
    conditions = self.all_of_stream stream_id, 0, true
    conditions[:deleted] = [false, nil]
    return self.count :conditions => conditions
  end

  def self.all_of_host host, page
    page = 1 if page.blank?
    return self.all :limit => LIMIT, :order => "_id DESC", :conditions => { :host => host, :deleted => [false, nil] }, :offset => self.get_offset(page), :fields => { :full_message => 0 }
  end
  
  def self.all_of_hostgroup hostgroup, page
    page = 1 if page.blank?

    return self.all :limit => LIMIT, :order => "_id DESC", :conditions => { :host => { "$in" => hostgroup.get_hostnames }, :deleted => [false, nil] }, :offset => self.get_offset(page), :fields => { :full_message => 0 }
  end

  def self.count_of_host host
    return self.count :conditions => { :host => host, :deleted => [false, nil] }
  end

  def self.count_of_hostgroup hostgroup
    return self.count :conditions => { :host => { "$in" => hostgroup.get_hostnames }, :deleted => [false, nil] }
  end

  def self.delete_all_of_host host
    self.delete_all :conditions => { :host => host, :deleted => [false, nil] }
  end

  def self.count_since x
    conditions = Hash.new

    (blacklist = BlacklistedTerm.get_all_as_condition_hash).blank? ? nil : conditions[:message] = blacklist;

    conditions[:created_at] = { '$gt' => (x).to_i }
    conditions[:deleted] = [false, nil]

    return self.count :conditions => conditions
  end

  def self.count_of_last_minutes x
    return self.count_since x.minutes.ago
  end

  def has_additional_fields
    return true if self.additional_fields.count > 0
    return false
  end
  
  def additional_fields
    additional = Array.new

    standard_fields = [ "created_at", "full_message", "line", "level", "_id", "deleted", "facility", "date", "type", "gelf", "file", "host", "message" ]

    self.keys.each do |key, value|
      next if standard_fields.include? key
      additional << { :key => key, :value => self[key] }
    end

    return additional
  end

  private

  def self.get_offset page
    if page.to_i <= 1
      return 0
    else
      return (LIMIT*(page.to_i-1))
    end
  end

end
