class Graph
  GRAPH_TYPE_TOTAL = 1

  def self.get_title graph_type
    case graph_type
      when self::GRAPH_TYPE_TOTAL:
          return "Total messages received"
    end

    throw "Invalid graph type supplied"
  end

  def self.enabled?
    $rrd_graphs_enabled
  end

  def self.rrd_binary
    $rrd_binary
  end

  def self.rrd_storage_path
    $rrd_storage_path
  end

  def self.graph_storage_path
    RAILS_ROOT + "/public/images/graphs"
  end

  def self.update_total
    return if !self.enabled?
    return process 'totalmessages', 12, self::GRAPH_TYPE_TOTAL
  end

  def self.process name, hours, graph_type
    title = self.get_title graph_type

    build_string  = "graph "
    build_string += "#{self.graph_storage_path}/#{name}.png"
    build_string += " --start #{Time.now.to_i-hours*60*60}"
    build_string += " DEF:mymessages=#{self.rrd_storage_path}/#{name}.rrd:messages:AVERAGE"
    build_string += " LINE1:mymessages#FD0C99"
    build_string += " --title \"#{title} - #{Time.now}\""
    build_string += " --width 550 --height 120"
    build_string += " --color BACK#FFFFFF --color CANVAS#FFFFFF --color SHADEA#FFFFFF --color SHADEB#FFFFFF"

    # Execute
    `#{self.rrd_binary} #{build_string}`
  end
end
