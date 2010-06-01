class DashboardController < ApplicationController

  layout "dashboard"

  def index
    #@stats = Systemstatistic.all :limit => 6.hours/60

    render :text => Message.count({ "created_at" => { "$gt" => 12.hours.ago.to_i }, "host" => "www20" })
    return
  end

end
