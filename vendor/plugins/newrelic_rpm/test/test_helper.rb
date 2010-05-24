module NewRelic; TEST = true; end unless defined? NewRelic::TEST
#ENV['NEWRELIC_ENABLE'] = 'true'
ENV['RAILS_ENV'] = 'test'
NEWRELIC_PLUGIN_DIR = File.expand_path(File.join(File.dirname(__FILE__),".."))
$LOAD_PATH << File.join(NEWRELIC_PLUGIN_DIR,"test")
$LOAD_PATH << File.join(NEWRELIC_PLUGIN_DIR,"ui/helpers")
$LOAD_PATH.uniq!

require File.expand_path(File.join(NEWRELIC_PLUGIN_DIR, "..","..","..","config","environment"))

require 'test_help'
require 'mocha'
require 'test/unit'

def assert_between(floor, ceiling, value, message = nil)
  assert floor <= value && value <= ceiling,
  message || "expected #{floor} <= #{value} <= #{ceiling}"
end

def compare_metrics expected_list, actual_list
  actual = Set.new actual_list
  actual.delete('GC/cumulative') # in case we are in REE
  expected = Set.new expected_list
  assert_equal expected.to_a.sort, actual.to_a.sort, "extra: #{(actual - expected).to_a.join(", ")}; missing: #{(expected - actual).to_a.join(", ")}"
end
=begin Enable this to see test names as they run
Test::Unit::TestCase.class_eval do
  def run_with_info *args, &block
    puts "#{self.class.name.underscore}/#{@method_name}"
    run_without_info *args, &block
  end
  alias_method_chain :run, :info
end
=end
module TransactionSampleTestHelper
  def make_sql_transaction(*sql)
    sampler = NewRelic::Agent::TransactionSampler.new
    sampler.notice_first_scope_push Time.now.to_f
    sampler.notice_transaction '/path', nil, :jim => "cool"
    sampler.notice_push_scope "a"
    
    sampler.notice_transaction '/path/2', nil, :jim => "cool"
    
    sql.each {|sql_statement| sampler.notice_sql(sql_statement, {:adapter => "test"}, 0 ) }
    
    sleep 1.0
    yield if block_given?
    sampler.notice_pop_scope "a"
    sampler.notice_scope_empty
    
    sampler.samples[0]
  end
end
