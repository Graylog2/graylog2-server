class Message

  LIMIT = 100
  ADDITIONAL_FIELD_SEPARATOR = '_'
  RESERVED_ADDITIONAL_FIELDS = %w(_type _index _version _score )

  @fields = [ :id, :message, :full_message, :created_at, :facility, :level, :host, :file, :line, :deleted, :streams ]
  @fields.each { |f| attr_accessor(f) }

  attr_accessor :source_index, :plain, :total_result_count

  def self.parse_from_elastic(x)
    m = self.new

    m.plain = x
    m.total_result_count = x.total rescue nil
    m.source_index = x[:_index]

    @fields.each do |f|
      # XXX ELASTIC: zomg workaround
      # - __send__ creates a Rake::FileTask instead of a string. not sure why yet
      if f != :file
        m.__send__(:"#{f}=", x.__send__(f)) rescue nil
      else
        m.file = x[:file] rescue nil
      end
    end

    return m
  end

  # XXX ELASTIC ZZZZOMMMGG, mega-duplication :/
  def self.parse_from_hash(x)
    m = self.new

    m.plain = x
    m.total_result_count = x.total rescue nil
    @fields.each do |f|
      m.__send__(:"#{f}=", x[f]) rescue nil
    end

    return m
  end

  # you best lazy evaluate that
  def additional_fields
    return @additionals unless @additionals.blank?

    @additionals = Hash.new
    plain.to_hash.keys.each do |key|
      key = key.to_s
      value = plain[key.to_sym]
      if key[0,1] == ADDITIONAL_FIELD_SEPARATOR and !RESERVED_ADDITIONAL_FIELDS.include?(key.to_s)
        @additionals[key[1, key.length]] = value
      end
    end

    return @additionals

    # XXX ELASTIC: sort alphabetically
  end

  # Overwriting the message getter. This always applies the filtering of filtered terms.
  def message
    FilteredTerm.apply(@message) || ''
  end

  # Returns +created_at+ as +Time+ in request's timezone
  def created_at_time
    Time.zone.at(self.created_at)
  end

  def file_and_line
    (self.file + (":#{self.line}" unless self.line.blank?).to_s unless self.file.blank?) || ''
  end

  def additional_fields?
    self.additional_fields.count > 0
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
  rescue
    # for example range errors for too long timestamps
    return "INVALID"
  end

  def to_param
    self.id.to_s
  end

  def as_json(options={})
    self.plain.as_json
  end
end
