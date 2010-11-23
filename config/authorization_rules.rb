authorization do
  role :admin do
    has_permission_on :messages, :to => [:index, :show, :getcompletemessage]
    
    has_permission_on :streams, :to => [:index, :show, :create, :delete, :alertable]
    has_permission_on :streamrules, :to => [:create, :delete]
    
    has_permission_on :hosts, :to => [:index]
    has_permission_on :hostgroups, :to => [:new, :create, :index, :show, :destroy]
    
    has_permission_on :blacklists, :to => [:index, :create, :delete]
    has_permission_on :blacklist_terms, :to => [:create, :delete]
    
    has_permission_on :settings, :to => [:index, :store]
    
    has_permission_on :users, :to => [:new, :index, :create, :edit, :delete, :update]
    
    has_permission_on :sessions, :to => [:destroy]
  end
  
  role :reader do
    has_permission_on :streams, :to => [:index, :show, :alertable] do
      if_attribute :users => contains { user }
    end
    has_permission_on :sessions, :to => [:destroy]
  end
end

privileges do
end