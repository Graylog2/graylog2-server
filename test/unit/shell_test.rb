require 'test_helper'

class ShellTest < ActiveSupport::TestCase

  context "command parsing" do

    should "parse selector" do
      s = Shell.new('all.find(_http_response_code = 500, host = "example.org")')
      assert_equal "all", s.selector
    end

    should "parse operator" do
      s = Shell.new('all.find(_http_response_code = 500, host = "example.org")')
      assert_equal "find", s.operator
    end

    should "parse operator options" do
      s = Shell.new('all.find(_http_response_code = 500, host = "example.org")')
      expected = Hash.new
      expected["_http_response_code"] = { :value => 500, :condition => "=" }
      expected["host"] =  { :value => "example.org", :condition => "=" }
      assert_equal expected, s.operator_options
    end

    should "work with whitespaces before or after command" do
      s = Shell.new(' all.find(_http_response_code = 500, host = "example.org")')
      assert_equal "find", s.operator

      s = Shell.new(' all.find(_http_response_code = 500, host = "example.org") ')
      assert_equal "find", s.operator

      s = Shell.new('all.find(_http_response_code = 500, host = "example.org") ')
      assert_equal "find", s.operator

      s = Shell.new('       all.find(_http_response_code = 500, host = "example.org") ')
      assert_equal "find", s.operator
    end

    should "accept negative integers in option values" do
      3.times { Message.make(:_foo => -9001) }
      1.times { Message.make(:_foo => 9001) }
      1.times { Message.make(:_foo => 5000) }
      1.times { Message.make(:_foo => 0) }

      s = Shell.new('all.count(_foo = -9001)')
      result = s.compute

      assert_equal "count", result[:operation]
      assert_equal 3, result[:result]
    end

    should "not overwrite multiple operator options of the same type" do
      s = Shell.new('all.find(foo > 500, foo < 600, foo >= 700)') # i know, these conditions do not make sense
      expected = Hash.new
      expected["foo"] = Array.new
      expected["foo"] << { :value => 500, :condition => ">" }
      expected["foo"] << { :value => 600, :condition => "<" }
      expected["foo"] << { :value => 700, :condition => ">=" }
      assert_equal expected, s.operator_options
    end

    should "respect conditional operators" do
      3.times { Message.make(:host => "example.org", :_http_response_code => 500) }
      7.times { Message.make(:host => "example.com", :_http_response_code => 500) }
      8.times { Message.make(:host => "example.com", :_http_response_code => 200) }
      10.times { Message.make(:host => "example.com", :_http_response_code => 201) }
      1.times { Message.make(:host => "example.com", :_http_response_code => 300) }

      s = Shell.new('all.count(host != "example.org", _http_response_code < 300, _http_response_code >= 200)')
      result = s.compute

      assert_equal "count", result[:operation]
      assert_equal 18, result[:result]
    end

    should "work with different conditional operators on the same key" do
      5.times { Message.make(:_foo => 1) }
      2.times { Message.make(:_foo => 2) }
      2.times { Message.make(:_foo => 3) }
      2.times { Message.make(:_foo => 4) }

      s = Shell.new('all.count(_foo > 0, _foo != 3, _foo != 4)')
      result = s.compute

      assert_equal "count", result[:operation]
      assert_equal 7, result[:result]
    end

    should "work with regex operator options" do
      5.times { Message.make(:host => "example.org") }
      5.times { Message.make(:host => "example.com") }
      5.times { Message.make(:host => "foo.example.com") }

      s = Shell.new('all.count(host = /^example\.(org|com)$/)')
      result = s.compute

      assert_equal "count", result[:operation]
      assert_equal 10, result[:result]
    end

    should "throw exception for not allowed selector" do
      assert_raise InvalidSelectorException do
        Shell.new('nothing.find(_http_response_code = 500, host = "example.org")')
      end
    end

    should "throw exception for not allowed operator" do
      assert_raise InvalidOperatorException do
        Shell.new('all.something(_http_response_code = 500, host = "example.org")')
      end
    end

  end

  context "stream selectors" do

    should "correctly parse stream selector" do
      stream_id = Stream.make().id
      s = Shell.new("stream(#{stream_id}).count()")
      assert_equal "stream", s.selector
      assert_equal [stream_id.to_s], s.stream_narrows
      assert_equal "count", s.operator
    end

    should "correctly parse streams selector with two streams" do
      streams = [Stream.make(), Stream.make()]
      stream_ids = streams.map { |s| s.id.to_s }

      s = Shell.new("streams(#{stream_ids.join(',')}).count()")
      assert_equal "streams", s.selector
      assert_equal stream_ids, s.stream_narrows
      assert_equal "count", s.operator
    end

    should "correctly parse streams selector with only one stream" do
      stream_id = Stream.make().id
      s = Shell.new("streams(#{stream_id}).count()")
      assert_equal "streams", s.selector
      assert_equal [stream_id.to_s], s.stream_narrows
      assert_equal "count", s.operator
    end

    should "work with stream selector" do
      wrong_stream_id = BSON::ObjectId.new
      correct_stream_id = Stream.make().id

      5.times { Message.make() }
      2.times { Message.make(:streams => wrong_stream_id) }
      3.times { Message.make(:streams => correct_stream_id) }

      s = Shell.new("stream(#{correct_stream_id}).count()")
      result = s.compute

      assert_equal 3, result[:result]
      assert_equal "count", s.operator
    end

    should "work with streams selector" do
      wrong_stream_id = BSON::ObjectId.new
      streams = [Stream.make(), Stream.make()]
      correct_stream_ids = streams.map { |s| s.id }

      5.times { Message.make() }
      2.times { Message.make(:streams => wrong_stream_id) }
      3.times { Message.make(:streams => correct_stream_ids[0]) }
      4.times { Message.make(:streams => correct_stream_ids[1]) }
      1.times { Message.make(:streams => correct_stream_ids) }

      s = Shell.new("stream(#{correct_stream_ids.join(',')}).count()")
      result = s.compute

      assert_equal 8, result[:result]
      assert_equal "count", s.operator
    end

    should "raise MissingStreamTargetException for empty stream target" do
      tests = Array.new
      tests << "stream().find()"
      tests << "streams().find()"
      tests << "streams.find()"
      tests << "stream.find()"
      tests << "stream().find(_foo = 9001)"

      tests.each do |test|
        assert_raises MissingStreamTargetException do
          s = Shell.new(test)
          s.compute
        end
      end
    end

    should "work with stream shortname" do
      stream = Stream.make(:shortname => "lolstream")
      wrong_stream_id = BSON::ObjectId.new

      5.times { Message.make(:streams => stream.id )}
      2.times { Message.make(:streams => wrong_stream_id) }
      1.times { Message.make() }

      s = Shell.new("stream(lolstream).count()")
      result = s.compute

      assert_equal 5, result[:result]
      assert_equal "count", s.operator
    end

    should "work with multiple streams shortnames" do
      stream1 = Stream.make(:shortname => "lolstream")
      stream2 = Stream.make(:shortname => "zomgstream")
      wrong_stream_id = BSON::ObjectId.new

      4.times { Message.make(:streams => stream1.id )}
      6.times { Message.make(:streams => stream2.id )}
      2.times { Message.make(:streams => wrong_stream_id) }
      1.times { Message.make() }

      s = Shell.new("streams(lolstream, zomgstream).count()")
      result = s.compute

      assert_equal 10, result[:result]
      assert_equal "count", s.operator
    end

    should "work with both stream shortnames and ids" do
      stream1 = Stream.make(:shortname => "lolstream")
      stream2 = Stream.make(:shortname => "zomgstream")
      wrong_stream_id = BSON::ObjectId.new

      3.times { Message.make(:streams => stream1.id )}
      2.times { Message.make(:streams => stream2.id )}
      2.times { Message.make(:streams => wrong_stream_id) }
      1.times { Message.make() }

      s = Shell.new("streams(#{stream1.id}, #{stream2.shortname}).count()")
      result = s.compute

      assert_equal 5, result[:result]
      assert_equal "count", s.operator
    end

    should "correctly report invalid stream id" do
      existing_stream = Stream.make(:shortname => "zomg")
      tests = Array.new
      tests << "stream(#{BSON::ObjectId.new}).find()"
      tests << "streams(#{BSON::ObjectId.new}, #{existing_stream.id}).find()"
      tests << "streams(wat).find()"
      tests << "streams(wat,#{existing_stream.shortname}).count()"

      tests.each do |test|
        assert_raises UnknownStreamException do
          s = Shell.new(test)
          s.compute
        end
      end
    end

  end

  context "counting" do

    should "count all with no options" do
      17.times { Message.make }
      s = Shell.new("all.count()")
      result = s.compute

      assert_equal "count", result[:operation]
      assert_equal 17, result[:result]
    end

    should "count all with options" do
      10.times { Message.make(:host => "example.org") }
      15.times { Message.make(:host => "example.com") }

      s = Shell.new('all.count(host = "example.com")')
      result = s.compute

      assert_equal "count", result[:operation]
      assert_equal 15, result[:result]
    end

    should "count all with options including integer option" do
      3.times { Message.make(:host => "example.org", :_http_response_code => 500) }
      7.times { Message.make(:host => "example.com", :_http_response_code => 500) }
      8.times { Message.make(:host => "example.org", :_http_response_code => 200) }

      s = Shell.new('all.count(host = "example.org", _http_response_code = 500)')
      result = s.compute

      assert_equal "count", result[:operation]
      assert_equal 3, result[:result]
    end

  end

  context "finding" do

    should "find with no options" do
      3.times { Message.make }

      s = Shell.new('all.find()')
      result = s.compute

      assert_equal 3, result[:result].count
    end

    should "find a simple message" do
      Message.make(:message => "foo")
      Message.make(:host => "bar.example.org", :message => "bar")

      s = Shell.new('all.find(message = "bar")')
      result = s.compute

      assert_equal "find", result[:operation]
      assert_equal "bar.example.org", result[:result][0].host
      assert_equal "bar", result[:result][0].message
    end

    should "find multiple messages" do
      Message.make(:message => "foo")
      5.times { Message.make(:host => "bar.example.org", :message => "bar") }

      s = Shell.new('all.find(message = "bar")')
      result = s.compute

      assert_equal "find", result[:operation]
      assert_equal 5, result[:result].count
      result[:result].each do |message|
        assert_equal "bar.example.org", message.host
        assert_equal "bar", message.message
      end
    end

    should "find multiple messages with combined options and regex" do
      pattern = /^ba.\.example.(com|org)/

      3.times { Message.make(:host => "foo.example.org", :message => "foo") }
      5.times { Message.make(:host => "bar.example.org", :message => "bar") }
      2.times { Message.make(:host => "baz.example.com", :message => "bar") }

      s = Shell.new("all.find(host = /#{pattern.source}/, message = \"bar\")")
      result = s.compute

      assert_equal "find", result[:operation]
      assert_equal 7, result[:result].count
      result[:result].each do |message|
        assert_match pattern, message.host
        assert_equal "bar", message.message
      end
    end

    should "find with conditional operators" do
      3.times { Message.make(:host => "example.org", :_http_response_code => 500) }
      7.times { Message.make(:host => "example.com", :_http_response_code => 500) }
      8.times { Message.make(:host => "example.com", :_http_response_code => 200) }
      10.times { Message.make(:host => "example.com", :_http_response_code => 201) }
      1.times { Message.make(:host => "example.com", :_http_response_code => 300) }

      s = Shell.new('all.find(host != "example.org", _http_response_code < 300, _http_response_code >= 200)')
      result = s.compute

      assert_equal "find", result[:operation]
      assert_equal 18, result[:result].count
    end

    should "find in a stream" do
      correct_stream_id = Stream.make().id
      wrong_stream_id = BSON::ObjectId.new
      2.times { Message.make(:streams => correct_stream_id) }
      3.times { Message.make(:streams => wrong_stream_id) }

      s = Shell.new("stream(#{correct_stream_id}).find()")
      result = s.compute

      assert_equal "find", result[:operation]
      assert_equal 2, result[:result].count
      result[:result].each { |r| assert_equal correct_stream_id, r.streams }
    end

    should "find in streams" do
      streams = [Stream.make(), Stream.make()]
      correct_stream_ids = streams.map { |s| s.id }
      wrong_stream_id = BSON::ObjectId.new
      5.times { Message.make(:streams => correct_stream_ids[0]) }
      2.times { Message.make(:streams => correct_stream_ids[1]) }
      2.times { Message.make(:streams => correct_stream_ids) }
      3.times { Message.make(:streams => wrong_stream_id) }

      s = Shell.new("stream(#{correct_stream_ids.join(',')}).find()")
      result = s.compute

      assert_equal "find", result[:operation]
      assert_equal 9, result[:result].count
    end

    should "find in a stream with options" do
      correct_stream_id = Stream.make().id
      wrong_stream_id = BSON::ObjectId.new
      4.times { Message.make(:host => "baz.example.org", :_foo => 12, :streams => correct_stream_id) }
      3.times { Message.make(:host => "bar.example.com", :_foo => 9001, :streams => correct_stream_id) }
      8.times { Message.make(:host => "bar.example.org", :_foo => 50, :streams => correct_stream_id) }
      4.times { Message.make(:host => "foo.example.org", :_foo => 5, :streams => correct_stream_id) }
      6.times { Message.make(:host => "baz.example.org", :_foo => 12, :streams => wrong_stream_id) }

      s = Shell.new("stream(#{correct_stream_id}).find(host = /^ba.\.example.(com|org)/, _foo > 10)")
      result = s.compute

      assert_equal "find", result[:operation]
      assert_equal 15, result[:result].count
    end

    should "find in streams with options" do
      streams = [Stream.make(), Stream.make()]
      correct_stream_ids = streams.map { |s| s.id }
      wrong_stream_id = BSON::ObjectId.new
      6.times { Message.make(:host => "baz.example.org", :_foo => 12, :streams => correct_stream_ids[0]) }
      1.times { Message.make(:host => "bar.example.com", :_foo => 9001, :streams => correct_stream_ids[1]) }
      4.times { Message.make(:host => "bar.example.org", :_foo => 50, :streams => correct_stream_ids) }
      2.times { Message.make(:host => "bar.example.org", :_foo => 1, :streams => correct_stream_ids[0]) }
      2.times { Message.make(:host => "foo.example.org", :_foo => 5, :streams => correct_stream_ids[0]) }
      6.times { Message.make(:host => "baz.example.org", :_foo => 12, :streams => wrong_stream_id) }

      s = Shell.new("streams(#{correct_stream_ids.join(',')}).find(host = /^ba.\.example.(com|org)/, _foo > 10)")
      result = s.compute

      assert_equal "find", result[:operation]
      assert_equal 11, result[:result].count
    end

  end

  context "distinction" do

    should "distinct with no query options" do
      4.times { Message.make(:host => "baz.example.org") }
      3.times { Message.make(:host => "bar.example.com") }

      s = Shell.new("all.distinct({host})")
      result = s.compute

      assert_equal "distinct", result[:operation]
      assert_equal ["baz.example.org", "bar.example.com"], result[:result]
    end

    should "distinct with some query options" do
      4.times { Message.make(:host => "baz.example.org", :_foo => "bar", :_something => "foo") }
      3.times { Message.make(:host => "bar.example.com", :_foo => "bar", :_something => "foo") }
      3.times { Message.make(:host => "foo.example.com", :_foo => "baz", :_something => "foo") }
      3.times { Message.make(:host => "wat.example.com", :_foo => "baz", :_something => "bar") }

      s = Shell.new('all.distinct({host}, _foo = "bar", _something = "foo")')
      result = s.compute

      assert_equal "distinct", result[:operation]
      assert_equal ["baz.example.org", "bar.example.com"], result[:result]
    end

    should "distinct with one query option" do
      4.times { Message.make(:host => "baz.example.org", :_foo => "bar") }
      3.times { Message.make(:host => "example.com", :_foo => "bar") }
      5.times { Message.make(:host => "foo.example.com", :_foo => "baz") }

      s = Shell.new('all.distinct({host}, _foo = "bar")')
      result = s.compute

      assert_equal "distinct", result[:operation]
      assert_equal ["baz.example.org", "example.com"], result[:result]
    end

    should "distinct with regex query options" do
      4.times { Message.make(:host => "baz.example.org", :_foo => "bar1") }
      3.times { Message.make(:host => "example.com", :_foo => "bar1") }
      5.times { Message.make(:host => "foo.example.com", :_foo => "baz") }

      s = Shell.new('all.distinct({host}, _foo = /^bar\d$/)')
      result = s.compute

      assert_equal "distinct", result[:operation]
      assert_equal ["baz.example.org", "example.com"], result[:result]
    end

    should "distinct with no query options on stream" do
      correct_stream = Stream.make()
      wrong_stream_id = BSON::ObjectId.new
      4.times { Message.make(:host => "baz.example.org", :streams => correct_stream.id) }
      10.times { Message.make(:host => "not.example.org", :streams => wrong_stream_id) }
      3.times { Message.make(:host => "bar.example.com", :streams => correct_stream.id) }

      s = Shell.new("stream(#{correct_stream.id}).distinct({host})")
      result = s.compute

      assert_equal "distinct", result[:operation]
      assert_equal ["baz.example.org", "bar.example.com"], result[:result]
    end

    should "distinct with one query option on stream" do
      correct_stream = Stream.make()
      wrong_stream_id = BSON::ObjectId.new
      4.times { Message.make(:host => "baz.example.org", :streams => correct_stream.id, :_foo => "bar") }
      4.times { Message.make(:host => "foo.example.org", :streams => correct_stream.id, :_foo => "moo") }
      10.times { Message.make(:host => "not.example.org", :streams => wrong_stream_id, :_foo => "bar" ) }
      3.times { Message.make(:host => "bar.example.com", :streams => correct_stream.id, :_foo => "bar") }

      s = Shell.new("stream(#{correct_stream.id}).distinct({host}, _foo = \"bar\")")
      result = s.compute

      assert_equal "distinct", result[:operation]
      assert_equal ["baz.example.org", "bar.example.com"], result[:result]
    end

    should "distinct with no query options on streams" do
      streams = [ Stream.make(), Stream.make() ]
      correct_stream_ids = streams.map {|s| s.id }
      wrong_stream_id = BSON::ObjectId.new
      4.times { Message.make(:host => "baz.example.org", :streams => correct_stream_ids[0]) }
      10.times { Message.make(:host => "not.example.org", :streams => wrong_stream_id) }
      3.times { Message.make(:host => "bar.example.com", :streams => correct_stream_ids[1]) }
      2.times { Message.make(:host => "foo.example.com", :streams => correct_stream_ids) }

      s = Shell.new("streams(#{correct_stream_ids.join(',')}).distinct({host})")
      result = s.compute

      assert_equal "distinct", result[:operation]
      assert_equal ["baz.example.org", "bar.example.com", "foo.example.com"], result[:result]
    end

  end

  context "distribution" do

    should "work with no query options" do
      4.times { Message.make(:host => "baz.example.org") }
      3.times { Message.make(:host => "bar.example.com") }

      s = Shell.new("all.distribution({host})")
      result = s.compute

      assert_equal "distribution", result[:operation]
      assert_equal [
        { :distinct => "baz.example.org", :count => 4 },
        { :distinct => "bar.example.com", :count => 3 },
      ], result[:result]
    end

    should "work with some query options" do
      4.times { Message.make(:host => "baz.example.org", :_foo => "bar", :_something => "foo") }
      3.times { Message.make(:host => "bar.example.com", :_foo => "bar", :_something => "foo") }
      3.times { Message.make(:host => "foo.example.com", :_foo => "baz", :_something => "foo") }
      3.times { Message.make(:host => "wat.example.com", :_foo => "baz", :_something => "bar") }

      s = Shell.new('all.distribution({host}, _foo = "bar", _something = "foo")')
      result = s.compute

      assert_equal "distribution", result[:operation]
      assert_equal [
        { :distinct => "baz.example.org", :count => 4 },
        { :distinct => "bar.example.com", :count => 3 }
      ], result[:result]
    end

    should "work with one query option" do
      4.times { Message.make(:host => "baz.example.org", :_foo => "bar") }
      3.times { Message.make(:host => "example.com", :_foo => "bar") }
      5.times { Message.make(:host => "foo.example.com", :_foo => "baz") }

      s = Shell.new('all.distribution({host}, _foo = "bar")')
      result = s.compute

      assert_equal "distribution", result[:operation]
      assert_equal [
        { :distinct => "baz.example.org", :count => 4 },
        { :distinct => "example.com", :count => 3 }
      ], result[:result]
    end

    should "work with regex query options" do
      4.times { Message.make(:host => "baz.example.org", :_foo => "bar1") }
      3.times { Message.make(:host => "example.com", :_foo => "bar1") }
      5.times { Message.make(:host => "foo.example.com", :_foo => "baz") }

      s = Shell.new('all.distribution({host}, _foo = /^bar\d$/)')
      result = s.compute

      assert_equal "distribution", result[:operation]
      assert_equal [
        { :distinct => "baz.example.org", :count => 4 },
        { :distinct => "example.com", :count => 3 }
      ], result[:result]
    end

    should "work with no query options on stream" do
      correct_stream = Stream.make()
      wrong_stream_id = BSON::ObjectId.new
      4.times { Message.make(:host => "baz.example.org", :streams => correct_stream.id) }
      10.times { Message.make(:host => "not.example.org", :streams => wrong_stream_id) }
      3.times { Message.make(:host => "bar.example.com", :streams => correct_stream.id) }

      s = Shell.new("stream(#{correct_stream.id}).distribution({host})")
      result = s.compute

      assert_equal "distribution", result[:operation]
      assert_equal [
       { :distinct => "baz.example.org", :count => 4 },
       { :distinct => "bar.example.com", :count => 3 }
      ], result[:result]
    end

    should "work with one query option on stream" do
      correct_stream = Stream.make()
      wrong_stream_id = BSON::ObjectId.new
      4.times { Message.make(:host => "baz.example.org", :streams => correct_stream.id, :_foo => "bar") }
      4.times { Message.make(:host => "foo.example.org", :streams => correct_stream.id, :_foo => "moo") }
      10.times { Message.make(:host => "not.example.org", :streams => wrong_stream_id, :_foo => "bar" ) }
      3.times { Message.make(:host => "bar.example.com", :streams => correct_stream.id, :_foo => "bar") }

      s = Shell.new("stream(#{correct_stream.id}).distribution({host}, _foo = \"bar\")")
      result = s.compute

      assert_equal "distribution", result[:operation]
      assert_equal [
        { :distinct => "baz.example.org", :count => 4 },
        { :distinct => "bar.example.com", :count => 3 }
      ], result[:result]
    end

    should "distinct with no query options on streams" do
      streams = [ Stream.make(), Stream.make() ]
      correct_stream_ids = streams.map {|s| s.id }
      wrong_stream_id = BSON::ObjectId.new
      4.times { Message.make(:host => "baz.example.org", :streams => correct_stream_ids[0]) }
      10.times { Message.make(:host => "not.example.org", :streams => wrong_stream_id) }
      3.times { Message.make(:host => "bar.example.com", :streams => correct_stream_ids[1]) }
      2.times { Message.make(:host => "foo.example.com", :streams => correct_stream_ids) }

      s = Shell.new("streams(#{correct_stream_ids.join(',')}).distribution({host})")
      result = s.compute

      assert_equal "distribution", result[:operation]
      assert_equal [
        { :distinct => "baz.example.org", :count => 4 },
        { :distinct => "bar.example.com", :count => 3 },
        { :distinct => "foo.example.com", :count => 2 }
      ], result[:result]
    end

  end

end
