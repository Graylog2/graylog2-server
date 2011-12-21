class AnalyticsController < ApplicationController
  filter_access_to :index, :shell

  def index
    @load_flot = true
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

    if @result[:operation] == "find"
      html_result = render_to_string("_find_result", :layout => false)
    else
      html_result = ""
    end

    render_success(ms, html_result, shell.operator, @result[:result])
  rescue InvalidSelectorException
    render_error("Invalid selector.")
  rescue InvalidOperatorException
    render_error("Invalid operator.")
  rescue InvalidOptionException
    render_error("Invalid options.")
  rescue MissingDistinctTargetException
    render_error("Missing distinct target.")
  rescue MissingStreamTargetException
    render_error("Missing stream target(s).")
  rescue UnknownStreamException
    render_error("Unknown stream(s).")
  rescue => e
    logger.warn("Error while computing shell command: " + e.to_s + e.backtrace.join("\n"))
    render_error("Could not parse command or encountered internal error.")
  end

  private

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

    if operation == "count" || operation == "distinct" || operation == "distribution"
      res[:result] = result;
    end

    render :text => res.to_json
  end

end
