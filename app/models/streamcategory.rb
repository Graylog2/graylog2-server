class Streamcategory
  include Mongoid::Document

  references_many :streams

  validates_presence_of :title

  field :title, :type => String

  def has_accessable_streams_for_user?(user)
    return true if user.role == "admin"
    
    allowed_streams = user.streams.collect { |s| s.id.to_s }

    self.streams.each do |stream|
      return true if allowed_streams.include?(stream.id.to_s)
    end

    false
  end

end
