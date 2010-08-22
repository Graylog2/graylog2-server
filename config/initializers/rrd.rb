rrd_config = YAML::load(File.read(RAILS_ROOT + "/config/graphs.yml"))

begin

  # Is graphing enabled?
  $rrd_graphs_enabled = true
  if rrd_config["graphs"]["enable_graphs"].blank? or rrd_config["graphs"]["enable_graphs"] == false
    $rrd_graphs_enabled = false
  end

  if $rrd_graphs_enabled
    $rrd_binary = rrd_config["graphs"]["rrdtool_binary_path"]
    throw "Missing variable 'rrdtool_binary_path'" if $rrd_binary.blank?
    throw "'rrdtool_binary_path' does not exist!" if !File.exist? $rrd_binary
    throw "'rrdtool_binary_path' is not executable!" if !File.executable? $rrd_binary

    $rrd_storage_path = rrd_config["graphs"]["rrd_storage_path"]
    throw "Missing variable 'rrd_storage_path'" if $rrd_storage_path.blank?
    throw "'rrd_storage_path' does not exist!" if !File.directory? $rrd_storage_path

    # Create graph storage path if not existant.
    if !File.directory?(RAILS_ROOT + "/public/images/graphs")
      Dir.mkdir(RAILS_ROOT + "/public/images/graphs")
    end
  end

  
rescue => e
  puts "ERROR: RRD: #{e.to_str}"
  puts e.backtrace
  exit
end