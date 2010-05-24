require 'fileutils'
module NewRelic
module Rack
  class MetricApp
    def initialize(options)
      if options[:install]
        FileUtils.copy File.join(File.dirname(__FILE__), "newrelic.yml"), "."
        NewRelic::Agent.logger.info "==============================="
        NewRelic::Agent.logger.info "A newrelic.yml template was copied to #{File.expand_path('.')}."
        NewRelic::Agent.logger.info "Please add a license key to the file and restart #{$0}"
        exit 0
      end
      options[:app_name] ||= 'EPM Monitor'
      options[:disable_samplers] = true
      NewRelic::Agent.manual_start options
      unless NewRelic::Control.instance.license_key
        NewRelic::Agent.logger.error "Please add a valid license key to newrelic.yml."
        exit 1
      end
    end
    def call(env)
      request = ::Rack::Request.new env
      segments = request.url.gsub(/^.*?\/metrics\//, '').split("?")[0].split("/")
      metric = "Custom/" + segments.join("/")
      raise "Expected value parameter!" unless request['value']
      data = request['value'].to_f
      stats = NewRelic::Agent.get_stats(metric, false)
      stats.record_data_point data
      response = ::Rack::Response.new "#{metric}=#{data}" 
      response.finish
    end
  end
  class Status
    def call(env)
      request = ::Rack::Request.new env
      data_url = "http://#{env['HTTP_HOST']}/metrics/path?value=nnn"
      body = StringIO.new
      body.puts "<html><body>"
      body.puts "<h1>New Relic Actively Monitoring #{NewRelic::Control.instance.app_names.join(' and ')}</h1>"
      body.puts "<p>To submit a metric value, use <a href='#{data_url}'>#{data_url}</a></p>"
      body.puts "<h2>Request Details</h2>"
      body.puts "<dl>"
      body.puts "<dt>ip<dd>#{request.ip}"
      body.puts "<dt>host<dd>#{request.host}"
      body.puts "<dt>path<dd>#{request.url}"
      body.puts "<dt>query<dd>#{request.query_string}"
      body.puts "<dt>params<dd>#{request.params.inspect}"
      body.puts "</dl>"
      body.puts "<h2>Complete ENV</h2>"
      body.puts "<ul>"
      body.puts env.to_a.map{|k,v| "<li>#{k} = #{v}</li>" }.join("\n")
      body.puts "</ul></body></html>"
      response = ::Rack::Response.new body.string
      response.finish
    end
  end
end
end
