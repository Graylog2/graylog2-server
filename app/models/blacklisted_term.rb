class BlacklistedTerm < ActiveRecord::Base
  belongs_to :blacklist

  validates_presence_of :term
  validates_presence_of :blacklist_id

  def self.all_as_array
    self.all.collect { |t| /#{t.term}/ }
  end
end
