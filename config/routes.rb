Graylog2WebInterface::Application.routes.draw do
 scope Configuration.general_url_prefix do
  match '/logout' => 'sessions#destroy', :as => :logout
  match '/login' => 'sessions#new', :as => :login
  resource :session
  resource :messages

  resources :hosts do
    resources :messages
  end

  resources :streams do
    resources :messages
  end
  
  match '/' => 'messages#index', :as => "root"
  match '/:controller(/:action(/:id))'
 end
end

