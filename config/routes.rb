Graylog2WebInterface::Application.routes.draw do
  match 'logout' => 'sessions#destroy', :as => :logout
  match 'login' => 'sessions#new', :as => :login
  resource :session
  resources :settings, :dashboard

  resources :users do
    collection do
      get :first
    end
  end

  resources :messages do
    collection do
      post :showrange
      get :showrange
    end
    member do 
      get :around
    end
  end

  resources :blacklists do
    resources :blacklistedterms, :as => "terms"
  end

  resources :hosts do
    resources :messages
  end

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
    
    resources :dashboard
  
    member do
      post :favorite
      post :unfavorite
      post :alertable
      post :setdescription
      post :showrange
      get :showrange
      post :rules
      get :rules
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
    
  resources :streamcategories
  
  resource :analytics do
    get :index
    get :messagespread
  end

  resources :filteredterms

  match '/visuals/fetch/:id' => 'visuals#fetch',:as => "visuals"
  
  match '/' => 'messages#index', :as => "root"
  match '/:controller(/:action(/:id))'
end
