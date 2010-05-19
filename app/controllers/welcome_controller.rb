class WelcomeController < ApplicationController
  def index
    @hits = Message.count(:message => /www/)
  end
end
