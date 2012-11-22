class UnsupportedResultType < StandardError; end

# Overwrite to allow setting of document type
#  - https://github.com/karmi/tire/issues/96
module Tire::Model::Naming::ClassMethods
  def document_type(name=nil)
    @document_type = name if name
    @document_type || klass.model_name.singular
  end
end

# monkey patch, shmonkey patch (Raising Timeout from 60s to no timeout)
module Tire::HTTP::Client
  class RestClient
    def self.get(url, data=nil)
      perform ::RestClient::Request.new(:method => :get, :url => url, :payload => data, :timeout => -1).execute
    rescue ::RestClient::Exception => e
      Tire::HTTP::Response.new e.http_body, e.http_code
    end
  end
end

# XXX ELASTIC: try curb as HTTP adapter for tire. reported to be faster: https://gist.github.com/1204159
class MessageGateway
  include Tire::Model::Search
  include Mongoid::Document

  # used if not set in config
  DEFAULT_INDEX_PREFIX = "graylog2"
  DEFAULT_RECENT_INDEX_NAME = "graylog2_recent"

  # XXX ELASTIC: sucks.
  if Rails.env == "test"
    ALL_INDICES_ALIAS = "graylog2_test"
    RECENT_INDEX_NAME = "graylog2_recent_test"
  else
    indices_prefix = Configuration.indexer_index_prefix.blank? ? DEFAULT_INDEX_PREFIX : Configuration.indexer_index_prefix
    config_recent_index = Configuration.indexer_recent_index_name
    config_recent_index.blank? ? RECENT_INDEX_NAME = DEFAULT_RECENT_INDEX_NAME : RECENT_INDEX_NAME = config_recent_index

    # Wildcard Multi Index Syntax
    # http://www.elasticsearch.org/blog/2012/07/02/0.19.8-released.html
    ALL_INDICES_ALIAS = "#{indices_prefix}_*,-#{RECENT_INDEX_NAME}"
  end

  TYPE_NAME = "message"

  index_name(ALL_INDICES_ALIAS)
  document_type(TYPE_NAME)

  @index = Tire.index(ALL_INDICES_ALIAS)
  @default_query_options = { :sort => "created_at desc" }

  def self.all_paginated(page = 1, opts = {})
    (opts[:all].blank? or opts[:all] == false) ? use_recent_index! : use_all_indices!

    r = search(pagination_options(page).merge(@default_query_options)) do
      query { all }
    end

    wrap(r)
  end

  def self.all_of_stream_paginated(stream_id, page = 1, opts = {})
    (opts[:all].blank? or opts[:all] == false) ? use_recent_index! : use_all_indices!

    r = search(pagination_options(page).merge(@default_query_options)) do
      query { all }
      filter :term, :streams => stream_id
    end

    wrap(r)
  end

  def self.all_of_host_paginated(hostname, page = 1, opts = {})
    (opts[:all].blank? or opts[:all] == false) ? use_recent_index! : use_all_indices!

    r = search(pagination_options(page).merge(@default_query_options)) do
      query { all }
      filter :term, :host => hostname
    end

    wrap(r)
  end

  def self.retrieve_by_id(id)
    use_all_indices!
    wrap search("_id:#{id}").first
  end

  def self.dynamic_search(what, with_default_query_options = false)
    use_all_indices!

    what = what.merge({:sort => { :created_at => :desc }}) if with_default_query_options
    wrap Tire.search(ALL_INDICES_ALIAS, what)
  end

  def self.universal_search(page = 1, query, opts)
    use_all_indices!

    histogram_only = !opts[:date_histogram].blank? and opts[:date_histogram] == true

    r = search(pagination_options(page).merge(@default_query_options)) do
      query { string(query) }

      filter :term, :streams => opts[:stream].id if opts[:stream]
      filter :term, :host => opts[:host].host if opts[:host]

      # Request date histogram facet?
      if histogram_only
        facet 'date_histogram' do
          date("histogram_time", :interval => (opts[:date_histogram_interval]))
        end
      end
    end

    return r.facets["date_histogram"]["entries"] if histogram_only rescue return []

    wrap(r)
  end

  def self.dynamic_distribution(target, query)
    use_all_indices!

    result = Array.new

    query[:facets] = {
      "distribution_result" => {
        "terms" => {
          "field" => target,
          "all_terms" => true,
          "size" => 99999
        }
      }
    }

    r = Tire.search(ALL_INDICES_ALIAS, query)

    # [{"term"=>"baz.example.org", "count"=>4}, {"term"=>"bar.example.com", "count"=>3}]
    r.facets["distribution_result"]["terms"].each do |r|
      next if r["count"] == 0 # ES returns the count for *every* field. Skip those that had no matches.
      result << { :distinct => r["term"], :count => r["count"] }
    end

    return result
  end

  def self.all_by_quickfilter(filters, page = 1, opts = {})
    use_all_indices!

    histogram_only = !opts[:date_histogram].blank? and opts[:date_histogram] == true

    if histogram_only
      options = nil
    else
      options = pagination_options(page).merge(@default_query_options)
    end

    r = search(options) do
      query do
        # If no message or full_message are set, we are doing a pure filter query.
        if filters[:message].blank? and filters[:full_message].blank?
          all
        else
          boolean do
            # Short message
            must { string("message:#{filters[:message]}") } unless filters[:message].blank?

            # Full message
            must { string("full_message:#{filters[:full_message]}") } unless filters[:full_message].blank?
          end
        end
      end

      # Stream
      unless opts[:stream_id].blank?
        filter :term, :streams => opts[:stream_id]
      end

      # Host (one is the actual input field, one is the message context)
      unless opts[:hostname].blank?
        filter :term, :host => opts[:hostname]
      end
      unless opts[:host].blank?
        filter :term, :host => opts[:host]
      end

      # Timeframe.
      if !filters[:date].blank?
        range = Quickfilter.get_conditions_timeframe(filters[:date])
        filter :range, :created_at => { :gt => range[:greater], :lt => range[:lower] }
      end

      # Facility
      filter :term, :facility => filters[:facility] unless filters[:facility].blank?

      # Severity
      if !filters[:severity].blank? and filters[:severity_above].blank?
        filter :term, :level => filters[:severity]
      end

      # Severity (or higher)
      if !filters[:severity].blank? and !filters[:severity_above].blank?
        filter :range, :level => { :to => filters[:severity].to_i }
      end

      # File name
      filter :term, :file => filters[:file] unless filters[:file].blank?

      # Line number
      filter :term, :line => filters[:line] unless filters[:line].blank?

      # Additional fields.
      Quickfilter.extract_additional_fields_from_request(filters).each do |key, value|
        filter :term, "_#{key}".to_sym => value
      end

      # Request date histogram facet?
      if histogram_only
        facet 'date_histogram' do
          date("histogram_time", :interval => (opts[:date_histogram_interval]))
        end
      end

    end

    return r.facets["date_histogram"]["entries"] if histogram_only rescue return []

    return wrap(r)
  end

  def self.total_count
    use_all_indices!

    # search with size 0 instead of count because of this issue: https://github.com/karmi/tire/issues/100
    search(:size => 0) do
      query { all }
    end.total
  end

  def self.stream_count(stream_id)
    use_all_indices!

    # search with size 0 instead of count because of this issue: https://github.com/karmi/tire/issues/100
    search(:size => 0) do
      query { all }
      filter :term, :streams => stream_id
    end.total
  end

  def self.host_count(hostname)
    use_all_indices!

    # search with size 0 instead of count because of this issue: https://github.com/karmi/tire/issues/100
    search(:size => 0) do
      query { all }
      filter :term, :host => hostname
    end.total
  end

  def self.oldest_message
    use_all_indices!

    r = search({ :sort => "created_at asc", :size => 1 }) do
      query { all }
    end.first

    wrap(r)
  end

  def self.all_in_range(page, from, to, opts = {})
    raise "You can only pass stream_id OR hostname" if !opts[:stream_id].blank? and !opts[:hostname].blank?
  
    use_all_indices!
    options = pagination_options(page).merge(@default_query_options)

    r = search(options) do
      query { all }

      unless opts[:stream_id].blank?
        filter :term, :streams => opts[:stream_id]
      end

      unless opts[:hostname].blank?
        filter :term, :host => opts[:hostname]
      end

      filter 'range', { :created_at => { :gte => from, :lte => to } }
    end

    wrap(r)
  end

  def self.delete_message(id)
    result = Tire.index(ALL_INDICES_ALIAS).remove(TYPE_NAME, id)
    Tire.index(ALL_INDICES_ALIAS).refresh
    return false if result.nil? or result["ok"] != true

    return true
  end

  # Returns how the text is broken down to terms.
  def self.analyze(text, index, field = "message")
    result = Tire.index(index).analyze(text, :field => "message.#{field}")
    return Array.new if result == false

    result["tokens"].map { |t| t["token"] }
  end

  def self.message_mapping(index)
    Tire.index(index).mapping["message"] rescue {}
  end

  def self.mapping_valid?(index)
    mapping = message_mapping(index)

    store_generic = mapping["dynamic_templates"][0]["store_generic"]
    return false if store_generic["mapping"]["index"] != "not_analyzed"
    return false if store_generic["match"] != "*"

    true
  rescue
    false
  end

  def self.all_additional_fields
    message_mapping(RECENT_INDEX_NAME)["properties"].keys.delete_if{ |field| field.at(0) != '_' or field.length < 2 }.map{ |field| field[1..-1].to_sym }
  rescue
    []
  end

  private

  def self.use_all_indices!
    index_name(ALL_INDICES_ALIAS)
  end

  def self.use_recent_index!
    index_name(RECENT_INDEX_NAME)
  end

  def self.wrap(x)
    return nil if x.nil?
    case(x)
      when Tire::Results::Item then Message.parse_from_elastic(x)
      when Tire::Results::Collection then wrap_collection(x)
      else
        Rails.logger.error "Unsupported result type while trying to wrap ElasticSearch response: #{x.class}"
        raise UnsupportedResultType
    end
  end

  def self.wrap_collection(c)
    r = MessageResult.new(c.results.map { |i| wrap(i) })
    r.total_result_count = c.total
    return r
  end

  def self.pagination_options(page)
    page = 1 if page.blank?

    { :per_page => Message::LIMIT, :page => page }
  end

end
