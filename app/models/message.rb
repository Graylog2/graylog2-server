class Message
  include Mongoid::Document

  FIELDS = %w(message date host level facility deleted  gelf full_message type file line version timestamp created_at)
  SPECIAL_FIELDS = %w(_id)

  key :version, Integer
  key :timestamp, Integer
  key :message, String
  key :date, String
  key :host, String
  key :level, Integer
  key :facility, Object
  key :deleted, Boolean
  key :created_at, Integer
  key :gelf, Boolean
  key :full_message, String
  key :type, Integer
  key :file, String
  key :line, Integer

  LIMIT = 100

  # This is controlled by general.yml. Disabling it gives great performance improve.
  if Configuration.allow_deleting
    scope :not_deleted, where({ :deleted => false })
  else
    scope :not_deleted, Hash.new
  end

  scope :page, lambda {|number| skip(self.get_offset(number))}
  scope :default_scope, order_by({"_id" => "-1"}).not_deleted.limit(LIMIT)
  scope :time_range, lambda {|from, to| where(:created_at => {"$gte" => from}).where(:created_at => {"$lte" => to})}

  def self.find_by_id(_id)
    where(:_id => BSON::ObjectId(_id)).first
  end

  def timestamp
    Time.at(created_at)
  end

  # Overwriting the message getter. This always applies the filtering of
  # filtered terms.
  def message
    if FilteredTerm.exists?
      msg = read_attribute(:message)
      FilteredTerm.all.each do |t|
        next if msg.blank? or t.term.blank?
        begin
          msg[/#{t.term}/] = "[FILTERED]"
        rescue => e
          Rails.logger.warn "Skipping filtered term: #{e}"
          next
        end
      end

      return msg
    end

    # No filtered terms set.
    read_attribute(:message)
  end

  def self.all_paginated page = 1, limit = LIMIT
    page = 1 if page.blank?

    default_scope.paginate(:page => page)
  end

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

  def self.all_by_quickfilter filters, page = 1, limit = LIMIT, conditions_only = false
    page = 1 if page.blank?

    conditions = self

    unless filters.blank?
      unless filters[:message].blank?
        message_conditions = Hash.new
        message_conditions[:message] = Hash.new
        message_conditions[:message]["$in"] = [/#{filters[:message].strip}/]

        conditions = conditions.where(message_conditions)
      end

      # Time Frame
      conditions = conditions.where(:created_at => get_conditions_from_date(filters[:date])) unless filters[:date].blank?

      # Facility
      conditions = conditions.where(:facility => filters[:facility]) unless filters[:facility].blank?

      # Severity
      if filters[:severity_above]
        conditions = conditions.where(:level => { "$lte" => filters[:severity].to_i }) unless filters[:severity].blank?
      else
        conditions = conditions.where(:level => filters[:severity].to_i) unless filters[:severity].blank?
      end

      # Host
      conditions = conditions.where(:host => filters[:host]) unless filters[:host].blank?

      self.extract_additional_from_quickfilter(filters).each do |key, value|
        conditions = conditions.where(key => value)
      end
    end

    return conditions if conditions_only

    conditions.default_scope.limit(LIMIT).paginate(:page => page)
  end

  def self.extract_additional_from_quickfilter(filters)
    return Hash.new if filters[:additional].blank? or filters[:additional][:keys].blank? or filters[:additional][:values].blank?

    ret = Hash.new
    i = 0
    filters[:additional][:keys].each do |key|
      next if key.blank? or filters[:additional][:values][i].blank?
      ret["_#{key}".to_sym] = filters[:additional][:values][i]
      i += 1
    end

    return ret
  end

  def self.by_stream(stream_id)
    not_deleted.where({:streams => stream_id})
  end

  def self.all_of_stream_since(stream_id, since)
    by_stream(stream_id).where(:created_at => {'$gt' => since.to_i}).default_scope.all
  end

  def self.count_stream stream_id
    return by_stream(stream_id).count
  end

  def self.all_of_stream_in_range(stream_id, page, from, to)
    page = 1 if page.blank?

    #by_stream(stream_id).default_scope.where(:created_at => {"$gte" => from}).where(:created_at => {"$lte" => to}).paginate(:page => page)
    by_stream(stream_id).default_scope.time_range(from, to).paginate(:page => page)
  end

  def self.all_in_range(page, from, to)
    page = 1 if page.blank?

    default_scope.time_range(from, to).paginate(:page => page)
  end

  def self.count_all_of_stream_in_range(stream_id, from, to)
    terms = Blacklist.all_terms
    #by_stream(stream_id).where(:created_at => {"$gte" => from}).where(:created_at => {"$lte" => to}).count
    by_stream(stream_id).time_range(from, to).count
  end

  def self.count_all_in_range(from, to)
    time_range(from, to).count
  end

  def self.counts_of_last_minutes(minutes)
    res = Array.new
    minutes.times do |m|
      m += 1 # Offset by one because we don't want to start with the current minute.
      t = m.minutes.ago

      # Get first second of minute.
      t -= t.sec

      # Try to read from cache.
      obj = { :type => :graphvalue, :allhosts => true, :minute => t.to_i }
      c = Rails.cache.read(obj)

      if c == nil
        # Cache miss. Perform counting and add to cache.
        c = Message.time_range(t.to_i, (t+60).to_i).count
        Rails.cache.write(obj, c)
      end

      res << { :minute => t, :count => c}
    end

    return res.reverse
  end

  def self.stream_counts_of_last_minutes(stream_id, minutes)
    stream = Stream.find(stream_id)
    return [{:count => 0}, {:count =>0}] if stream.blank? or stream.streamrules.blank? # sparklines needs at least two elements..

    res = Array.new
    minutes.times do |m|
      m += 1 # Offset by one because we don't want to start with the current minute.
      t = m.minutes.ago

      # Get first second of minute.
      t -= t.sec

      # Try to read from cache.
      obj = { :type => :graphvalue, :rules => stream.rule_hash, :stream_id => stream_id, :minute => t.to_i }
      c = Rails.cache.read(obj)

      if c == nil
        # Cache miss. Perform counting and add to cache.
        c = Message.by_stream(stream_id).time_range(t.to_i, (t+60).to_i).count
        Rails.cache.write(obj, c)
      end

      res << { :minute => t, :count => c}
    end

    return res.reverse
  end

  def self.all_of_host host, page
    page = 1 if page.blank?
    where(:host => host).default_scope.paginate(:page => page)
  end

  def self.all_of_hostgroup hostgroup, page
    page = 1 if page.blank?

    return where(:host.in => hostgroup.all_conditions ).default_scope #.paginate(:page => page)
  end

  def self.count_of_hostgroup hostgroup
    where(:host.in => hostgroup.all_conditions).not_deleted.count
  end

  def self.delete_all_of_host host
    Message.set({:host => host}, :deleted => true )
  end

  def self.count_since x
    if x.to_i > 0
      conditions = not_deleted.where(:created_at.gt => x.to_i)
    else
      conditions = not_deleted
    end

    conditions.count
  end

  def self.count_of_last_minutes x
    return self.count_since x.minutes.ago
  end

  def has_additional_fields
    return true if self.additional_fields.count > 0
    return false
  end

  def additional_fields
    additional = []
    self.attributes.each do |key, value|
      next if SPECIAL_FIELDS.include?(key) or key.length <= 1 or key.at(0) != "_"
      # Cut off underscore if there is one. (There is one if it's coming directly from MongoDB)
      cut_key = (key.at(0) == "_" ? key[1..key.length] : key)
      additional << { :key => cut_key, :value => self[key] }
    end
    return additional
  end

  def self.recalculate_host_counts
    Host.all.each do |host|
      host.message_count = Message.where(:host => host.host, :deleted => false).count
      host.save
    end
  end

  def around(*args)
    opts = {
      :same_host => true,
      :same_facility => false,
      :same_level => false,
      :order => :desc
    }.merge(args.extract_options!)

    qry = self.attributes.dup.delete_if { |k,v| !opts["same_#{k}".to_sym] }
    nb = args.first || 100
    terms = Blacklist.all_terms
    from = self.class.default_scope.where(qry.merge(:_id => { "$lte" => self.id })).order({"_id" => "-1"}).skip(nb).first
    return Array.new unless from
    res = self.class.default_scope.where(qry.merge(:_id => {"$gte" => from.id})).limit(1 + nb.to_i * 2).order({"_id" => "1"}).to_a
    res.reverse! if opts[:order] == :desc
    res
  end

  # Workaround for migration problems. #WEBINTERFACE-24
  def referenced_streams
    ret_streams = Array.new
    streams.each do |stream_id|
      begin
        stream = Stream.find_by_id(stream_id.to_s)
        ret_streams << stream unless stream.blank?
      rescue
        next
      end
    end

    return ret_streams
  end

  def time
    Time.at(self.created_at) rescue nil
  end

  def accessable_for_user?(current_user)
    return true if current_user.role == "admin"

    # Check if any of the streams this message is filed in is accessible by the user
    self.streams.each do |stream_id|
      stream = Stream.find(stream_id)
      return true if stream.accessable_for_user?(current_user)
    end

    return false
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
