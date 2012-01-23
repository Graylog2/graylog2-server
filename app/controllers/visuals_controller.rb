class VisualsController < ApplicationController
  STANDARD_TIMESPAN_HOURS = 12

  def fetch
    r = Hash.new

    time = Benchmark.realtime do
      case params[:id]
        when "totalgraph" then
          r["data"] = calculate_totalgraph(params[:hours])
        when "streamgraph" then
          r["data"] = calculate_streamgraph(params[:stream_id], params[:hours])
        when "hostgraph" then
          r["data"] = calculate_hostgraph(params[:hostname], params[:hours])
        when "quickfiltergraph" then
          r["data"] = calculate_quickfiltergraph(params[:filters], params[:timespan_type], params[:timespan_value])
      end
    end

    r["time"] = sprintf("%#.2f", time*1000);

    render :js => r.to_json
  end

  private

  def calculate_totalgraph(hours = STANDARD_TIMESPAN_HOURS)
    MessageCount.counts_of_last_minutes(hours.to_i*60).collect do |c|
      [ (c[:timestamp].to_i+Time.now.utc_offset)*1000, c[:count] ]
    end
  end

  def calculate_streamgraph(stream_id, hours = STANDARD_TIMESPAN_HOURS)
    stream = Stream.find(stream_id)

    return Array.new if stream.streamrules.blank?

    MessageCount.counts_of_last_minutes(hours.to_i*60, :stream_id => stream.id).collect do |c|
      [ (c[:timestamp].to_i+Time.now.utc_offset)*1000, c[:count] ]
    end
  end

  def calculate_hostgraph(hostname, hours = STANDARD_TIMESPAN_HOURS)
    MessageCount.counts_of_last_minutes(hours.to_i*60, :hostname => hostname).collect do |c|
      [ (c[:timestamp].to_i+Time.now.utc_offset)*1000, c[:count] ]
    end
  end

  def calculate_quickfiltergraph(filters, timespan_type, timespan_value)
    raise "Invalid timespan type" if !valid_timespan_type?(timespan_type)
    MessageGateway.all_by_quickfilter(filters, 1, :date_histogram => true, :date_histogram_interval => timespan_type).collect do |c|
      [ c["time"].to_i, c["count"] ]
    end
  end

  def valid_timespan_type?(type)
    ["year", "month", "week", "day", "hour", "minute"].include?(type)
  end

  def escape(what)
    CGI.escapeHTML(what.to_s)
  end

end
