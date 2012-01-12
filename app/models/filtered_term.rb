# Filters messages, used only by web.
class FilteredTerm
  include Mongoid::Document

  field :term, :type => String

  validates_presence_of :term

  # TODO validate regexp on save

  class << self
    extend ActiveSupport::Memoizable

    def apply(str)
      all_cached.each { |ft| str = ft.apply(str) }
      str
    end

    # to prevent hitting db for every message
    def all_cached
      all.to_a  # to_a is essential
    end
    memoize :all_cached

    def expire_cache
      flush_cache(:all_cached)
    end
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
