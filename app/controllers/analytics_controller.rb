class AnalyticsController < ApplicationController
  def index
  end
  
  def messagespread
    @load_jit = true
    @term = params[:term]
    if @term.blank?
      redirect_to :controller => "messages"
      return
    end
  end
end
