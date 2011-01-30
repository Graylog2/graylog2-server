class FilteredTerm < ActiveRecord::Base

  validates_presence_of :term

  def exist?
    self.count > 0
  end

end
