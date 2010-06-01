class Systemstatistic
  include MongoMapper::Document

  set_database_name 'graylog2'

  key :handled_syslog_events, Float
end
