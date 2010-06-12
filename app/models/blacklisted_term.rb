class BlacklistedTerm < ActiveRecord::Base
  belongs_to :blacklist

  validates_presence_of :term
  validates_presence_of :blacklist_id

  def self.get_all_as_condition_hash negated = true, id = nil
    if id.blank?
      terms = self.all
    else
      terms = self.find_all_by_blacklist_id id
    end

    return nil if terms.blank?

    conditions = Array.new
    terms.each do |term|
      conditions << /#{Regexp.escape(term.term)}/
    end

    modifier = "$in"

    modifier = "$nin" if negated

   return { modifier => conditions }
  end
end
