class Graph
  include MongoMapper::Document

  key :host, String
  key :value, Integer
  key :created_at, Integer

  def self.all_of_host(host, since = 0)
    return self.all :conditions => { :host => host, :created_at => { "$gt" => since.to_i } }
  end

end
