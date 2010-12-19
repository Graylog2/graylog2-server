class Stream < ActiveRecord::Base
  has_many :streamrules
  has_many :favoritedStreams, :dependent => :destroy
  has_many :subscribedStreams, :dependent => :destroy
  has_many :alertedStreams, :dependent => :destroy

  validates_presence_of :title

  validates_numericality_of :alarm_limit, :allow_nil => true
  validates_numericality_of :alarm_timespan, :allow_nil => true, :greater_than => 0

  def alerted?(user_id)
    AlertedStream.alerted?(self.id, user_id)
  end

  def subscribed?(user_id)
    SubscribedStream.subscribed?(self.id, user_id)
  end

  def favorited?(user_id)
    FavoritedStream.favorited?(self.id, user_id)
  end

  # giving back IDs because all_with_subscribers does too
  def self.all_with_enabled_alerts
    ids = Array.new
    self.find_all_by_alarm_active(true).each do |s|
      ids << s.id
    end
    return ids
  end

  def self.all_with_subscribers
    ids = Array.new
    self.joins(:subscribedStreams).each do |s|
      next if ids.include?(s.id)
      ids << s.id
    end
    return ids
  end

  def self.get_message_count(stream_id)
    conditions = Message.by_stream(stream_id).criteria
    return Message.count(:conditions => conditions)
  end

  def self.message_count_since(stream_id, since)
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

  end
