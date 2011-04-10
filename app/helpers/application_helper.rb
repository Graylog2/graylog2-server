module ApplicationHelper
  def menu_item title, where
    current = String.new
    if where.instance_of?(Hash)
      current = where[:controller].to_s
    else
      current = where[1..-1]
    end

    "<li class=\"#{"topmenu-active" if is_current_menu_item?(current)}\">#{link_to(title, "/#{current}")}</li>"
  end

  def tab_link tab
    "<div class=\"content-tabs-tab#{" content-tabs-tab-active" if is_current_tab?(tab)}\" >
      #{link_to tab.first, tab.second}
    </div>"
  end

  def get_ordered_severities_for_select
      [
        ["Emergency", 0],
        ["Alert", 1],
        ["Critical", 2],
        ["Error", 3],
        ["Warning", 4],
        ["Notice", 5],
        ["Informational", 6],
        ["Debug", 7]
      ]
  end

  def syslog_level_to_human level
    return "None" if level == nil

    case level.to_i
      when 0 then return "Emergency"
      when 1 then return "Alert"
      when 2 then return "Critical"
      when 3 then return "Error"
      when 4 then return "Warning"
      when 5 then return "Notice"
      when 6 then return "Informational"
      when 7 then return "Debug"
    end
    return "Invalid"
  end

  def build_controller_action_uri append_params = nil
    ret = String.new
    appender = String.new

    request.path_parameters[:id].blank? ? id = String.new : id = request.path_parameters[:id]+ '/'
    if params[:filters].blank?
      ret = '/' + request.path_parameters[:controller] + '/' + request.path_parameters[:action] + '/' + id
      appender = "?"
    else
      ret = '/' + request.path_parameters[:controller] + '/' + request.path_parameters[:action] + '/' + id + '?'
      params[:filters].each { |k,v| ret += "filters[#{CGI.escape(k)}]=#{CGI.escape(v)}&" }
      ret = ret.chop
      appender = "&"
    end

    # apped possible params
    unless append_params.blank?
      append_params.each do |param|
        ret += appender + "#{param[:key]}=#{param[:value]}"
        appender = "&" # Set after first run.
      end
    end

    return ret
  end

  def flot_graph_loader(options)
   if options[:stream_id].blank?
     url = visuals_path("totalgraph", :hours => options[:hours])
   else
     url = visuals_path("streamgraph", :stream_id => options[:stream_id], :hours => options[:hours])
   end

   "<script type='text/javascript'>
      function plot(data){
        $.plot($('#{options[:inject]}'),
          [ {
              color: '#fd0c99',
              shadowSize: 10,
              data: data,
              points: { show: false, },
              lines: { show: true, fill: true }
          } ],
          {
            xaxis: { mode: 'time' },
            grid: {
              show: true,
              color: '#ccc',
              borderWidth: 0,
            },
            selection: { mode: 'x' }
          }
        );
      }

      $('#{options[:inject]}').bind('plotselected', function(event, ranges) {
        $('#streams-sidebar-totalcount').hide();
        from = (ranges.xaxis.from/1000).toFixed(0);
        to = (ranges.xaxis.to/1000).toFixed(0);
        $('#graph-rangeselector').show();
        $('#graph-rangeselector-from').val(from);
        $('#graph-rangeselector-to').val(to);
      });

      $.post('#{url}', function(data) {
        json = eval('(' + data + ')');
          plot(json.data);
        });
    </script>"
  end

  def ajaxtrigger(title, description, url, checked)
   "#{check_box_tag(title, nil, checked, :class => "ajaxtrigger", "data-target" => url)}
    #{label_tag(title, description)}
    <span id=\"#{title.to_s}-ajaxtrigger-loading\" style=\"display: none;\">#{image_tag('loading-small.gif')} Saving...</span>
    <span id=\"#{title.to_s}-ajaxtrigger-done\" class=\"status-okay-text\" style=\"display: none;\">Saved!</span>"
  end

  def sparkline_values values
    res = ""
    i = 1
    values.each do |v|
      res += v.to_s
      res += "," unless i == values.size
      i += 1
    end

    return res
  end

  def stream_link(stream)
    ret = "
        <span class=\"favorite-stream-sparkline\">
          #{sparkline_values(Message.stream_counts_of_last_minutes(stream.id, 10).map { |c| c[:count] })}
        </span>
        #{link_to(h(stream.title), stream_path(stream), :class => 'favorite-streams-title')}
    "

    if stream.alarm_active and !stream.alarm_limit.blank? and !stream.alarm_timespan.blank?
      stream_count = stream.message_count_since(stream.alarm_timespan.minutes.ago.to_i)
      ret += "
        <span class=\"favorite-stream-limits #{stream_count > stream.alarm_limit ? "status-alarm-text" : "status-okay-text"}\">
          (#{stream_count}/#{stream.alarm_limit}/#{stream.alarm_timespan}m)
        </span>
      "
    end

    return ret
  end

  def user_link(user)
    return "Unknown or deleted user" if user.blank? or !user.instance_of?(User)

    "<span class=\"user-link\">
      #{image_tag "icons/user.png", :class => "user-link-img" }#{link_to(user.login, { :controller => "users", :action => "show", :id => user.id })}
    </span>"
  end

  def awesome_link(title, url, options = Hash.new)
    if options[:class].blank?
      options[:class] = "awesome"
    else
      options[:class] += " awesome"
    end

    link_to(title, url, options)
  end

  def awesome_submit_link(title, options = Hash.new)
    if options[:class].blank?
      options[:class] = "awesome submit-link"
    else
      options[:class] += " awesome submit-link"
    end

    link_to(title, "#", options)
  end

  def tooltip(to)
    link_to(image_tag("icons/tooltip.png"), "https://github.com/Graylog2/graylog2-web-interface/wiki/" + to, :class => "tooltip", :target => "_blank", :title => "Help page in the wiki")
  end

  def array_for_flot_with_timeseries(values)
    ret = "["
    values.each do |value|
      ret += "[#{value[0]},#{value[1]}],"
    end
    ret.chop! # remove last comma
    ret += "]"

    return ret
  end

  private

  def is_current_menu_item? item
    return true if (@scoping == :hostgroup and item == "hosts")
    return (@scoping.to_s.pluralize == item) unless @scoping.nil?

    (@scoping == item) or (params[:controller] == root_path and item == "/") or (params[:controller] == "hostgroups" and item == "hosts") or (params[:controller] == item)
  end

  def is_current_tab? tab
    current_page?(tab.second)
  end

  def current_page
    params[:page].blank? ? 1 : params[:page].to_i
  end

  def next_page
    current_page + 1
  end

  def previous_page
    current_page <= 2 ? 1 : current_page - 1
  end

  def partial_for(element, scoping="shared", action="")
    scoping = scoping.to_s.pluralize unless scoping == "shared"
    element = element.to_s + "_#{action}" unless action == ""
    "#{scoping}/#{element}"
  end

  def message_count_interval
    Setting.get_message_count_interval(current_user)
  end

  def tabs
    @tabs = []
    if (@scoping == :stream and @stream)
      @tabs.push ["Show", stream_path(@stream)] if permitted_to?(:show, @stream)
      @tabs.push ["Rules", rules_stream_path(@stream)] if permitted_to?(:rules, @stream)
      @tabs.push ["Forwarders", forward_stream_path(@stream)] if permitted_to?(:forward, @stream)
      @tabs.push ["Analytics", analytics_stream_path(@stream)] if permitted_to?(:analytics, @stream)
      @tabs.push ["Settings", settings_stream_path(@stream)] if permitted_to?(:show, @stream)
    elsif (@scoping == :hostgroup and @hostgroup)
      @tabs.push ["Show", hostgroup_path(@hostgroup)]
      @tabs.push ["Hosts", hosts_hostgroup_path(@hostgroup)]
      @tabs.push ["Settings", settings_hostgroup_path(@hostgroup)]
    end

    @tabs
  end
end
