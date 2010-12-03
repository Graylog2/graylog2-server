class AnalyticsController < ApplicationController
  def index
  end
  
  def messagespread
    @load_jit = true

    @term_is_regex = params[:regex].blank? ? false : true

    @term = params[:term]
    if @term.blank?
      flash[:error] = "Missing term."
      redirect_to :controller => "analytics"
      return
    end
  end
end
