class VisualsController < ApplicationController
  def fetch
    r = Hash.new

    time = Benchmark.realtime do
      case params[:id]
        when "hostgrouprelation" then
          r["data"] = calculate_hostgrouprelation(false, params[:group])
        when "totalgraph" then
          r["data"] = calculate_totalgraph(params[:hours])
        when "streamgraph" then
          r["data"] = calculate_streamgraph(params[:stream_id], params[:hours])
      end
    end

    r["time"] = sprintf("%#.2f", time*1000);

    render :js => r.to_json
  end

  private

  def calculate_hostgrouprelation(all_hosts, group_id)
    group = Hostgroup.find(BSON::ObjectId(group_id))
    values = Array.new

    # Add hostname conditions
    hostnames = group.hostname_conditions(true)
    hostnames.each do |hostname|
      values << {
        "id" => escape(hostname[:id]),
        "name" => escape(hostname[:value])
      }
    end

    # Add regex conditions and their matches
    regexes = group.regex_conditions(true)
    regexes.each do |regex|
      # Get machtching hosts.
      hosts = Host.all :conditions => { :host => /#{regex[:value]}/ }
      children = Array.new
      hosts.each do |host|
        children << {
          "id" => "regex-match-#{escape(host.id)}",
          "name" => escape(host.host),
        }
      end

      values << {
        "id" => escape(regex[:id]),
        "name" => "Regex: #{escape(regex[:value].source)}",
        "children" => children
      }
    end

    r = Hash.new
    # Add root node.
    r["id"] = "root"
    r["name"] = "Group: #{escape(group.name)}"
    r["children"] = values

    return r
  end

  def calculate_totalgraph(hours = 12)
    MessageCount.counts_of_last_minutes(hours.to_i*60).collect do |c|
      [ (c[:timestamp].to_i+Time.now.utc_offset)*1000, c[:count] ]
    end
  end

  def calculate_streamgraph(stream_id, hours=12)
    stream = Stream.find(stream_id)

    return Array.new if stream.streamrules.blank?

    Message.stream_counts_of_last_minutes(stream.id, hours.to_i*60).collect do |c|
      [ (c[:minute].to_i+Time.now.utc_offset)*1000, c[:count] ]
    end
  end

  def escape(what)
    CGI.escapeHTML(what.to_s)
  end
end
