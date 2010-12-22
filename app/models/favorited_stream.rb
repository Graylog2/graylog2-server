class FavoritedStream < ActiveRecord::Base
  belongs_to :stream
  belongs_to :user

  def self.favorited?(stream_id, user_id)
    self.count(:conditions => { :user_id => user_id, :stream_id => stream_id }) > 0
  end

  def self.all_of_user(user_id)
    self.find_all_by_user_id(user_id)
  end
end
