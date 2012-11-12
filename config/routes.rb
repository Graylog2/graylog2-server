Graylog2WebInterface::Application.routes.draw do
  match 'logout' => 'sessions#destroy', :as => :logout
  match 'login' => 'sessions#new', :as => :login
  resource :session
  resources :dashboard
  resources :operations

  resources :users do
    collection do
      post :createfirst
      get :first
    end
    member do
      post :key
    end
  end

  resources :messages do
    collection do
      post :showrange
      get :showrange
      post :deletebystream
      post :deletebyquickfilter
      get :realtime
      get :universalsearch
    end
    member do
      post :show
    end
  end

  resources :blacklists do
    resources :blacklistedterms, :as => "terms"
  end

  resources :hosts, :constraints => { :id => /.*/ } do
    resources :messages

    member do
      post :showrange
      get :showrange
    end

    collection do
      post :quickjump
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
      get :analytics
      get :alarms
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
      post :toggledisabled
      post :togglecallbackactive
      post :rename
      post :clone
      get :settings
      post :categorize
      post :addcolumn
      delete :removecolumn
      post :shortname
      post :related
    end
  end

  resources :alertedstreams do
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
    post :shell
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
      post :currentmqsize
    end
  end

  resources :additionalcolumns

  resources :settings do
    collection do
      post :store
    end
    
    member do
      delete :removecolumn
    end
  end

  resources :systemsettings do
    collection do
      post :allow_usage_stats
      post :toggle_alarmcallback_force
    end
  end

  resources :amqp_settings

  match '/visuals/fetch/:id' => 'visuals#fetch',:as => "visuals"

  # The contraints makes the typeclass parameter accept dots. Everything except slash is allowed.
  match '/plugin_configuration/configure/:plugin_type/:typeclass' => "plugin_configuration#configure", :constraints => { :typeclass => /[^\/]+/ }, :via => :get
  match '/plugin_configuration/configure/:plugin_type/:typeclass' => "plugin_configuration#store", :constraints => { :typeclass => /[^\/]+/ }, :via => :post

  root :to => 'messages#index'
end
