module HostgroupsHelper
  def tabs
    @tabs = []
    if @hostgroup
      @tabs.push [ "Show", hostgroup_path(@hostgroup)]
      @tabs.push ["Hosts", hosts_hostgroup_path(@hostgroup)]
      @tabs.push ["Settings", settings_hostgroup_path(@hostgroup)]
    end
    @tabs
  end
end
