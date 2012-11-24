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
        when "resultgraph" then
          if params[:filters]
            r["data"] = calculate_quickfiltergraph(
              params[:filters],
              params[:interval],
              params[:hostname],
              params[:stream_id]
            )
          elsif params[:query]
            stream = Stream.find(params[:stream_id]) if !params[:stream_id].blank?
            host = Host.find(:first, :conditions => {:host=> params[:hostname]}) if !params[:hostname].blank?

            r["data"] = calculate_querygraph(params[:query], params[:interval], host, stream, params[:since])
          end
      end
    end

    r["time"] = sprintf("%#.2f", time*1000);

    render :js => r.to_json
  end

  private

  def calculate_totalgraph(hours = STANDARD_TIMESPAN_HOURS)
    MessageCount.counts_of_last_minutes(hours.to_i*60).collect do |timestamp, count|
      [ (timestamp.to_i+Time.now.utc_offset)*1000, count ]
    end
  end

  def calculate_streamgraph(stream_id, hours = STANDARD_TIMESPAN_HOURS)
    stream = Stream.find(stream_id)

    return Array.new if stream.streamrules.blank?

    MessageCount.counts_of_last_minutes(hours.to_i*60, :stream_id => stream.id).collect do |timestamp, count|
      [ (timestamp.to_i+Time.now.utc_offset)*1000, count ]
    end
  end

  def calculate_hostgraph(hostname, hours = STANDARD_TIMESPAN_HOURS)
    MessageCount.counts_of_last_minutes(hours.to_i*60, :hostname => hostname).collect do |timestamp, count|
      [ (timestamp.to_i+Time.now.utc_offset)*1000, count ]
    end
  end

  def calculate_quickfiltergraph(filters, interval, hostname, stream_id)
    raise "Invalid interval" unless valid_interval?(interval)

    MessageGateway.all_by_quickfilter(filters, 1, :date_histogram => true, :date_histogram_interval => interval, :hostname => hostname, :stream_id => stream_id).collect do |c|
      [ c["time"].to_i, c["count"] ]
    end
  end

  def calculate_querygraph(query, interval, host, stream, since)
    raise "Invalid interval" unless valid_interval?(interval)

    MessageGateway.universal_search(1, query, :date_histogram => true, :date_histogram_interval => interval, :host => host, :stream => stream, :since => since).collect do |c|
      [ c["time"].to_i, c["count"] ]
    end
  end

  def escape(what)
    CGI.escapeHTML(what.to_s)
  end

  def valid_interval?(interval)
  ["year", "month", "week", "day", "hour", "minute"].include?(interval)
  end

end
