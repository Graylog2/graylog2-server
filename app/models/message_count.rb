class MessageCount
  include Mongoid::Document

  field :timestamp, :type => Integer
  field :total, :type => Integer
  field :streams, :type => Hash
  field :hosts, :type => Hash
  
  def self.total_count_of_last_minutes(x)
    return 0 if x == 0

    # XXX ELASTIC - this sucks. use map reduce here.
    total = 0
    counts_of_last_minutes(x).each do |c|
      total += c[:count]
    end

    return total
  end

  def self.counts_of_last_minutes(x, opts = {})
    res = Array.new

    all(:conditions => { :timestamp => { "$gte" => x.minutes.ago.to_i }}).each do |c|
      case(count_type(opts))
        when :stream then
          sc = c.streams[opts[:stream_id].to_s]
          sc.blank? ? count = 0 : count = sc
          res << { :timestamp => c.timestamp, :count => count }
        when :total then
          res << { :timestamp => c.timestamp, :count => c.total }
      end
    end

    return res
  end

  private
  def self.count_type(opts)
    return :stream if !opts[:stream_id].blank?
    return :host if !opts[:hostname].blank?
    return :total
  end

end
