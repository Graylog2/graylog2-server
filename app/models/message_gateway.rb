class UnsupportedResultType < StandardError; end

class MessageGateway
  include Tire::Model::Search
  include Mongoid::Document

  INDEX_NAME = "graylog2"
  TYPE_NAME = "message"

  index_name(INDEX_NAME)

  @index = Tire.index(INDEX_NAME)

  def self.all_paginated(page = 1)
    page = 1 if page.blank?

    wrap search("*", :per_page => Message::LIMIT, :page => page, :sort => "created_at desc" )
  end

  def self.retrieve_by_id(id)
    wrap @index.retrieve(TYPE_NAME, params[:id])
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
    Rails.logger.info "WRRRRRRRRRRRRRRRRRRRRRRR: #{c.results.inspect}"
    c.results.map { |i| wrap(i) }
  end
end
