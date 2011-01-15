authorization do
  role :admin do
    has_permission_on :messages, :to => [:index, :show, :getcompletemessage, :getsimilarmessages, :showrange, :deletebyquickfilter, :deletebystream, :getnewmessagecount]
    
    has_permission_on :streams, :to => [:index, :show, :showrange, :create, :destroy, :setdescription, :setalarmvalues, :togglefavorited, :togglealarmactive, :togglealarmforce, :rules, :analytics, :settings, :subscribe, :togglesubscription, :rename]
    has_permission_on :streamrules, :to => [:create, :destroy]

    has_permission_on :analytics, :to => [:index, :messagespread]

    has_permission_on :hosts, :to => [:index, :show, :destroy, :quickjump]
    has_permission_on :hostgroups, :to => [:new, :create, :hosts, :index, :settings, :show, :destroy, :rename]
    has_permission_on :hostgroup_hosts, :to => [:create, :destroy]
    
    has_permission_on :blacklists, :to => [:index, :show, :create, :destroy]
    has_permission_on :blacklistedterms, :to => [:create, :destroy]
    
    has_permission_on :settings, :to => [:index, :store]
    
    has_permission_on :users, :to => [:new, :index, :show, :create, :edit, :delete, :update]
    
    has_permission_on :sessions, :to => [:destroy]

    has_permission_on :dashboard, :to => [:index]
  end
  
  role :reader do
    has_permission_on :streams, :to => [:index, :show, :analytics, :favorite, :unfavorite, :alertable, :showrange] do
      if_attribute :users => contains { user }
    end
    has_permission_on :sessions, :to => [:destroy]
    has_permission_on :messages, :to => [:show]
  end
end

privileges do
end
