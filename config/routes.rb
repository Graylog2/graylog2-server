Graylog2WebInterface::Application.routes.draw do
  match 'logout' => 'sessions#destroy', :as => :logout
  match 'login' => 'sessions#new', :as => :login
  resource :session
  resources :settings, :dashboard
  
  resources :users do
    collection do
      post :createfirst
      get :first
    end
  end

  resources :messages do
    collection do
      post :showrange
      get :showrange
      post :getnewmessagecount
    end
    member do 
      post :show
      get :around
    end
  end

  resources :blacklists do
    resources :blacklistedterms, :as => "terms"
  end

  # this regular expression matches on invalid hostnames as well. this is not a problem in this case
  resources :hosts , :constraints => {:id => /[a-z0-9\-\.\-]+/} do
    resources :messages
  end
  
  resources :hostgroups do
    resources :messages
    member do
      get :hosts
      get :settings
      post :rename
    end
  end

  resources :hostgroup_hosts

  resources :facilities do
    member do
      post :changetitle
    end
  end

  resources :messagecomments do
  end

  resources :streams do
    resources :messages
    resources :streamrules
    resources :forwarders

    resources :dashboard
  
    member do
      get :deletebystream
      get :analytics
      post :favorite
      post :unfavorite
      post :alertable
      post :setdescription
      post :showrange
      get :showrange
      post :rules
      get :rules
      post :forward
      get :forward
      post :setalarmvalues
      post :togglealarmactive
      post :togglefavorited
      post :togglealarmforce
      post :togglesubscription
      post :rename
      get :settings
      post :subscribe
      post :unsubscribe
      post :categorize
    end
  end
  
  resources :alertedstreams do
    member do
      post :toggle
    end
  end
  
  resources :subscribedstreams do
    member do
      post :toggle
    end
  end
    
  resources :streamcategories do
    member do
      get :rename
    end
  end
  
  resource :analytics do
    get :index
    get :messagespread
  end

  resources :versioncheck do
    collection do
      get :perform
    end
  end

  resources :filteredterms
  
  resources :visuals, :constraints => {:id => /[a-z]+/} do
    member do
      post :fetch
    end
  end
  
  resources :health do
    collection do
      post :currentthroughput
    end
  end

  match '/visuals/fetch/:id' => 'visuals#fetch',:as => "visuals"
  
  root :to => 'messages#index'
end
