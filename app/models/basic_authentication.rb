class BasicAuthentication
  def initialize(login, password)
    @login = login
    @password = password
  end

  def credentials
    @credentials ||= if user && user.authenticated?(@password)
      Authenticator::Credentials.new(@login)
    end
  end

  private
  def user
    @user ||= User.find_by_login(@login.downcase)
  end
end
