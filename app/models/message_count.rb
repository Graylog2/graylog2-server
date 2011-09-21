class MessageCount
  include Mongoid::Document

  field :timestamp, :type => Integer
  field :total, :type => Integer
  field :streams, :type => Hash
  field :hosts, :type => Hash
  
  def self.counts_of_last_minutes(x, opts = {})
    return Array.new unless opts[:stream_id].blank?

    res = Array.new

    all(:conditions => { :timestamp => { "$gte" => x.minutes.ago.to_i }}).each do |c|
      res << { :timestamp => c.timestamp, :count => c.total }
    end

    return res
  end

end
