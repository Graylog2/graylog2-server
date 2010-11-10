#ActionController::Routing::Routes.draw do |map|
#  map.logout '/logout', :controller => 'sessions', :action => 'destroy'
#  map.login '/login', :controller => 'sessions', :action => 'new'
#
#  map.resource :session
#
#  map.root :controller => "messages"
#  
#  map.connect ':controller/:action/:id'
#  map.connect ':controller/:action/:id.:format'
#end

Graylog2WebInterface::Application.routes.draw do
  match '/logout' => 'sessions#destroy', :as => :logout
  match '/login' => 'sessions#new', :as => :login
  resource :session
  match '/' => 'messages#index'
  match '/:controller(/:action(/:id))'
end

