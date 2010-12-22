class AlertedStream < ActiveRecord::Base
  belongs_to :stream
  belongs_to :user

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
      self.find(:all, :conditions => ["stream_id = ?", stream.id]).each do |s|
        next if s.user.blank? or s.user.email.blank?
        emails << s.user.email
      end
    end

    return emails
  end
  
  def self.job_active?
    last_check = Stream.maximum('last_alarm_check')
    return false if last_check.blank?
    
    last_check > 15.minutes.ago
  end

end
