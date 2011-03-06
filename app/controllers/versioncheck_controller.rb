class VersioncheckController < ApplicationController

  before_filter :allowed?

  def index
    @outdated = Version.outdated?
  end

  def perform
    current_user.last_version_check = Time.now.to_i
    current_user.save
    render :text => Version.outdated? ? "true" : "false"
  end

  private

  def allowed?
    if !Configuration.allow_version_check
      render :text => "not allowed", :status => :forbidden
      return false
    end
  end

end
