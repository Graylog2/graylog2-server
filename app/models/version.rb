class Version
  ENDPOINT = "http://versioncheck.graylog2.org/check"

  def self.outdated?
    begin
      self.get_current_version > ::GRAYLOG2_VERSION_TIMESTAMP
    rescue
      false
    end
  end

  private

  def self.get_current_version
    begin
      return Net::HTTP.get(URI.parse(ENDPOINT)).to_i
    rescue => e
      Logger.warn("Could not get current Graylog2 version: #{e}")
      return ::GRAYLOG2_VERSION_TIMESTAMP+1
    end
  end
end
