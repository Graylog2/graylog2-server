class AmqpSettingsController < ApplicationController

  filter_access_to :all

  def index
  	@has_settings_tabs = true
  	@amqp_configuration = AmqpConfiguration.new
  	@amqp_configurations = AmqpConfiguration.all
  end

  def create
  	@amqp_configuration = AmqpConfiguration.new(params[:amqp_configuration])
  	if @amqp_configuration.save
  		flash[:notice] = "AMQP configuration has been saved. System will start using it soon."
  	else
  		flash[:error] = "Could not add AMQP configuration. Make sure to fill out all fields."
  	end

  	redirect_to :action => :index
  end

  def destroy
    config = AmqpConfiguration.find(params[:id])

    if config.destroy
      flash[:notice] = "AMQP configuration has been delete. System will stop consuming it soon."
    else
      flash[:error] = "Could not delete AMQP configuration."
    end

    redirect_to :action => :index
  end
  
end