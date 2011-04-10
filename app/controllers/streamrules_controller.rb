class StreamrulesController < ApplicationController
  filter_access_to :all

  before_filter :fetch_stream

  def create
    new_rule = @stream.streamrules.new params[:streamrule]
    if new_rule.save
      flash[:notice] = "Rule has been added."
    else
      flash[:error] = "Could not add rule."
    end
    redirect_to rules_stream_path(@stream)
  end

  def destroy
    rule = @stream.streamrules.find(:first, :conditions => {:_id => BSON::ObjectId(params[:id])})
    if rule.destroy
      flash[:notice] = "Rule has been removed from stream."
    else
      flash[:error] = "Could not remove rule from stream."
    end
    redirect_to rules_stream_path(@stream)
  end

  protected
  def fetch_stream
    if params[:stream_id]
      @stream = Stream.find_by_id(params[:stream_id])
    end
  end
end
