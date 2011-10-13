class UnsupportedResultType < StandardError; end

# Overwrite to allow setting of document type
#  - https://github.com/karmi/tire/issues/96
module Tire::Model::Naming::ClassMethods
  def document_type(name=nil)
    @document_type = name if name
    @document_type || klass.model_name.singular
  end
end

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
    search("*", pagination_options(page).merge(@default_query_options))
  end

  def self.all_of_stream_paginated(stream_id, page = 1)
    search("streams:#{stream_id}", pagination_options(page).merge(@default_query_options))
  end

  def self.retrieve_by_id(id)
    @index.retrieve(TYPE_NAME, id)
  end

  def self.all_by_quickfilter filters, page = 1
    search pagination_options(page).merge(@default_query_options) do
      query do
        boolean do
          # Short message
          must { string("message:#{filters[:message]}") } unless filters[:message].blank?

          # Facility
          must { term(:facility, filters[:facility]) } unless filters[:facility].blank?

          # Severity
          if !filters[:severity].blank? and filters[:severity_above].blank?
            must { term(:level, filters[:severity]) }
          end

          # Host
          must { term(:host, filters[:host]) } unless filters[:host].blank?

          # XXX ELASTIC hostgroup missing.
      
          # Additional fields.
          Quickfilter.extract_additional_fields_from_request(filters).each do |key, value|
            must { term("_#{key}".to_sym, value) }
          end
        end
      end
      
      # Severity (or higher)
      if !filters[:severity].blank? and !filters[:severity_above].blank?
        filter 'range', { :level => { :to => filters[:severity].to_i } }
      end

      # Timeframe.
      if !filters[:date].blank?
        range = Quickfilter.get_conditions_timeframe(filters[:date])
        filter 'range', { :created_at => { :gt => range[:greater], :lt => range[:lower],  } }
      end

    end
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
    search("*", { :sort => "created_at asc", :size => 1 }).first
  end

  def self.all_in_range(page, from, to)
    search pagination_options(page).merge(@default_query_options) do
      query { string("*") }
        
      filter 'range', { :created_at => { :gte => from, :lte => to } }
    end
  end

  private

  def self.pagination_options(page)
    page = 1 if page.blank?

    { :per_page => Message::LIMIT, :page => page }
  end
  
end
