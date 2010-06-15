class Blacklist < ActiveRecord::Base
  has_many :blacklisted_terms

  validates_presence_of :title
end
