class AnalyticsController < ApplicationController
  filter_access_to :index, :shell

  def index
    @has_shell = true
  end

  def shell
    render_error("Empty input.") and return if params[:cmd].blank?
    
    time = Benchmark.realtime do
      100000.times do
        puts "LOL"
      end
    end

    ms = sprintf("%#.2f", time*1000);


    render_success(ms, "IMPLEMENT ME!") and return
  rescue
    render_error("Internal error.");
  end

  private
  def render_error(reason)
    res = {
      :code => "error",
      :reason => reason
    }

    render :text => res.to_json
  end

  def render_success(ms, content)
    res = {
      :code => "success",
      :ms => ms,
      :content => content
    }

    render :text => res.to_json
  end

end
