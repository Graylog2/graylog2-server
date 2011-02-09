class VisualsController < ApplicationController

  def fetch
    r = Hash.new

    time = Benchmark.realtime do
      case params[:id]
        when "messagespread" then
          case_sensitive = params[:case] == "true" ? false : true
          is_regex = params[:regex] == "true" ? true : false
          r["data"] = calculate_messagespread(is_regex, case_sensitive, params[:term])
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

  def calculate_messagespread(is_regex, case_sensitive, message)
    values = Array.new

    conditions = Hash.new
    conditions["deleted"] = false

    if is_regex
      search_for = message
    else
      search_for = Regexp.escape(message)
    end

    if case_sensitive
      conditions["message"] = /#{search_for}/
    else
      conditions["message"] = /#{search_for}/i
    end

    hosts = Host.all

    highest = 0
    hosts.each do |host|
      conditions["host"] = escape(host.host)
      count = Message.count :conditions => conditions

      if count > 0
        value = Hash.new
        value["data"] = { "$angularWidth" => count }
        value["id"] = Base64.encode64(host.host).chomp
        value["name"] = escape(host.host)

        values << value
      end
      highest = count if count > highest
    end

    # Sort values.
    values = values.sort_by { |v| v["data"]["$angularWidth"] } 

    # Add weighted colors.
    colored_values = Array.new
    values.each do |value|
      red = (value["data"]["$angularWidth"].to_f/highest.to_f*255).to_i.floor.to_s(16)
      red = "0#{red}" if red.length == 1
      value["data"]["$color"] = "##{red}0010"
      colored_values << value
    end

    r = Hash.new
    r["data"] = Hash.new
    r["data"]["$color"] = "#fff"
    r["children"] = Array.new
    r["children"] << { "children" => colored_values }

    return r
  end

  def calculate_hostgrouprelation(all_hosts, group_id)
    group = Hostgroup.find(group_id)
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
      hosts = Host.all :conditions => { :host.in => regex }
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
    Message.counts_of_last_minutes(hours.to_i*60).collect do |c|
      [ (c[:minute].to_i+Time.now.utc_offset)*1000, c[:count] ]
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
