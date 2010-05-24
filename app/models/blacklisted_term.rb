class BlacklistedTerm < ActiveRecord::Base
  belongs_to :blacklist

  def self.get_all_as_condition_hash negated = true, id = nil
    regex_string = String.new

    if id.blank?
      terms = self.all
    else
      terms = self.find_all_by_blacklist_id id
    end

    return nil if terms.blank?
    terms.each do |term|
      regex_string += "#{Regexp.escape(term.term)}|"
    end

    if negated
      return { '$not' => /#{regex_string.chop}/ }
    else
      return /#{regex_string.chop}/
    end
  end
end
