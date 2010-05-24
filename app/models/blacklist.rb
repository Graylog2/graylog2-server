class Blacklist < ActiveRecord::Base
  has_many :blacklisted_terms
end
