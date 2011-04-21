# Filters messages, used only by web.
class FilteredTerm
  include Mongoid::Document

  field :term, :type => String

  validates_presence_of :term

  # TODO validate regexp on save

  def self.apply(str)
    all.each { |ft| str = ft.apply(str) }
    str
  end

  def apply(str)
    return str if str.blank? || term.blank?

    begin
      str[/#{term}/] = "[FILTERED]"
    rescue => e
      Rails.logger.warn "Skipping filtered term: #{e}"
    end

    str
  end
end
