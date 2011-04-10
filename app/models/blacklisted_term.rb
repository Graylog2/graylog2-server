class BlacklistedTerm
  include Mongoid::Document

  embedded_in :blacklist

  field :term, :type => String

  validates_presence_of :term
  validate :valid_regex

  def self.all_as_array
    self.all.collect { |t| /#{t.term}/ }
  end

  private

  def valid_regex
    begin
      String.new =~ /#{term}/
    rescue RegexpError
      errors.add(:term, "invalid regular expression")
    end
  end
end
