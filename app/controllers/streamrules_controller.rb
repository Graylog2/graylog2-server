class StreamrulesController < ApplicationController
  def create
    new_rule = Streamrule.new params[:streamrule]
    if new_rule.save
      flash[:notice] = "Rule has been added."
    else
      flash[:error] = "Could not add rule."
    end
    redirect_to :controller => "streams", :action => "show", :id => params[:streamrule][:stream_id]
  end

  def destroy
    rule = Streamrule.find params[:id]
    if rule.destroy
      flash[:notice] = "Rule has been removed from stream."
    else
      flash[:error] = "Could not remove rule from stream."
    end
    redirect_to :controller => "streams", :action => "show", :id => params[:stream_id]
  end
end
