class ApplicationController < ActionController::Base
  include AuthenticatedSystem

  protect_from_forgery
  helper Authorization::AuthorizationHelper
  before_filter :login_required, :clear_terms_cache, :block_demo_access

  rescue_from "Mongo::ConnectionFailure" do
      render_custom_error_page("mongo_connectionfailure") and return
  end

  rescue_from "RestClient::ResourceNotFound" do
      render_custom_error_page("elasticsearch_noindex") and return
  end

  rescue_from "Errno::ECONNREFUSED" do
      render_custom_error_page("elasticsearch_noconnection") and return
  end

  helper_method :has_users
  def has_users
    return false if User.count == 0
    return true
  end

  # TODO remove, replace with time_to_formatted_s
  helper_method :gl_date
  def gl_date(date)
    date = date.to_s
    return String.new if date == nil or date.length == 0
    tmp = DateTime.parse(date)
    return tmp.strftime(::Configuration.date_format)
  end

  #carry around a list of actions that should not use the session for auth.
  #we do this because of some non-api json routes like health that /do/ use the session.
  @@session_ignorant_actions = []
  def self.ignore_session_on_json(*actions)
    @@session_ignorant_actions = actions
  end

  def should_ignore_session?(action)
    return @@session_ignorant_actions.include?(action.to_sym)
  end

  private

  def logged_in?
    begin
      return true if current_user
    end
    return false
  end

  def api_login
    key = params[:api_key]
    return nil unless key
    return User.find_by_key(key)
  end

  def login_required
    if  request.format.json? then
      return true if logged_in? and not should_ignore_session?(action_name)
      @current_user = api_login() || false
      if !logged_in?
        render :json => {"error" => "unauthorized"}, :status=>401
        return false
      end
    end
    if !logged_in?
      store_location
      redirect_to login_path
    else
      return true
    end
    return false
  end

  def not_found
    render :file  => "#{Rails.root.to_s}/public/404.html", :status => 404, :layout => false
  end

  def clear_terms_cache
    FilteredTerm.expire_cache
  end

  def render_custom_error_page(tpl)
    render :file => "#{Rails.root.to_s}/public/#{tpl}.html", :status => 500, :layout => false
    return
  end

  def block_demo_access
    # current_user check because login POST must still work.
    if ::Configuration.is_demo_system? and current_user and !request.get?
      flash[:error] = "Sorry, this demo is not allowing any changes."
      redirect_to :controller => :messages, :action => :index
    end
  end

end
