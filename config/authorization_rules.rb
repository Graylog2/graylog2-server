authorization do
  role :admin do
    has_permission_on :messages, :to => [:index, :show]
    
    has_permission_on :streams, :to => [:index, :show, :create, :delete]
    has_permission_on :streamrules, :to => [:create, :delete]
    
    has_permission_on :hosts, :to => [:index]
    
    has_permission_on :blacklists, :to => [:index, :create, :delete]
    has_permission_on :blacklist_terms, :to => [:create, :delete]
    
    has_permission_on :settings, :to => [:index, :store]
    
    has_permission_on :users, :to => [:new, :index, :create, :edit, :delete, :update]
    
    has_permission_on :sessions, :to => [:destroy]
  end
  
  role :reader do
    has_permission_on :streams, :to => [:index, :show] do
      if_attribute :users => contains { user }
    end
    has_permission_on :sessions, :to => [:destroy]
  end
end

privileges do
end