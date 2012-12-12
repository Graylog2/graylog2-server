class LdapAuthentication
  def initialize(login, password)
    @login    = login
    @password = password
  end

  def credentials
    # Make sure that we dont allow users to bind with a blank password.
    # https://github.com/ruby-ldap/ruby-net-ldap/issues/5

    if @password.present?
      user = get_user
      if user
        email = user[::Configuration.ldap_mail_attribute].first
        name  = user[::Configuration.ldap_displayname_attribute].first
        Authenticator::Credentials.new(@login, email, name, true)
      end
    end
  end

  private
  def get_user
    session = Net::LDAP.new(
          host:       ::Configuration.ldap_host,
          port:       ::Configuration.ldap_port,
          auth:       get_auth_for_configuration,
          encryption: get_encryption)
      
    if ::Configuration.ldap_user_dn_pattern
      result = session.search(
          base:          get_user_dn_from_pattern,
          attributes:    get_attributes,
          return_result: true
      )
      result ? result.try(:first) : nil
    elsif ::Configuration.ldap_search_base_dn && ::Configuration.ldap_search_filter
      result = session.bind_as(
          base:          ::Configuration.ldap_search_base_dn,
          filter:        get_search_filter_bind_as,
          password:      @password,
          attributes:    get_attributes,
          return_result: true
      )
      result ? result.try(:first) : nil
    else
      raise ArgumentError, 'LDAP authentication requires either a user_dn_pattern, or a search_base_dn and a search_filter'
    end
  end

  private
  def get_auth_for_configuration
    if ::Configuration.ldap_user_dn_pattern
      get_auth get_user_dn_from_pattern, @password
    elsif ::Configuration.ldap_search_bind_dn && ::Configuration.ldap_search_bind_password
      get_auth ::Configuration.ldap_search_bind_dn, ::Configuration.ldap_search_bind_password
    end
  end

  private
  def get_auth username, password
    { method: :simple, username: username, password: password }
  end

  private
  def get_encryption
    ::Configuration.ldap_tls_enabled? ? :simple_tls : nil
  end

  private
  def get_attributes
    [ ::Configuration.ldap_displayname_attribute, ::Configuration.ldap_mail_attribute ]
  end

  private
  def get_user_dn_from_pattern
    ::Configuration.ldap_user_dn_pattern % [ @login ]
  end

  private
  def get_search_filter_bind_as
    ::Configuration.ldap_search_filter % [ @login ]
  end
end
