Graylog2WebInterface::Application.routes.draw do
  scope Configuration.general_url_prefix do
    match 'logout' => 'sessions#destroy', :as => :logout
    match 'login' => 'sessions#new', :as => :login
    resource :session
    resources :messages do
      collection do
        post :showrange
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
    
      member do
        post :favorite
        post :unfavorite
        post :alertable
        post :setdescription
        post :showrange
        post :setalarmvalue
        post :togglealarmactive
        post :togglefavorited
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
    
    resource :analytics do
      get :index
      get :messagespread
    end
    
    match '/visuals/fetch/:id' => 'visuals#fetch',:as => "visuals"
    
    match '/' => 'messages#index', :as => "root"
    match '/:controller(/:action(/:id))'
  end
 
  match '/' => 'messages#index'
end

