class BlacklistedTerm
  include Mongoid::Document
  
  embedded_in :blacklist

  field :term, :type => String
  
  validates_presence_of :term

  def self.all_as_array
    self.all.collect { |t| /#{t.term}/ }
  end
end
