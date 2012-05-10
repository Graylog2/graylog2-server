class Configuration
  @general_config = YAML::load File.read(Rails.root.to_s + "/config/general.yml")
  @email_config = YAML::load File.read(Rails.root.to_s + "/config/email.yml")
  @indexer_config = YAML::load File.read(Rails.root.to_s + "/config/indexer.yml")

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

  def self.allow_deleting
    general_config :allow_deleting, false
  end

  def self.custom_cookie_name
    general_config :custom_cookie_name
  end

  def self.date_format
    general_config :date_format, "%d.%m.%Y - %H:%M:%S"
  end

  def self.hide_message_stats?
    general_config :hide_message_stats, false
  end

  def self.hoptoad_config(key, default = nil)
    nested_general_config :hoptoad, key, default
  end

  def self.hoptoad_enabled?
    hoptoad_config :enabled, false
  end

  def self.hoptoad_ssl?
    hoptoad_config :ssl
  end

  def self.hoptoad_key
    hoptoad_config :api_key
  end

  def self.hoptoad_host
    hoptoad_config :host
  end

  def self.subscr_config(key, default)
    nested_general_config :subscriptions, key, default
  end

  def self.subscription_from_address
    subscr_config :from, 'graylog2@example.org'
  end

  def self.subscription_subject
    subscr_config :subject, "[graylog2] Subscription"
  end

  def self.streamalarm_config(key, default)
    nested_general_config :streamalarms, key, default
  end

  def self.streamalarm_from_address
    streamalarm_config :from, "graylog2@example.org"
  end

  def self.streamalarm_subject
    streamalarm_config :subject, "[graylog2] Stream alarm!"
  end

  def self.email_config(key = nil, default = nil)
    if key
      config_value @email_config, Rails.env, key, default
    else
      @email_config[Rails.env]
    end
  end

  def self.indexer_config(key = nil, default = nil)
    if key
      config_value @indexer_config, Rails.env, key, default
    else
      @indexer_config[Rails.env]
    end
  end

  def self.email_transport_type
    default = :sendmail
    email_config('via', default).to_sym.tap do |value|
      value = default unless [:sendmail, :smtp].include?(value) # Only sendmail or SMTP allowed.
    end
  end

  def self.email_smtp_settings
    Hash.new.tap do |ret|
      if email_transport_type == :smtp
        email_config.each_pair do |key, value|
          ret[key.to_sym] = value unless value.blank?
        end
      end
    end
  end

  def self.indexer_host
    indexer_config :url
  end

  def self.indexer_index_name
    indexer_config :index_name
  end
end
