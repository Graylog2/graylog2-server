# Filters added to this controller apply to all controllers in the application.
# Likewise, all the methods added will be available for all controllers.

class ApplicationController < ActionController::Base
  helper :all # include all helpers, all the time
  protect_from_forgery # See ActionController::RequestForgeryProtection for details

  # Scrub sensitive parameters from your log
  # filter_parameter_logging :password

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
    render :file  => "#{RAILS_ROOT}/public/500.html", :status => 500
  end
end
