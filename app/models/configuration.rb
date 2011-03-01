class Configuration
  @general_config = YAML::load(File.read(Rails.root.to_s + "/config/general.yml"))
  @email_config = YAML::load(File.read(Rails.root.to_s + "/config/email.yml"))

  @subscr_config = @general_config['subscriptions']
  @streamalarm_config = @general_config['streamalarms']
  @livetail_config = @general_config['livetail']
  @app_config = @general_config['app']

  def self.external_hostname
    return "localhost" if @general_config.blank? or @general_config['general'].blank? or @general_config['general']['external_hostname'].blank?
    return @general_config['general']['external_hostname']
  end

  def self.allow_version_check
    return false if @general_config.blank? or @general_config['general'].blank? or @general_config['general']['allow_version_check'].blank?
    return @general_config['general']['allow_version_check']
  end

  def self.allow_deleting
    return false if @general_config.blank? or @general_config['general'].blank? or @general_config['general']['allow_deleting'].blank?
    return @general_config['general']['allow_deleting']
  end

  def self.subscription_from_address
    return @subscr_config['from'] unless @subscr_config.blank? or @subscr_config['from'].blank?
    return "graylog2@example.org"
  end

  def self.subscription_subject
    return @subscr_config['subject'] unless @subscr_config.blank? or @subscr_config['subject'].blank?
    return "[graylog2] Subscription"
  end
  
  def self.streamalarm_from_address
    return @streamalarm_config['from'] unless @streamalarm_config.blank? or @streamalarm_config['from'].blank?
    return "graylog2@example.org"
  end

  def self.streamalarm_subject
    return @streamalarm_config['subject'] unless @streamalarm_config.blank? or @streamalarm_config['subject'].blank?
    return "[graylog2] Stream alarm!"
  end

  def self.email_transport_type
    standard = :sendmail
    return standard if @email_config[Rails.env].blank? or @email_config[Rails.env]['via'].blank?
    # Only sendmail or SMTP allowed.
    allowed = ['sendmail', 'smtp']
    return standard unless allowed.include? @email_config[Rails.env]['via']

    return @email_config[Rails.env]['via'].to_sym
  end

  def self.email_smtp_settings
    return Hash.new if @email_config[Rails.env].blank? or @email_config[Rails.env]['via'].blank?
    config = @email_config[Rails.env]
    ret = Hash.new

    if config['via'] == 'smtp'
      ret[:host] = config['host'] unless config['host'].blank?
      ret[:port] = config['port'] unless config['port'].blank?
      ret[:user] = config['user'] unless config['user'].blank?
      ret[:password] = config['password'] unless config['password'].blank?
      ret[:auth] = config['auth'] unless config['auth'].blank?
      ret[:domain] = config['domain'] unless config['domain'].blank?
      return ret
    end

    return ret
  end

  def self.livetail_enabled
    return false if @livetail_config.blank? or @livetail_config['enable'].blank?
    return true if @livetail_config['enable'] == true
    return false
  end

  def self.livetail_secret
    return nil if @livetail_config.blank? or @livetail_config['secret'].blank?
    return @livetail_config['secret'].to_s
  end

  def self.date_format
    default = "%d.%m.%Y - %H:%M:%S"
    (@general_config.blank? or @general_config["general"].blank? or @general_config["general"]["date_format"].blank?) ? default : @general_config["general"]["date_format"]
  end

end
