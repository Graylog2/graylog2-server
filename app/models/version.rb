require 'net/http'

class Version
  ENDPOINT = "http://versioncheck.graylog2.org/check"

  def self.outdated?
    self.get_current_version > ::GRAYLOG2_VERSION_TIMESTAMP
  end

private
  def self.get_current_version
    begin
      return Net::HTTP.get(URI.parse(ENDPOINT)).to_i
    rescue => e
      Rails.logger.warn("Could not get current Graylog2 version: #{e}")
      return ::GRAYLOG2_VERSION_TIMESTAMP - 1 # Not outdated.
    end
  end
end
