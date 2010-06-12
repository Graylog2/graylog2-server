class Stream < ActiveRecord::Base
  has_many :streamrules

  validates_presence_of :title
end
