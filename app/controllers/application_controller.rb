class ApplicationController < ActionController::Base
  include AuthenticatedSystem

  clear_helpers
#  helper :all # include all helpers, all the time
  protect_from_forgery # See ActionController::RequestForgeryProtection for details
  helper Authorization::AuthorizationHelper

  before_filter :login_required

  def rescue_action e
    # Connection to MongoDB failed.
    if e.class == Mongo::ConnectionFailure
        render :file => "#{Rails.root.to_s}/public/mongo_connectionfailure.html", :status => 500
        return
    end

    # Default 404 error.
    if e.class == ActionController::RoutingError
      render :file  => "#{Rails.root.to_s}/public/404.html", :status => 404
      return
    end

    # Default 500 error.
    Rails.logger.error "ERROR: #{e.to_s}"
    render :file  => "#{Rails.root.to_s}/public/500.html", :status => 500
  end

  helper_method :has_users
  def has_users
    return false if User.count == 0
    return true
  end

  helper_method :gl_date
  def gl_date(date)
    date = date.to_s
    return String.new if date == nil or date.length == 0
    tmp = DateTime.parse(date)
    return tmp.strftime(Configuration.date_format)
  end

  private

  def logged_in?
    begin
      return true if current_user
    end
    return false
  end

  def login_required
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

end
