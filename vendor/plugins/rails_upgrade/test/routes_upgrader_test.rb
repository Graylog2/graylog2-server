require 'test_helper'
require 'routes_upgrader'

# Stub out methods on upgrader class
module Rails
  module Upgrading
    class RoutesUpgrader
      attr_writer :routes_code
      
      def has_routes_file?
        true
      end
      
      def routes_code
        @routes_code
      end
    end
    
    class RouteGenerator
      def app_name
        "MyApplication"
      end
    end
  end
end

class RoutesUpgraderTest < ActiveSupport::TestCase
  def setup
    Rails::Upgrading::RouteRedrawer.stack = []
  end
  
  def test_generates_routes_file
    routes_code = "
      ActionController::Routing::Routes.draw do |map|
        map.connect '/home', :controller => 'home', :action => 'index'
        map.login '/login', :controller => 'sessions', :action => 'new'
        
        map.resources :hats
        map.resource :store
      end
    "
    
    new_routes_code = "MyApplication::Application.routes.draw do
  match '/home' => 'home#index'
  match '/login' => 'sessions#new', :as => :login
  resources :hats
  resource :store
end
"
    
    upgrader = Rails::Upgrading::RoutesUpgrader.new
    upgrader.routes_code = routes_code
    
    result = upgrader.generate_new_routes
    
    assert_equal new_routes_code, result
  end
  
  def test_generates_code_for_regular_route
    route = Rails::Upgrading::FakeRoute.new("/about", {:controller => 'static', :action => 'about'})
    assert_equal "match '/about' => 'static#about'", route.to_route_code
  end
  
  def test_generates_code_for_named_route
    route = Rails::Upgrading::FakeRoute.new("/about", {:controller => 'static', :action => 'about'}, "about")
    assert_equal "match '/about' => 'static#about', :as => :about", route.to_route_code
  end
  
  def test_generates_code_for_namespace
    ns = Rails::Upgrading::FakeNamespace.new("static")
    # Add a route to the namespace
    ns << Rails::Upgrading::FakeRoute.new("/about", {:controller => 'static', :action => 'about'})
    
    assert_equal "namespace :static do\nmatch '/about' => 'static#about'\nend\n", ns.to_route_code
  end
  
  def test_generates_code_for_resources
    route = Rails::Upgrading::FakeResourceRoute.new("hats")
    assert_equal "resources :hats", route.to_route_code
  end
  
  def test_generates_code_for_resources
    route = Rails::Upgrading::FakeSingletonResourceRoute.new("hat")
    assert_equal "resource :hat", route.to_route_code
  end
  
  def test_generates_code_for_resources_with_special_methods
    route = Rails::Upgrading::FakeResourceRoute.new("hats", {:member => {:wear => :get}, :collection => {:toss => :post}})
    assert_equal "resources :hats do\ncollection do\npost :toss\nend\nmember do\nget :wear\nend\n\nend\n", route.to_route_code
  end
  
  def test_generates_code_for_route_with_extra_params
    route = Rails::Upgrading::FakeRoute.new("/about", {:controller => 'static', :action => 'about', :something => 'extra'})
    assert_equal "match '/about' => 'static#about', :something => 'extra'", route.to_route_code
  end
  
  def test_generates_code_for_route_with_requirements
    route = Rails::Upgrading::FakeRoute.new("/foo", {:controller => 'foo', :action => 'bar', :requirements => {:digit => /%d/}})
    assert_equal "match '/foo' => 'foo#bar', :constraints => { :digit => /%d/ }", route.to_route_code
  end
  
  def test_generates_code_for_root
    routes_code = "
      ActionController::Routing::Routes.draw do |map|
        map.root :controller => 'home', :action => 'index'
      end
    "

    new_routes_code = "MyApplication::Application.routes.draw do
  match '/' => 'home#index'
end
"

    upgrader = Rails::Upgrading::RoutesUpgrader.new
    upgrader.routes_code = routes_code

    result = upgrader.generate_new_routes

    assert_equal new_routes_code, result
  end
  
  def test_generates_code_for_default_route
    routes_code = "
      ActionController::Routing::Routes.draw do |map|
        map.connect ':controller/:action/:id.:format'
        map.connect ':controller/:action/:id'
      end
    "

    new_routes_code = "MyApplication::Application.routes.draw do
  match '/:controller(/:action(/:id))'
end
"

    upgrader = Rails::Upgrading::RoutesUpgrader.new
    upgrader.routes_code = routes_code

    result = upgrader.generate_new_routes

    assert_equal new_routes_code, result
  end
end