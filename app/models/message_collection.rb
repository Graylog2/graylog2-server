class MessageCollection

  def self.storage_size
    stats["storageSize"].to_i
  end

  def self.size
    stats["size"].to_i
  end

  def self.index_size
    stats["totalIndexSize"].to_i
  end

  def self.average_object_size
    stats["avgObjSize"].to_f
  end

  def self.is_capped?
    stats["capped"] == 1 ? true : false
  end

  def self.count
    stats["count"].to_i
  end

  private

  def self.stats
    Message.collection.stats
  end

end
