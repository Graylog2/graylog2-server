class VisualsController < ApplicationController
  def messagespread
    @load_jit = true
    @term = params[:term]
    if @term.blank?
      redirect_to :controller => "messages"
      return
    end
  end

  def fetch
    case params[:id]
      when "messagespread" then
        r = calculate_messagespread(params)
    end

    render :text => r
  end

  private

  def calculate_messagespread(params)
    values = Array.new

    conditions = Hash.new
    conditions["message"] = /#{Regexp.escape(params[:term])}/
    #conditions["short_message"] = Blacklistedterm.get_all_as_condition_hash

    hosts = Host.all

    highest = 0
    hosts.each do |host|
      conditions["host"] = host.host
      count = Message.count :conditions => conditions

      if count > 0
        value = Hash.new
        value["data"] = { "$angularWidth" => count }
        value["id"] = Base64.encode64(host.host).chomp
        value["name"] = host.host

        values << value
      end
      highest = count if count > highest
    end

    # Sort values.
    values = values.sort_by { |v| v["data"]["$angularWidth"] } 

    # Add weighted colors.
    colored_values = Array.new
    values.each do |value|
      red = (value["data"]["$angularWidth"].to_f/highest.to_f*255).to_i.floor.to_s(16)
      red = "0#{red}" if red.length == 1
      value["data"]["$color"] = "##{red}0010"
      colored_values << value
    end

    r = Hash.new
    r["data"] = Hash.new
    r["data"]["$color"] = "#fff"
    r["children"] = Array.new
    r["children"] << { "children" => colored_values }

    return r.to_json
  end
end
