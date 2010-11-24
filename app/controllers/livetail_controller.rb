class LivetailController < ApplicationController
  def index
    @hide_stats = true
    @content_class = 'livetail'
  end
end
