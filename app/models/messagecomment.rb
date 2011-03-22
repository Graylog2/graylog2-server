class Messagecomment
  include Mongoid::Document
  
  references_one :user

  field :title, :type => String
  field :comment, :type => String
  field :match, :type => String
  
  validates_presence_of :title
  validates_presence_of :comment
  validates_presence_of :match
  validates_presence_of :user_id

  def self.all_matched(message)
    matched = Array.new
    self.all.each do |c|
      matched << c if message.message =~ /#{c.match}/
    end

    return matched
  end

end
