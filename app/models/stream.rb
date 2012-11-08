class Stream
  include Mongoid::Document
  include Mongoid::Timestamps

  embeds_many :streamrules
  embeds_many :forwarders

  has_and_belongs_to_many :users, :inverse_of => :streams
  has_and_belongs_to_many :favorited_streams, :class_name => "User", :inverse_of => :favorite_streams

  referenced_in :streamcategory

  validates_presence_of :title
  validates_numericality_of :alarm_limit, :allow_nil => true
  validates_numericality_of :alarm_timespan, :allow_nil => true, :greater_than => 0
  validates_length_of :shortname, :maximum => 23
  validates_uniqueness_of :shortname, :allow_nil => true
  validates_format_of :shortname, :with => /^[A-Za-z0-9_]+$/, :allow_nil => true
  validate :valid_regexes

  field :title, :type => String
  field :alarm_limit, :type => Integer
  field :alarm_timespan, :type => Integer
  field :description, :type => Integer
  field :created_at, :type => DateTime
  field :updated_at, :type => DateTime
  field :alarm_force, :type => Boolean
  field :alarm_active, :type => Boolean
  field :alarm_period, :type => Integer
  field :disabled, :type => Boolean
  field :additional_columns, :type => Array, :default => []
  field :shortname, :type => String
  field :related_streams_matcher, :type => String
  field :alarm_callbacks, :type => Array, :default => []

  def self.find_by_id(_id)
    _id = $1 if /^([0-9a-f]+)-/ =~ _id
    first(:conditions => { :_id => BSON::ObjectId(_id)})
  end
  
  def self.find_by_shortname(shortname)
    first(:conditions => { :shortname => shortname})
  end

  def self.find_by_id_or_name(id_or_name)
    begin
      return self.find_by_id id_or_name
    rescue 
      return self.find_by_shortname id_or_name
    end
    return nil
  end

  def title_possibly_disabled
    disabled ? title + " (disabled)" : title if title
  end

  def alerted?(user)
    AlertedStream.alerted?(self.id, user.id)
  end

  def favorited?(user_id)
    !favorited_streams.nil? and favorited_streams.include? user_id
  end

  def to_param
    title.blank? ? id.to_s : "#{id}-#{title.parameterize}"
  end

  def self.all_with_enabled_alerts
    all.where({:alarm_active => true}).collect &:id
  end

  def message_count_since(since)
    return Stream.message_count_since(id, since)
  end

  def rule_hash
    hashme = String.new
    self.streamrules.each do |rule|
      hashme += rule.rule_type.to_s + rule.value
    end

    return Digest::MD5.hexdigest(hashme)
  end

  def self.message_count_since(stream_id, since)
    minutes_ago = (Time.now.to_f-since.to_f)/60 rescue 0
    MessageCount.total_count_of_last_minutes(minutes_ago, :stream_id => stream_id)
  end

  def self.get_distinct_hosts(stream_id)
    conditions = Message.by_stream(stream_id).criteria
    return Message.collection.distinct(:host, conditions)
  end

  def self.get_count_by_host(stream_id, host)
    conditions = Message.by_stream(stream_id).criteria
    conditions[:host] = host
    return Message.count(:conditions => conditions).to_s
  end

  def all_users_with_favorite
    favorited_streams
  end

  def all_users_with_alarm
    alerts = AlertedStream.where(:stream_id => id)
    alerts.collect &:user
  end

  def accessable_for_user?(user)
    return true if user.role == "admin"

    allowed_streams = user.streams.collect { |s| s.id.to_s }
    allowed_streams.include?(self.id.to_s)
  end

  def related_streams
    return Array.new if self.related_streams_matcher.blank?
    Stream.where(:title => /#{self.related_streams_matcher}/, :_id => { "$ne" => self.id }).all
  end

  def alarm_status(user)
    return :disabled if !self.alarm_active or self.alarm_limit.blank? or self.alarm_timespan.blank?

    unless alarm_force
      return :disabled if !alerted?(user)
    end

    stream_count = self.message_count_since(self.alarm_timespan.minutes.ago.to_f)
    return stream_count > self.alarm_limit ? :alarm : :no_alarm
  end

  def alarm_callback_active?(typeclass)
    if !alarm_callbacks.blank? and alarm_callbacks.is_a?(Array)
      return true if alarm_callbacks.include?(typeclass)
    end

    return false
  end

  def set_alarm_callback_active(typeclass, what)
    actives = alarm_callbacks
    if what == true
      # Add to list.
      actives << typeclass
    else
      # Remove from list.
      actives.delete(typeclass)
    end

    # Update
    alarm_callbacks = actives
  end

  private
  def valid_regexes
    unless self.related_streams_matcher.blank?
      begin
        Regexp.new(/#{self.related_streams_matcher}/)
      rescue RegexpError
        errors.add(:related_streams_matcher, "Invalid regular expression")
      end
    end
  end
end
