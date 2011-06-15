class AnalyticsController < ApplicationController
  filter_access_to :index, :shell

  def index
    @has_shell = true
  end

  def shell
    render_error("Empty input.") and return if params[:cmd].blank?
    
    result = String.new
    shell = Shell.new(params[:cmd])

    time = Benchmark.realtime do
      result = shell.compute
    end

    ms = sprintf("%#.2f", time*1000);

    render_success(ms, result) and return
  rescue => e
    logger.warn("Error while computing shell command: " + e.to_s + e.backtrace.join("\n"))
    render_error("Internal error.")
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
