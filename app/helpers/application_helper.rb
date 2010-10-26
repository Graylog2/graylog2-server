module ApplicationHelper
  def menu_item where
    if where[:controller] == "/"
      destination = "/"
    else
      destination = "/" + where[:controller]
    end

    "<li class=\"#{"topmenu-active" if is_current_menu_item? where[:controller]}\">#{link_to(where[:title], destination)}</li>"
  end

  def gl_date date
    return String.new if date.blank?
    tmp = DateTime.parse(date)
    return tmp.strftime "%d.%m.%Y - %H:%M:%S"
  end

  def get_ordered_facilities_for_select
      [
        ["Kernel (0)", 0],
        ["User-Level (1)", 1],
        ["Mail (2)", 2],
        ["System Daemon (3)", 3],
        ["Security/Authorization (4/10)", 4],
        ["Syslogd (5)", 5],
        ["Line Printer (6)", 6],
        ["News (7)", 7],
        ["UUCP (8)", 8],
        ["Clock (9/15)", 9],
        ["FTP (11)", 11],
        ["NTP (12)", 12],
        ["Log Audit (13)", 13],
        ["Log Alert (14)", 14],
        ["local0 (16)", 16],
        ["local1 (17)", 17],
        ["local2 (18)", 18],
        ["local3 (19)", 19],
        ["local4 (20)", 20],
        ["local5 (21)", 21],
        ["local6 (22)", 22],
        ["local7 (23)", 23]
      ]
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

  def syslog_facility_to_human facility
    return "GELF" if facility == nil

    case facility.to_i
      when  0 then return "kernel"
      when  1 then return "user-level"
      when  2 then return "mail"
      when  3 then return "system daemon"
      when  4 then return "security/authorization"
      when  5 then return "syslogd"
      when  6 then return "line printer"
      when  7 then return "news"
      when  8 then return "UUCP"
      when  9 then return "clock"
      when 10 then return "security/authorization"
      when 11 then return "FTP"
      when 12 then return "NTP"
      when 13 then return "log audit"
      when 14 then return "log alert"
      when 15 then return "clock"
      when 16 then return "local0"
      when 17 then return "local1"
      when 18 then return "local2"
      when 19 then return "local3"
      when 20 then return "local4"
      when 21 then return "local5"
      when 22 then return "local6"
      when 23 then return "local7"
    end

    return "Unknown"
  end

  def build_controller_action_uri append = nil
    ret = String.new
    appender = String.new

    request.path_parameters['id'].blank? ? id = String.new : id = request.path_parameters['id']+ '/'
    if params[:filters].blank?
      ret = '/' + request.path_parameters['controller'] + '/' + request.path_parameters['action'] + '/' + id
      appender = '?'
    else
      ret = '/' + request.path_parameters['controller'] + '/' + request.path_parameters['action'] + '/' + id + '?'
      params[:filters].each { |k,v| ret += "filters[#{CGI.escape(k)}]=#{CGI.escape(v)}&" }
      ret = ret.chop
      appender = '&'
    end

    if append.blank?
      return ret
    else
       return ret + appender + append + '='
    end
  end

  private

  def is_current_menu_item? item
    true if (params[:controller] == "messages" and item == "/") or (params[:controller] == "hostgroups" and item == "hosts") or params[:controller] == item
  end
end
