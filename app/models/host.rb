class Host
  include MongoMapper::Document

  key :host, String
  key :message_count, Float
end
