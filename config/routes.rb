Graylog2WebInterface::Application.routes.draw do
 #scope Configuration.general_url_prefix do
  match 'logout' => 'sessions#destroy', :as => :logout
  match 'login' => 'sessions#new', :as => :login
  resource :session
  resources :messages

  #resources :analytics

  resources :hosts do
    resources :messages
  end

  resources :streams do
    resources :messages
    
    member do
      post :favorite
      post :unfavorite
      post :alertable
    end
  end
  
  match '/' => 'messages#index', :as => "root"
  match '/:controller(/:action(/:id))'
 #end
 
 match '/' => 'messages#index'
end

