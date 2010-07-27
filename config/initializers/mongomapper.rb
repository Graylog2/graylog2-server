db_config = YAML::load(File.read(RAILS_ROOT + "/config/database.yml"))

begin
  if db_config['mongodb']
    config = db_config['mongodb']

    MongoMapper.connection = Mongo::Connection.new(config['hostname'], config['port'], { :logger => Rails.logger })
    MongoMapper.database = config['database']
    if config['authenticate'] == true
      MongoMapper.database.authenticate(config['username'], config['password'])
    end
  end
rescue => e
  puts "ERROR: Could not connect to MongoDB or read config: #{e}"
  exit
end