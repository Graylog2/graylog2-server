class OperationsController < ApplicationController

  def destroy
    oi = OperationInterface.new
    if !oi.kill(params[:id])
      flash[:error] = "Could not kill operation"
    else
      flash[:notice] = "Tried to kill operation"
    end

    redirect_to messages_path
  end

end
