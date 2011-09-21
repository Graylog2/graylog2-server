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

  def self.retrieve_by_id(id)
    wrap @index.retrieve(TYPE_NAME, id)
  end

  def self.count_of_last_minutes(x)
    # XXX ELASTIC: returns whole result set with all the messages in it. use *real* count.
    search do
      filter 'range', {
        :created_at => { :from => x.minutes.ago.to_i, :include_upper => true  }
      }
      size 0
    end.total
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
