class Message
  include MongoMapper::Document

  set_database_name 'graylog2'

  key :message, String
  key :level, Integer
  key :facility, Integer
end
