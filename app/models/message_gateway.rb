class UnsupportedResultType < StandardError; end

# Overwrite to allow setting of document type
#  - https://github.com/karmi/tire/issues/96
module Tire::Model::Naming::ClassMethods
  def document_type(name=nil)
    @document_type = name if name
    @document_type || klass.model_name.singular
  end
end

# XXX ELASTIC: possibly rename to MessageSearch or similar as it is not always returning Message objects
#              but can also return e.g. counts (integer)
class MessageGateway
  include Tire::Model::Search
  include Mongoid::Document

  # XXX ELASTIC: make configurable. also host etc
  INDEX_NAME = "graylog2"
  TYPE_NAME = "message"

  index_name(INDEX_NAME)
  document_type(TYPE_NAME)

  @index = Tire.index(INDEX_NAME)
  @default_query_options = { :sort => "created_at desc" }

  def self.all_paginated(page = 1)
    page = 1 if page.blank?

    wrap search("*", { :per_page => Message::LIMIT, :page => page }.merge(@default_query_options))
  end

  def self.all_of_stream_paginated(stream_id, page = 1)
    page = 1 if page.blank?

    wrap search("streams:#{stream_id}", { :per_page => Message::LIMIT, :page => page }.merge(@default_query_options))
  end

  def self.retrieve_by_id(id)
    wrap @index.retrieve(TYPE_NAME, id)
  end

  def self.count_of_last_minutes(x)
    # Delegating for backward compatibility reasons. Should be removed soon. (XXX ELASTIC)
    MessageCount.total_count_of_last_minutes(x)
  end
  
  def self.total_count
    # search with size 0 instead of count because of this issue: https://github.com/karmi/tire/issues/100
    search("*", :size => 0).total
  end

  def self.stream_count(stream_id)
    # search with size 0 instead of count because of this issue: https://github.com/karmi/tire/issues/100
    search("streams:#{stream_id}", :size => 0).total
  end

  private
  def self.wrap(x)
    case(x)
      when Tire::Results::Item then Message.parse_from_elastic(x)
      when Tire::Results::Collection then wrap_collection(x)
      else
        Rails.logger.error "Unsupported result type while trying to wrap ElasticSearch response: #{x.class}"
        raise UnsupportedResultType
    end
  end

  def self.wrap_collection(c)
    c.results.map { |i| wrap(i) }
  end
end
