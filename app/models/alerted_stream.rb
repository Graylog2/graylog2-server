class AlertedStream
  include Mongoid::Document

  referenced_in :user
  referenced_in :stream

  JOB_TITLE = "streamalarm_check"

  def self.alerted?(stream_id, user_id)
    self.count(:conditions => { :user_id => user_id, :stream_id => stream_id }) > 0
  end

  def self.all_subscribers(stream)
    emails = Array.new
    subscribers = Array.new

    if stream.alarm_force
      User.all.each do |u|
        next if u.email.blank?
        emails << u.email
      end
    else
      self.where(:stream_id => stream.id).each do |s|
        next if s.user.blank? or s.user.email.blank?
        emails << s.user.email
      end
    end

    return emails
  end

end
