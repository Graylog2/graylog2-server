class UsersController < ApplicationController

  # Allow to create a fist admin user.
  if User.find(:all).size == 0
    skip_before_filter :login_required, :only => [:new, :create]
    layout "login"
  end

  # render new.rhtml
  def new
    @user = User.new
  end
 
  def create
    logout_keeping_session!
    @user = User.new(params[:user])
    success = @user && @user.save
    if success && @user.errors.empty?
            # Protects against session fixation attacks, causes request forgery
      # protection if visitor resubmits an earlier form using back
      # button. Uncomment if you understand the tradeoffs.
      # reset session
      self.current_user = @user # !! now logged in
      redirect_back_or_default('/')
      flash[:notice] = "User has been created."
    else
      flash[:error]  = "Could not create user."
      render :action => 'new'
    end
  end
end
