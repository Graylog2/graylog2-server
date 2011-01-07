class Job < ActiveRecord::Base

  def self.done(title)
    delete_all("title = '#{title}'")
    
    job = Job.new
    job.title = title
    job.last_run = Time.now.to_i
    job.save
  end

  def self.last_run(title)
    job = find_by_title(title)
    return nil if job.blank? or job.last_run.blank?

    job.last_run.to_i
  end
  
  def active?(interval=15.minutes.ago.to_i)
    !last_run.blank? and (last_run > interval)
  end
  
  def self.[](name)
    find_by_title(name)
  end

end
