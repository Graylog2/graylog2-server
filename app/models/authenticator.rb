class Authenticator
  class Credentials
    attr_reader :login, :email, :name, :from_ldap

    def initialize(login, email=nil, name=nil, from_ldap=false)
      @login     = login
      @email     = email
      @name      = name
      @from_ldap = from_ldap
    end

    def to_hash
      { :login     => login,
        :email     => email,
        :name      => name,
        :from_ldap => from_ldap }
    end
  end

  attr_reader :credentials

  def initialize(login, password)
    Graylog2WebInterface::Application.config.authentication_strategies.each do |strategy|
      @credentials = strategy.new(login, password).credentials

      break if @credentials
    end
  end

  def authenticated?
    credentials
  end
end
