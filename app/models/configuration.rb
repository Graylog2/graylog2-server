class Configuration
  @general_config = YAML::load File.read((ENV['GRAYLOG2_BASE'] || Rails.root.to_s) + "/config/general.yml")
  @indexer_config = YAML::load File.read((ENV['GRAYLOG2_BASE'] || Rails.root.to_s) + "/config/indexer.yml")
  @ldap_config = YAML::load File.read((ENV['GRAYLOG2_BASE'] || Rails.root.to_s) + "/config/ldap.yml")

  def self.config_value(root, nesting, key, default = nil)
    [root, root[nesting.to_s], root[nesting.to_s][key.to_s]].any?(&:blank?) ? default : root[nesting.to_s][key.to_s]
  rescue
    nil
  end

  def self.nested_general_config(nesting, key, default = nil)
    config_value @general_config, nesting, key, default
  end

  def self.general_config(key, default = nil)
    nested_general_config :general, key, default
  end

  def self.external_hostname
    general_config :external_hostname, 'localhost'
  end

  def self.allow_version_check
    general_config :allow_version_check, false
  end

  def self.is_demo_system?
    general_config :is_demo_system, false
  end

  def self.custom_cookie_name
    general_config :custom_cookie_name
  end

  def self.date_format
    general_config :date_format, "%d.%m.%Y - %H:%M:%S"
  end

  def self.indexer_config(key = nil, default = nil)
    if key
      config_value @indexer_config, Rails.env, key, default
    else
      @indexer_config[Rails.env]
    end
  end

  def self.indexer_host
    indexer_config :url
  end

  def self.indexer_index_prefix
    indexer_config :index_prefix
  end

  def self.indexer_recent_index_name
    indexer_config :recent_index_name
  end

  def self.ldap_config(key, default = nil)
    config_value @ldap_config, :ldap, key, default
  end

  def self.ldap_enabled?
    ldap_config :enabled, false
  end

  def self.ldap_host
    ldap_config :host
  end

  def self.ldap_port
    ldap_config :port, 389
  end

  def self.ldap_tls_enabled?
    ldap_config :tls_enabled, false
  end

  def self.ldap_displayname_attribute
    ldap_config :displayname_attribute, 'cn'
  end

  def self.ldap_mail_attribute
    ldap_config :mail_attribute, 'mail'
  end

  def self.ldap_user_dn_pattern
    ldap_config :user_dn_pattern
  end
  
  def self.ldap_search_base_dn
    ldap_config :search_base_dn
  end

  def self.ldap_search_filter
    ldap_config :search_filter
  end

  def self.ldap_search_bind_dn
    ldap_config :search_bind_dn
  end

  def self.ldap_search_bind_password
    ldap_config :search_bind_password
  end

end
