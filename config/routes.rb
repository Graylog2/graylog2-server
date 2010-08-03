ActionController::Routing::Routes.draw do |map|
  map.logout '/logout', :controller => 'sessions', :action => 'destroy'
  map.login '/login', :controller => 'sessions', :action => 'new'

  map.resource :session

  map.root :controller => "messages"

  map.connect ':controller/:action/:id'
  map.connect ':controller/:action/:id.:format'
end
