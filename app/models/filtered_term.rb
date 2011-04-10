class FilteredTerm
  include Mongoid::Document

  field :term, :type => String

  validates_presence_of :term

  def exist?
    self.count > 0
  end

end
