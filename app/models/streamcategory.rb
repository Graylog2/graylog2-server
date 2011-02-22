class Streamcategory < ActiveRecord::Base

  has_many :streams

  validates_presence_of :title

end
