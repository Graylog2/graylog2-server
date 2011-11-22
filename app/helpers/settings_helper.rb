module SettingsHelper

  def settings_tabs
    tabs = [
      ["General", settings_path],
      ["Message comments", messagecomments_path],
      ["Filtered terms", filteredterms_path],
    ]

    tabs << ["Version check", versioncheck_index_path] if Configuration.allow_version_check

    tabs
  end

end
