class Messagecomment
  include Mongoid::Document
  include Mongoid::Timestamps

  references_one :user

  field :title, :type => String
  field :comment, :type => String
  field :match, :type => String
  field :user_id, :type => BSON::ObjectId

  validates_presence_of :title
  validates_presence_of :comment
  validates_presence_of :match
  validates_presence_of :user_id

  validate :valid_regex

  def self.all_matched(message)
    matched = Array.new
    self.all.each do |c|
      matched << c if message.message =~ /#{c.match}/
    end

    return matched
  end

  private

  def valid_regex
    begin
      String.new =~ /#{match}/
    rescue RegexpError
      errors.add(:value, "invalid regular expression")
    end
  end

end
