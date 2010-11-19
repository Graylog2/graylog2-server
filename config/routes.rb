Graylog2WebInterface::Application.routes.draw do
  match '/logout' => 'sessions#destroy', :as => :logout
  match '/login' => 'sessions#new', :as => :login
  resource :session

  resources :hosts do
    resources :messages
  end

  resources :streams do
    resources :messages
  end
  
  match '/' => 'messages#index'
  match '/:controller(/:action(/:id))'
end

