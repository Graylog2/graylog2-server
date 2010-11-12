Graylog2WebInterface::Application.routes.draw do
  match '/logout' => 'sessions#destroy', :as => :logout
  match '/login' => 'sessions#new', :as => :login
  resource :session
  match '/' => 'messages#index'
  match '/:controller(/:action(/:id))'
end

