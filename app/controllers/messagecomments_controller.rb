class MessagecommentsController < ApplicationController

  def index
    @has_settings_tabs = true
    @comments = Messagecomment.all
    @comment = Messagecomment.new
  end

  def show
    @has_settings_tabs = true
    @comment = Messagecomment.find(params[:id])
  end

  def create
    comment = Messagecomment.new(params[:messagecomment])
    comment.user_id = current_user.id

    if comment.save
      flash[:notice] = "Message comment has been created."
    else
      flash[:error] = "Could not create message comment!"
    end

    redirect_to :action => "index"
  end

  def update
    comment = Messagecomment.find(params[:id])
    
    if comment.update_attributes(params[:messagecomment])
      flash[:notice] = "Message comment has been updated."
    else
      flash[:error] = "Could not update message comment!"
    end

    redirect_to :action => "index"
  end

  def destroy
    comment = Messagecomment.find(params[:id])

    if comment.destroy
      flash[:notice] = "Message comment has been deleted."
    else
      flash[:error] = "Could not delete message comment!"
    end

    redirect_to :action => "index"
  end

end
