module SettingsHelper

  def settings_tabs
    # lol this tab shit sucks: http://jira.graylog2.org/browse/WEBINTERFACE-43
    return [] unless @has_settings_tabs

    tabs = [
      ["General", settings_path],
      ["AMQP", amqp_settings_path],
      ["Additional columns", additionalcolumns_path],
      ["Message comments", messagecomments_path],
      ["Filtered terms", filteredterms_path],
      ["System", systemsettings_path],
    ]

    tabs << ["Version check", versioncheck_index_path] if Configuration.allow_version_check

    tabs
  end

end
