class Stream < ActiveRecord::Base
  has_many :streamrules
  has_and_belongs_to_many :favoritedStreams, :join_table => "favorite_streams", :class_name => "User"
  has_and_belongs_to_many :users
  #has_many :subscribedStreams, :dependent => :destroy
  has_many :alertedStreams, :dependent => :destroy
  has_and_belongs_to_many :subscribers, :join_table => "subscribed_streams", :class_name => "User"

  belongs_to :streamcategory

  validates_presence_of :title

  validates_numericality_of :alarm_limit, :allow_nil => true
  validates_numericality_of :alarm_timespan, :allow_nil => true, :greater_than => 0

  def alerted?(user_id)
    AlertedStream.alerted?(self.id, user_id)
  end

  def subscribed?(user)
    if user.is_a?(User)
      subscribers.include?(user)
    else
      subscriber_ids.include? user
    end
  end

  def favorited?(user_id)
    favoritedStreams.include? user_id
  end
  
  def to_param
    "#{id}-#{title.parameterize}"
  end

  # giving back IDs because all_with_subscribers does too
  def self.all_with_enabled_alerts
    find_all_by_alarm_active(true).collect &:id
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

  def last_message
    return nil if self.streamrules.blank?
    Message.first(:conditions => Message.by_stream(self.id).criteria, :order => "created_at DESC")
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
    conditions = Message.by_stream(stream_id).criteria
    conditions[:created_at] = { "$gte" => since }
    return Message.count(:conditions => conditions)
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
    favoritedStreams
  end

  def all_users_with_alarm
    uids = AlertedStream.find_all_by_stream_id(id)
    
    users = Array.new
    uids.each do |uid|
      user = User.find(uid.user_id)
      users << user unless user.blank?
    end
    
    return users
  end

end
