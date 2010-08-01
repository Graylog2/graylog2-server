class UsersController < ApplicationController

  if User.find(:all).size == 0
    skip_before_filter :login_required, :only => [:new, :create]
    layout "login"
  end

  def index
    @users = User.find :all
  end

  def new
    @user = User.new
  end
 
  def create
    @user = User.new(params[:user])
    success = @user && @user.save
    if success && @user.errors.empty?
      redirect_back_or_default('/')
      flash[:notice] = "User has been created."
    else
      flash[:error]  = "Could not create user."
      render :action => 'new'
    end
  end

  def delete
    # Don't let the user delete the last user.
    if User.count == 1
      flash[:error] = "You cannot delete all users."
      redirect_to :action => 'index'
      return
    end

    user = User.find params[:id]
    if user.destroy
      # Send back to login page if the user deleted himself.
      if current_user.id == params[:id]
        logout_killing_session!
        redirect_to '/'
        return
      end

      flash[:notice] = "User has been deleted."
    else
      flash[:error] = "Could not delete user."
    end

    redirect_to :action => 'index'
  end

end
