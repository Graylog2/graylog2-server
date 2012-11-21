class StreamrulesController < ApplicationController
  filter_access_to :all

  before_filter :fetch_stream

  def create
    new_rule = @stream.streamrules.new params[:streamrule]
    if new_rule.save
      flash[:notice] = "Rule has been added."
    else
      msg = "Could not add rule."
      # lololol this sucks. we are not prepared for an output of *all* possible errors yet (layout wise)
      msg += " - Not a valid regular expression." if new_rule.errors[:value].include?("invalid regular expression")
      flash[:error] = msg
    end
    redirect_to rules_stream_path(@stream)
  end

  def update
    rule = @stream.streamrules.find(:first, :conditions => {:_id => BSON::ObjectId(params[:id])})
    rule.value = params[:streamrule][:value]
    if rule.save
      flash[:notice] = "Rule has been updated."
    else
      flash[:error] = "Could not update rule."
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
