class OperationInterface

  ALLOWED_OPERATIONS = %w(count find distinct)

  def initialize
    @ops = Mongoid.database["$cmd.sys.inprog"].find_one
  end

  def get_all
    res = Array.new
    @ops["inprog"].each do |op|
      next unless allowed_op?(op)

      res << {
        :opid => op["opid"],
        :secs_running => op["secs_running"],
        :type => extract_type(op),
        :query => op["query"]["query"]
      }
    end
  rescue => e
    Rails.logger.error "Could not get current operations: #{e.message + e.backtrace.join("\n")}"
    return Array.new
  end

  def kill(opid)
  end

  private

  def allowed_op?(op)
    return false unless op["ns"] =~ /^#{Mongoid.database.name}\.messages$/ # Only messages collection of graylog2 database.
    return false unless op["op"] == "query" # Only query operations.
    return false unless ALLOWED_OPERATIONS.include?(extract_type(op["query"])) # Only count, find or distinct queries.

    return true
  rescue => e
    Rails.logger.error "Could not check if operation is allowed: #{e.message + e.backtrace.join("\n")}"
    return false
  end

  def extract_type(op)
    op["query"].keys.first
  end

end
