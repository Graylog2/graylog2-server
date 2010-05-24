require File.expand_path(File.join(File.dirname(__FILE__),'..','..','test_helper')) 

class NewRelic::Agent::NetInstrumentationTest < Test::Unit::TestCase
  include NewRelic::Agent::Instrumentation::ControllerInstrumentation
  def setup
    NewRelic::Agent.manual_start
    @engine = NewRelic::Agent.instance.stats_engine
    @engine.clear_stats
  end
  def test_get
    url = URI.parse('http://www.google.com/index.html')
    res = Net::HTTP.start(url.host, url.port) {|http|
      http.get('/index.html')
    }
    assert_match /<head>/, res.body
    assert_equal %w[External/www.google.com/Net::HTTP/GET External/allOther External/www.google.com/all].sort,
       @engine.metrics.sort 
  end

  def test_background
    perform_action_with_newrelic_trace("task", :category => :task) do
      url = URI.parse('http://www.google.com/index.html')
      res = Net::HTTP.start(url.host, url.port) {|http|
        http.get('/index.html')
      }
      assert_match /<head>/, res.body
    end
    assert_equal @engine.metrics.select{|m| m =~ /^External/}.sort, 
       %w[External/www.google.com/Net::HTTP/GET External/allOther External/www.google.com/all
       External/www.google.com/Net::HTTP/GET:OtherTransaction/Background/NewRelic::Agent::NetInstrumentationTest/task].sort
  end

  def test_transactional
    perform_action_with_newrelic_trace("task") do
      url = URI.parse('http://www.google.com/index.html')
      res = Net::HTTP.start(url.host, url.port) {|http|
        http.get('/index.html')
      }
      assert_match /<head>/, res.body
    end
    assert_equal @engine.metrics.select{|m| m =~ /^External/}.sort, 
       %w[External/www.google.com/Net::HTTP/GET External/allWeb External/www.google.com/all
       External/www.google.com/Net::HTTP/GET:Controller/NewRelic::Agent::NetInstrumentationTest/task].sort
  end
  def test_get__simple
    Net::HTTP.get URI.parse('http://www.google.com/index.html')
    assert_equal @engine.metrics.sort, 
       %w[External/www.google.com/Net::HTTP/GET External/allOther External/www.google.com/all].sort
  end
  def test_ignore
    NewRelic::Agent.disable_all_tracing do
      url = URI.parse('http://www.google.com/index.html')
      res = Net::HTTP.start(url.host, url.port) {|http|
        http.post('/index.html','data')
      }
    end
    assert_equal 0, @engine.metrics.size 
  end
  def test_head
    url = URI.parse('http://www.google.com/index.html')
    res = Net::HTTP.start(url.host, url.port) {|http|
      http.head('/index.html')
    }
    assert_equal %w[External/www.google.com/Net::HTTP/HEAD External/allOther External/www.google.com/all].sort,
    @engine.metrics.sort 
  end
  
  def test_post
    url = URI.parse('http://www.google.com/index.html')
    res = Net::HTTP.start(url.host, url.port) {|http|
      http.post('/index.html','data')
    }
    assert_equal %w[External/www.google.com/Net::HTTP/POST External/allOther External/www.google.com/all].sort, 
    @engine.metrics.sort 
  end
  
end
