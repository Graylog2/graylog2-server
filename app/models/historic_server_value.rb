class HistoricServerValue
  include Mongoid::Document

  key :type, String
  key :created_at, Integer

  def self.used_memory(minutes)
    get("used_memory", minutes)
  end

  private
  def self.get(what, minutes)
    self.all(:conditions => { :type => what }, :sort => ["$natural", "descending"], :limit => minutes).collect { |v| [v.created_at*1000, (v.value/1024/1024).to_i] }
  end

end
