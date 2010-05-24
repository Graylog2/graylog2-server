require File.expand_path(File.join(File.dirname(__FILE__),'..', '..','..','test_helper'))
require 'new_relic/agent/samplers/cpu_sampler'

class NewRelic::Agent::StatsEngine::SamplersTest < Test::Unit::TestCase
  
  def setup
    @stats_engine = NewRelic::Agent::StatsEngine.new
    NewRelic::Agent.instance.stubs(:stats_engine).returns(@stats_engine)
  end
  def test_cpu
    s = NewRelic::Agent::Samplers::CpuSampler.new
    # need to sleep because if you go to fast it will skip the points
    s.stats_engine = @stats_engine
    sleep 2
    s.poll
    sleep 2
    s.poll
    assert_equal 2, s.systemtime_stats.call_count
    assert_equal 2, s.usertime_stats.call_count
    assert s.usertime_stats.total_call_time >= 0, "user cpu greater/equal to 0: #{s.usertime_stats.total_call_time}"
    assert s.systemtime_stats.total_call_time >= 0, "system cpu greater/equal to 0: #{s.systemtime_stats.total_call_time}"
  end
  def test_memory__default
    s = NewRelic::Agent::Samplers::MemorySampler.new
    s.stats_engine = @stats_engine
    s.poll
    s.poll
    s.poll
    assert_equal 3, s.stats.call_count
    assert s.stats.total_call_time > 0.5, "cpu greater than 0.5 ms: #{s.stats.total_call_time}"
  end
  def test_memory__linux
    return if RUBY_PLATFORM =~ /darwin/
    NewRelic::Agent::Samplers::MemorySampler.any_instance.stubs(:platform).returns 'linux'
    s = NewRelic::Agent::Samplers::MemorySampler.new
    s.stats_engine = @stats_engine
    s.poll
    s.poll
    s.poll
    assert_equal 3, s.stats.call_count
    assert s.stats.total_call_time > 0.5, "cpu greater than 0.5 ms: #{s.stats.total_call_time}"
  end
  def test_memory__solaris
    return if defined? JRuby
    NewRelic::Agent::Samplers::MemorySampler.any_instance.stubs(:platform).returns 'solaris'
    NewRelic::Agent::Samplers::MemorySampler::ShellPS.any_instance.stubs(:get_memory).returns 999
    s = NewRelic::Agent::Samplers::MemorySampler.new
    s.stats_engine = @stats_engine
    s.poll
    assert_equal 1, s.stats.call_count
    assert_equal 999, s.stats.total_call_time
  end
  def test_memory__windows
    return if defined? JRuby
    NewRelic::Agent::Samplers::MemorySampler.any_instance.stubs(:platform).returns 'win32'
    assert_raise NewRelic::Agent::Sampler::Unsupported do
      NewRelic::Agent::Samplers::MemorySampler.new
    end
  end
  def test_load_samplers
    @stats_engine.expects(:add_harvest_sampler).once unless defined? JRuby
    @stats_engine.expects(:add_sampler).never
    NewRelic::Control.instance.load_samplers
    sampler_count = 4
    assert_equal sampler_count, NewRelic::Agent::Sampler.sampler_classes.size, NewRelic::Agent::Sampler.sampler_classes.inspect
  end
  def test_memory__is_supported
    NewRelic::Agent::Samplers::MemorySampler.stubs(:platform).returns 'windows'
    assert !NewRelic::Agent::Samplers::MemorySampler.supported_on_this_platform? || defined? JRuby
  end
  
end
