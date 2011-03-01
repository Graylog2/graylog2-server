class VersioncheckController < ApplicationController

  before_filter :allowed?

  def index
    @outdated = Version.outdated?
  end

  def perform
    render :text => Version.outdated? ? "true" : "false"
  end

  private

  def allowed?
    redirect_to :controller => "messages" if !Configuration.allow_version_check
    return
  end

end
