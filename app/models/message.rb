class Message

  LIMIT = 100
  ADDITIONAL_FIELD_SEPARATOR = '_'
  RESERVED_ADDITIONAL_FIELDS = %w(_type _index _version)

  @fields = [ :id, :message, :full_message, :created_at, :facility, :level, :host, :file, :line, :deleted, :streams ]
  @fields.each { |f| attr_accessor(f) }

  attr_accessor :plain

  # XXX ELASTIC: possibly do this with Tire.configuration.wrapper
  def self.parse_from_elastic(x)
    m = self.new
    m.plain = x
    @fields.each do |f|
      m.__send__(:"#{f}=", x.__send__(f))

      # XXX ELASTIC: zomg workaround
      # - __send__ creates a Rake::FileTask instead of a string. not sure why yet
      m.file = x[:file] if f == :file
    end

    return m
  end

  # you best lazy evaluate that
  def additional_fields
    return @additionals unless @additionals.blank?

    @additionals = Hash.new
    plain.to_hash.keys.each do |key|
      value = plain[key]
      if key[0,1] == ADDITIONAL_FIELD_SEPARATOR and !RESERVED_ADDITIONAL_FIELDS.include?(key.to_s)
        @additionals[key[1, key.length]] = value
      end
    end

    # XXX ELASTIC: sort alphabetically
  end

  # Overwriting the message getter. This always applies the filtering of filtered terms.
  def message
    FilteredTerm.apply(@message)
  end

  # Returns +created_at+ as +Time+ in request's timezone
  def created_at_time
    Time.zone.at(self.created_at)
  end

  def file_and_line
    self.file + (":#{@line}" unless @line.blank?).to_s
  end

  def additional_fields?
    self.additional_fields.count > 0
  end

  # XXX ELASTIC
  # This is controlled by general.yml. Disabling it gives great performance improve.
  #if Configuration.allow_deleting
  #  scope :not_deleted, where({ :deleted => false })
  #else
  #  scope :not_deleted, Hash.new
  #end

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
#    page = 1 if page.blank?
#
#    conditions = self
#
#    unless filters.blank?
#      unless filters[:message].blank?
#        message_conditions = Hash.new
#        message_conditions[:message] = Hash.new
#        message_conditions[:message]["$in"] = [/#{filters[:message].strip}/]
#
#        conditions = conditions.where(message_conditions)
#      end
#
#      # Time Frame
#      conditions = conditions.where(:created_at => get_conditions_from_date(filters[:date])) unless filters[:date].blank?
#
#      # Facility
#      conditions = conditions.where(:facility => filters[:facility]) unless filters[:facility].blank?
#
#      # Severity
#      if filters[:severity_above]
#        conditions = conditions.where(:level => { "$lte" => filters[:severity].to_i }) unless filters[:severity].blank?
#      else
#        conditions = conditions.where(:level => filters[:severity].to_i) unless filters[:severity].blank?
#      end
#
#      # Host (and hostgroup)
#      host_conditions = Array.new
#      host_conditions << filters[:host] unless filters[:host].blank?
#      unless filters[:hostgroup].blank?
#        hostgroup = Hostgroup.find(BSON::ObjectId(filters[:hostgroup]))
#        host_conditions = host_conditions | hostgroup.all_conditions
#      end
#      conditions = conditions.any_in(:host => host_conditions) unless host_conditions.size == 0
#
#      self.extract_additional_from_quickfilter(filters).each do |key, value|
#        conditions = conditions.where(key => value)
#      end
#    end
#
#    return conditions if conditions_only
#
#    conditions.default_scope.paginate(:page => page, :per_page => LIMIT)

    Array.new
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

  def self.recalculate_host_counts
    Host.all.each do |host|
      host.message_count = Message.where(:host => host.host, :deleted => false).count
      host.save
    end
  end

  def around(*args)
#    opts = {
#      :same_host => true,
#      :same_facility => false,
#      :same_level => false,
#      :order => :desc
#    }.merge(args.extract_options!)
#
#    qry = self.attributes.dup.delete_if { |k,v| !opts["same_#{k}".to_sym] }
#    nb = args.first || 100
#    terms = Blacklist.all_terms
#    from = self.class.default_scope.where(qry.merge(:_id => { "$lte" => self.id })).order({"_id" => "-1"}).skip(nb).first
#    return Array.new unless from
#    res = self.class.default_scope.where(qry.merge(:_id => {"$gte" => from.id})).limit(1 + nb.to_i * 2).order({"_id" => "1"}).to_a
#    res.reverse! if opts[:order] == :desc
#    res

    Array.new
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

  # XXX ELASTIC: test with reader user
  def accessable_for_user?(current_user)
    return true if current_user.role == "admin"

    # Check if any of the streams this message is filed in is accessible by the user
    streams.each do |stream_id|
      stream = Stream.find(stream_id)
      return true if stream.accessable_for_user?(current_user)
    end

    return false
  end

  def uniform_date_string
    d = Time.at(self.created_at)
    "#{d.year}-#{d.month}-#{d.day}"
  end

end
