class PluginConfigurationController < ApplicationController

  filter_access_to :all

  def configure
    @config = PluginConfiguration.where(:typeclass => params[:typeclass]).first || PluginConfiguration.new
    @requested_fields = get_requested_fields(params[:plugin_type], params[:typeclass]) || Hash.new
  end

  def store
  	config = PluginConfiguration.where(:typeclass => params[:typeclass]).first || PluginConfiguration.new

  	config.typeclass = params[:typeclass]
  	config.configuration = params[:config]

  	if config.save
  	  flash[:notice] = "Plugin configuration updated."
  	else
  	  flash[:error] = "Could not update plugin configuration!"
  	end

  	redirect_to :action => :configure
  end

  private
  def get_requested_fields(plugin_type, typeclass)
  	case plugin_type
  		when "alarm_transport"
  			return AlarmCallback.where(:typeclass => typeclass).first.requested_config
  	end
  rescue
  	{}
  end

end