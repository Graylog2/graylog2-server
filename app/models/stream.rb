class Stream < ActiveRecord::Base
  has_many :streamrules

  validates_presence_of :title

  def self.get_message_count stream_id
    conditions = Message.all_of_stream stream_id, nil, true
    return Message.count(:conditions => conditions)
  end

  def self.get_distinct_hosts stream_id
    conditions = Message.all_of_stream stream_id, nil, true
    return Message.collection.distinct(:host, conditions)
  end

  def self.get_count_by_host stream_id, host
    conditions = Message.all_of_stream stream_id, nil, true
    conditions[:host] = host
    return Message.count(:conditions => conditions).to_s
  end
end
