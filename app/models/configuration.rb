class Configuration
  @general_config = YAML::load(File.read(RAILS_ROOT + "/config/general.yml"))
  @email_config = YAML::load(File.read(RAILS_ROOT + "/config/email.yml"))

  @alert_config = @general_config['alerts']

  def self.alert_from_address
    return @alert_config['from'] unless @alert_config.blank? or @alert_config['from'].blank?
    return "graylog2@example.org"
  end

  def self.alert_subject
    return @alert_config['subject'] unless @alert_config.blank? or @alert_config['subject'].blank?
    return "Graylog2 stream alert!"
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

end
