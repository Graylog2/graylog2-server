class UsersController < ApplicationController
  filter_access_to :index
  filter_access_to :show
  filter_access_to :new
  filter_access_to :edit
  filter_access_to :create
  filter_access_to :update
  filter_access_to :delete

  skip_before_filter :login_required, :only => [:first, :createfirst]

  layout :choose_layout

  def index
    @users = User.find :all
  end

  def show
    @user = User.find(params[:id])
  end

  def new
    @user = User.new
  end

  def edit
    @user = User.find params[:id]
  end

  def create
    @user = User.new(params[:user])
    success = @user && @user.save
    if success && @user.errors.empty?
      redirect_to :action => 'index'
      flash[:notice] = "User has been created."
    else
      flash[:error]  = "Could not create user."
      render :action => 'new'
    end
  end

  def update
    params[:user].delete :password if params[:user][:password].blank?
    params[:user].delete :password_confirmation if params[:user][:password_confirmation].blank?

    @user = User.find(params[:id])
    
    @user.transports = transport_hash(params)

    if @user.update_attributes(params[:user]) then
      flash[:notice] = 'User has been updated'
      redirect_to users_path
    else
      flash[:error] = 'Could not update user'
      render :action => :edit
    end
  end

  def destroy
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
        redirect_to root_path
        return
      end

      flash[:notice] = "User has been deleted."
    else
      flash[:error] = "Could not delete user."
    end

    redirect_to :action => 'index'
  end

  def key
    @user = User.find(params[:id])
    key = @user.generate_api_key
    @user.save()
    flash[:notice] = "The new api key is: " + key
    redirect_to :action => :edit
  end

  def first
    # This action is allowed without login. Block after first user is created.
    if User.count > 0
      block_access
      return
    end

    @user = User.new
  end

  def createfirst
    # This action is allowed without login. Block after first user is created.
    if User.count > 0
      block_access
      return
    end

    @user = User.new(params[:user])
    success = @user && @user.save
    if success && @user.errors.empty?
      redirect_to messages_path
      flash[:notice] = "Your first user has been created. Welcome to Graylog2!"
    else
      render :action => 'first'
    end
  end

  private
  def block_access
    render :text => "not authorized", :status => 401
    return
  end

  def choose_layout
    action_name == "first" || action_name == "createfirst" ? "login" : "application"
  end

  def transport_hash(p)
    r = []
    
    if p.blank? or p[:transports].blank?
      return r
    end

    p[:transports].each do |k,v|
      r << { :typeclass => k, :value => v }
    end

    return r
  end

end
