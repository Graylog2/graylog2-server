db_config = YAML::load(File.read(Rails.root.to_s + "/config/mongodb.yml"))

begin
  if db_config[Rails.env]
    config = db_config[Rails.env]
    if config['hostname'].is_a? Array
      MongoMapper.connection = Mongo::Connection.multi(config['hostname'], { :logger => Rails.logger })
    else
      MongoMapper.connection = Mongo::Connection.new(config['hostname'], config['port'], { :logger => Rails.logger, :slave_ok => true })
    end
    MongoMapper.database = config['database']
    if config['authenticate'] == true
      MongoMapper.database.authenticate(config['username'], config['password'])
    end
  end
rescue => e
  puts "ERROR: Could not connect to MongoDB or read config: #{e}"
  puts e.backtrace
  exit
end
