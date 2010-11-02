# Filters added to this controller apply to all controllers in the application.
# Likewise, all the methods added will be available for all controllers.

class ApplicationController < ActionController::Base
  include AuthenticatedSystem

  helper :all # include all helpers, all the time
  protect_from_forgery # See ActionController::RequestForgeryProtection for details

  # Scrub sensitive parameters from your log
  filter_parameter_logging :password

  before_filter :login_required

  rescue_from Exception do |e|
    if consider_all_requests_local || local_request?
      rescue_action_without_handler e
    else
      logger.error "ERROR: #{e.to_s}"
      render :file  => "#{RAILS_ROOT}/public/500.html", :status => 500
    end
  end

  rescue_from ActionView::TemplateError do |e|
    # This rescue_from handling all kind of errors which is raised during template rendering.
    # So, if environment is development, we should handle it as usual.
    # If environment is test or production, we should handle it in particular way –
    # just reraise the exception so that other rescue_from can handle it.
    if Rails.env.development?
      rescue_action_without_handler e
    else
      rescue_with_handler e.original_exception
    end
  end

  rescue_from Mongo::ConnectionFailure do |e|
    render :file => "#{RAILS_ROOT}/public/mongo_connectionfailure.html", :status => 500
  end

  rescue_from ActionController::RoutingError do |e|
    render :file  => "#{RAILS_ROOT}/public/404.html", :status => 404
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
