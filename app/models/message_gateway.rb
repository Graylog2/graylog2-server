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
# XXX ELASTIC: try curb as HTTP adapter for tire. reported to be faster: https://gist.github.com/1204159
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
    wrap search("*", pagination_options(page).merge(@default_query_options))
  end

  def self.all_of_stream_paginated(stream_id, page = 1)
    wrap search("streams:#{stream_id}", pagination_options(page).merge(@default_query_options))
  end

  def self.retrieve_by_id(id)
    # XXX ELASTIC sucks to use @index - fix.
    wrap @index.retrieve(TYPE_NAME, id)
  end

  def self.all_by_quickfilter filters, page = 1
    r = search pagination_options(page).merge(@default_query_options) do
      query do
        boolean do
          # Short message
          must { string("message:#{filters[:message]}") } unless filters[:message].blank?

          # Facility
          must { string("facility:#{filters[:facility]}") } unless filters[:facility].blank?

          # Severity
          if !filters[:severity].blank? and filters[:severity_above].blank?
            must { string("level:#{filters[:severity]}") }
          end

          # Host
          # XXX ELASTIC: no exact match here. ffffufufufufufufufu full text search
          must { string("host:#{filters[:host]}") } unless filters[:host].blank?

          # XXX ELASTIC: additional_fields missing
          # XXX ELASTIC: hostgroup missing.
          # XXX ELASTIC timeframe filter missing
        end
      end

      # Severity (or higher) - XXX ELASTIC - possible to integrate in query block above?
      if !filters[:severity].blank? and !filters[:severity_above].blank?
        filter 'range', { :level => { :to => filters[:severity].to_i } }
      end

    end

    return wrap(r)

    # XXX ELASTIC - building that string directly sucks. find a way to use the Tire DSL.
    #wrap search(quickfilter_condition_string(filters), { :per_page => Message::LIMIT, :page => page }.merge(@default_query_options))
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
    # XXX ELASTIC is that size 0 actually working?
    # search with size 0 instead of count because of this issue: https://github.com/karmi/tire/issues/100
    search("streams:#{stream_id}", :size => 0).total
  end

  def self.oldest_message
    # XXX ELASTIC is that size 0 actually working?
    wrap search("*", { :sort => "created_at asc", :size => 1 }).first
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

  def self.pagination_options(page)
    page = 1 if page.blank?

    { :per_page => Message::LIMIT, :page => page }
  end

end
