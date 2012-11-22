class MessageCount
  include Mongoid::Document

  field :timestamp, :type => Integer
  field :total, :type => Integer
  field :streams, :type => Hash
  field :hosts, :type => Hash
  
  def self.total_count_of_last_minutes(x, opts = {})
    return 0 if x == 0

    # XXX ELASTIC - this sucks. use map reduce here.
    total = 0
    counts_of_last_minutes(x, opts).each do |timestamp, count|
      total += count
    end

    return total
  end

  def self.counts_of_last_minutes(x, opts = {})
    res = {}

    if x == 1
      to = Time.now.to_i
    else
      to = 1.minute.ago.to_i
    end

    conditions = { :timestamp => { "$gte" => x.minutes.ago.to_i, "$lt" => to+1 }}

    all(:conditions => conditions).distinct(:server_id).each do |node|
      all(:conditions => conditions.merge(:server_id => node)).each do |c|
        tsc = 0
        case(count_type(opts))
          when :stream then
            sc = c.streams[opts[:stream_id].to_s]
            sc.blank? ? count = 0 : count = sc
            tsc = count
          when :host then
            hc = c.hosts[Base64.encode64(opts[:hostname]).chop]
            hc.blank? ? count = 0 : count = hc
            tsc = count
          when :total then
            tsc = c.total
        end

        if res[c.timestamp].blank?
          # First node counts for this timestamp.
          res[c.timestamp] = tsc
        else
          # Another node already has stored counts. Add on top.
          res[c.timestamp] = res[c.timestamp]+tsc
        end
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
