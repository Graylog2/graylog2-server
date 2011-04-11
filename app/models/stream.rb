class Stream
  include Mongoid::Document
  include Mongoid::Timestamps

  embeds_many :streamrules
  embeds_many :forwarders

  has_and_belongs_to_many :users, :inverse_of => :streams
  has_and_belongs_to_many :favorited_streams, :class_name => "User", :inverse_of => :favorite_streams
  has_and_belongs_to_many :subscribers,       :class_name => "User", :inverse_of => :subscribed_streams

  referenced_in :streamcategory

  validates_presence_of :title
  validates_numericality_of :alarm_limit, :allow_nil => true
  validates_numericality_of :alarm_timespan, :allow_nil => true, :greater_than => 0

  field :title, :type => String
  field :alarm_limit, :type => Integer
  field :alarm_timespan, :type => Integer
  field :description, :type => Integer
  field :created_at, :type => DateTime
  field :updated_at, :type => DateTime
  field :alarm_force, :type => Boolean
  field :last_subscription_check, :type => Integer
  field :last_alarm_check, :type => Integer
  field :alarm_active, :type => Boolean

  def self.find_by_id(_id)
    _id = $1 if /^([0-9a-f]+)-/ =~ _id
    first(:conditions => { :_id => BSON::ObjectId(_id)})
  end

  def alerted?(user)
    AlertedStream.alerted?(self.id, user.id)
  end

  def subscribed?(user)
    !subscribers.nil? and subscribers.include?(user)
  end

  def favorited?(user_id)
    !favorited_streams.nil? and favorited_streams.include? user_id
  end

  def to_param
    "#{id}-#{title.parameterize}"
  end

  # giving back IDs because all_with_subscribers does too
  def self.all_with_enabled_alerts
    all.where({:alarm_active => true}).collect &:id
  end

  def self.get_message_count(stream_id)
    return 0 if Stream.find(stream_id).streamrules.blank?
    Message.count(:conditions => Message.by_stream(stream_id).criteria)
  end

  def message_count
    Stream.get_message_count(self.id)
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
    return 0 if Stream.find(stream_id).streamrules.blank?
    Message.by_stream(stream_id).where(:created_at.gt => since.to_i).count
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
    uids = AlertedStream.where(:stream_id => id)

    users = Array.new
    uids.each do |uid|
      user = User.find(uid.user_id)
      users << user unless user.blank?
    end

    return users
  end

  def accessable_for_user?(user)
    return true if user.role == "admin"

    allowed_streams = user.streams.collect { |s| s.id.to_s }
    return false unless allowed_streams.include?(self.id.to_s)

    return true
  end
end
