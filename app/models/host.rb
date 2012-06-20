class Host
  include Mongoid::Document

  PER_PAGE = 30
  paginates_per(PER_PAGE)

  field :host, :type => String
  field :message_count, :type => Float  # FIXME float??? so we can have 3.14 messages from this host?

  validates_presence_of :host

  def self.all_hostnames
    r = []
    all.collect do |host|
      begin
        r << CGI::escapeHTML(host.host)
      rescue
        # There may be errors from faulty UTF8 characters in the hostname.
        # (True story: https://groups.google.com/d/topic/graylog2/g9g1bJ-kjgY/discussion)
        next
      end
    end

    return r.sort
  end

end
