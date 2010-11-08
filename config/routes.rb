ActionController::Routing::Routes.draw do |map|
  map.logout '/logout', :controller => 'sessions', :action => 'destroy'
  map.login '/login', :controller => 'sessions', :action => 'new'

  map.resource :session
  
  map.resources :hosts do |hosts|
    hosts.resources :messages
  end
  
  map.resources :streams do |streams|
    streams.resources :messages
  end

  map.root :controller => "messages"
  
  map.connect ':controller/:action/:id'
  map.connect ':controller/:action/:id.:format'
end
