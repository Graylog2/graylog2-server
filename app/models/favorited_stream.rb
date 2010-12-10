class FavoritedStream < ActiveRecord::Base
  belongs_to :stream

  def self.all_of_user(user_id)
    self.find_all_by_user_id(user_id)
  end
end
