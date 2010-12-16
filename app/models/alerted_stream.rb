class AlertedStream < ActiveRecord::Base
  belongs_to :stream

  def self.alerted?(stream_id, user_id)
    self.count(:conditions => { :user_id => user_id, :stream_id => stream_id }) > 0
  end

end
