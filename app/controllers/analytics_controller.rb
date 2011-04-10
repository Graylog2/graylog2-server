class AnalyticsController < ApplicationController
  filter_access_to :index
  filter_access_to :messagespread

  def index
    @load_flot = true
  end

  def messagespread
    @load_jit = true

    @term_is_regex = params[:regex] == "true" ? true : false
    @term_is_case_insensitive = params[:notcase] == "true" ? true : false

    @term = params[:term]
    if @term.blank?
      flash[:error] = "Missing term."
      redirect_to :action => "index"
      return
    end
  end
end
