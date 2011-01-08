class MessageCollection

  def self.storage_size
    (stats["storageSize"] || 0).to_i
  end

  def self.size
    (stats["size"] || 0).to_i
  end

  def self.index_size
    (stats["totalIndexSize"] || 0).to_i
  end

  def self.average_object_size
    x = stats["avgObjSize"].to_f
    x.nan? ? 0 : x
  end

  def self.is_capped?
    stats["capped"] == 1 ? true : false
  end

  def self.count
    (stats["count"] || 0).to_i
  end

  private

  def self.stats
    begin
      return Message.collection.stats
    rescue
      return Hash.new
    end
  end

end
