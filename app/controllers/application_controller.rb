# Filters added to this controller apply to all controllers in the application.
# Likewise, all the methods added will be available for all controllers.

class ApplicationController < ActionController::Base
  include AuthenticatedSystem

  helper :all # include all helpers, all the time
  protect_from_forgery # See ActionController::RequestForgeryProtection for details

  # Scrub sensitive parameters from your log
  filter_parameter_logging :password

  before_filter :login_required

  def rescue_action e
    # Connection to MongoDB failed.
    if e.class == Mongo::ConnectionFailure
        render :file => "#{RAILS_ROOT}/public/mongo_connectionfailure.html", :status => 500
        return
    end

    # Default 404 error.
    if e.class == ActionController::RoutingError
      render :file  => "#{RAILS_ROOT}/public/404.html", :status => 404
      return
    end

    # Default 500 error.
    logger.error "ERROR: #{e.to_s}"
    render :file  => "#{RAILS_ROOT}/public/500.html", :status => 500
  end

  helper_method :has_users
  def has_users
    return false if User.find(:all).size == 0
    return true
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
      redirect_to :controller => "sessions", :action => "new"
    else
      return true
    end
    return false
  end
end
