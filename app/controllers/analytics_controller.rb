class AnalyticsController < ApplicationController
  filter_access_to :index, :shell

  def index
    @has_shell = true
    @has_sidebar = true
  end

  def shell
    render_error("Empty input.") and return if params[:cmd].blank?
    
    shell = Shell.new(params[:cmd])

    time = Benchmark.realtime do
      @result = shell.compute
    end

    ms = sprintf("%#.2f", time*1000);

    html_result = render_to_string(decide_result_partial(@result[:operation]), :layout => false)

    render_success(ms, html_result, shell.operator, @result[:result])
  rescue => e
    logger.warn("Error while computing shell command: " + e.to_s + e.backtrace.join("\n"))
    render_error("Internal error.")
  end

  private
  def decide_result_partial(type)
    case type
      when "count" then return "_count_result"
      when "find" then return "_find_result"
      when "distinct" then return "_distinct_result"
    end
  end

  def render_error(reason)
    res = {
      :code => "error",
      :reason => reason
    }

    render :text => res.to_json
  end

  def render_success(ms, content, operation, result)
    res = {
      :code => "success",
      :ms => ms,
      :content => content,
      :op => operation
    }

    if operation == "count" or operation == "distinct"
      res[:result] = result;
    end

    render :text => res.to_json
  end

end
