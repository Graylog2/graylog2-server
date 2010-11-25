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

  def get_random_color
    random_colors = [
      "FF0000", "FF8000", "FFFF00",
      "80FF00", "00FF00", "00FF80",
      "00FFFF", "0080FF", "0000FF",
      "8000FF", "FF00FF", "FF0080"
    ]
    
    random_colors[rand(random_colors.size)]
  end

  def calculate_messagespread(params)
    values = Array.new

    conditions = Hash.new
    conditions["message"] = /#{Regexp.escape(params[:term])}/
    #conditions["short_message"] = Blacklistedterm.get_all_as_condition_hash

    hosts = Host.all

    hosts.each do |host|
      conditions["host"] = host.host
      count = Message.count :conditions => conditions

      if count > 0
        value = Hash.new
        value["data"] = {
          "$color" => get_random_color,
          "$angularWidth" => count
        }
        value["id"] = Base64.encode64(host.host).chomp
        value["name"] = host.host

        values << value
      end
    end

    # Sort values.
    values = values.sort_by { |v| v["data"]["$angularWidth"] } 

    r = Hash.new
    r["children"] = Array.new
    r["children"] << { "children" => values }

    return r.to_json
  end
end
