class Host
  include MongoMapper::Document

  set_database_name 'graylog2'

  key :host, String
  key :message_count, Float
end
