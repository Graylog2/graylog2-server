class Streamcategory
  include Mongoid::Document

  references_many :streams

  validates_presence_of :title

  field :title, :type => String

end
