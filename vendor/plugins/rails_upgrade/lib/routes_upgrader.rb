# TODO: Fix formatting on member/collection methods

module Rails
  module Upgrading
    module FakeRouter
      module ActionController
        module Routing
          class Routes
            def self.setup
              @redrawer = Rails::Upgrading::RouteRedrawer.new
            end

            def self.redrawer
              @redrawer
            end

            def self.draw
              yield @redrawer
            end
          end
        end
      end
    end
    
    class RoutesUpgrader
      def generate_new_routes
        if has_routes_file?
          upgrade_routes
        else
          raise FileNotFoundError, "Can't find your routes file [config/routes.rb]!"
        end
      end
      
      def has_routes_file?
        File.exists?("config/routes.rb")
      end
      
      def routes_code
        File.read("config/routes.rb")
      end
      
      def upgrade_routes
        FakeRouter::ActionController::Routing::Routes.setup

        # Read and eval the file; our fake route mapper will capture
        # the calls to draw routes and generate new route code
        FakeRouter.module_eval(routes_code)

        # Give the route set to the code generator and get its output
        generator = RouteGenerator.new(FakeRouter::ActionController::Routing::Routes.redrawer.routes)
        generator.generate
      end
    end
    
    class RouteRedrawer
      attr_accessor :routes

      def self.stack
        @stack
      end
      
      def self.stack=(val)
        @stack = val
      end
      
      def initialize
        @routes = []

        # The old default route was actually two routes; we generate the new style
        # one only if we haven't generated it for the first old default route.
        @default_route_generated = false
        
        # Setup the stack for parents; used use proper indentation
        self.class.stack = [@routes]
      end
      
      def root(options)
        debug "mapping root"
        @routes << FakeRoute.new("/", options)
      end
      
      def connect(path, options={})
        debug "connecting #{path}"
        
        if (path == ":controller/:action/:id.:format" || path == ":controller/:action/:id")
          if !@default_route_generated
            current_parent << FakeRoute.new("/:controller(/:action(/:id))", {:default_route => true})
          
            @default_route_generated = true
          end
        else
          current_parent << FakeRoute.new(path, options)
        end
      end
      
      def resources(*args)
        if block_given?
          parent = FakeResourceRoute.new(args.shift)
          debug "mapping resources #{parent.name} with block"
          
          parent = stack(parent) do
            yield(self)
          end
          
          current_parent << parent
        else
          if args.last.is_a?(Hash)
            current_parent << FakeResourceRoute.new(args.shift, args.pop)
            debug "mapping resources #{current_parent.last.name} w/o block with args"
          else
            args.each do |a|              
              current_parent << FakeResourceRoute.new(a)
              debug "mapping resources #{current_parent.last.name}"
            end
          end
        end
      end
      
      def resource(*args)
        if block_given?
          parent = FakeSingletonResourceRoute.new(args.shift)
          debug "mapping resource #{parent.name} with block"
          
          parent = stack(parent) do
            yield(self)
          end
          
          current_parent << parent
        else
          if args.last.is_a?(Hash)
            current_parent << FakeSingletonResourceRoute.new(args.shift, args.pop)
            debug "mapping resources #{current_parent.last.name} w/o block with args"
          else
            args.each do |a|
              current_parent << FakeSingletonResourceRoute.new(a)
              debug "mapping resources #{current_parent.last.name}"
            end
          end
        end
      end
      
      def namespace(name)
        debug "mapping namespace #{name}"
        namespace = FakeNamespace.new(name)
        
        namespace = stack(namespace) do
          yield(self)
        end
        
        current_parent << namespace
      end
      
      def method_missing(m, *args)
        debug "named route: #{m}"
        current_parent << FakeRoute.new(args.shift, args.pop, m.to_s)
      end
      
      def self.indent
        ' ' * ((stack.length) * 2)
      end

    private
      def debug(txt)
        puts txt if ENV['DEBUG']
      end
      
      def stack(obj)
        self.class.stack << obj
        yield
        self.class.stack.pop
      end
    
      def current_parent
        self.class.stack.last
      end
    end
    
    class RouteObject
      def indent_lines(code_lines)
        if code_lines.length > 1
          code_lines.flatten.map {|l| "#{@indent}#{l.chomp}"}.join("\n") + "\n"
        else
          "#{@indent}#{code_lines.shift}"
        end
      end
      
      def opts_to_string(opts)
        opts.is_a?(Hash) ? opts.map {|k, v|
          ":#{k} => " + (v.is_a?(Hash) ? ('{ ' + opts_to_string(v) + ' }') : "#{value_to_string(v)}")
        }.join(", ") : opts.to_s
      end
      
      def value_to_string(value)
        case value
        when Regexp then value.inspect
        when String then "'" + value.to_s + "'"
        else value.to_s
        end
      end
    end
    
    class FakeNamespace < RouteObject
      attr_accessor :routes, :name
      
      def initialize(name)
        @routes = []
        @name = name
        @indent = RouteRedrawer.indent
      end
      
      def to_route_code
        lines = ["namespace :#{@name} do", @routes.map {|r| r.to_route_code}, "end"]
        
        indent_lines(lines)
      end
      
      def <<(val)
        @routes << val
      end
      
      def last
        @routes.last
      end
    end
    
    class FakeRoute < RouteObject
      attr_accessor :name, :path, :options
      
      def initialize(path, options, name = "")
        @path = path
        @options = options || {}
        @name = name
        @indent = RouteRedrawer.indent
      end
      
      def to_route_code
        if @options[:default_route]
          indent_lines ["match '#{@path}'"]
        else
          base = "match '%s' => '%s#%s'"
          extra_options = []
        
          if not name.empty?
            extra_options << ":as => :#{name}"
          end
          
          if @options[:requirements]
            @options[:constraints] = @options.delete(:requirements)
          end
          
          if @options[:conditions]
            @options[:via] = @options.delete(:conditions).delete(:method)
          end
        
          @options ||= {}
          base = (base % [@path, @options.delete(:controller), (@options.delete(:action) || "index")])
          opts = opts_to_string(@options)
          
          route_pieces = ([base] + extra_options + [opts])
          route_pieces.delete("")
          
          indent_lines [route_pieces.join(", ")]
        end
      end
    end
    
    class FakeResourceRoute < RouteObject
      attr_accessor :name, :children
      
      def initialize(name, options = {})
        @name = name
        @children = []
        @options = options
        @indent = RouteRedrawer.indent
      end
      
      def to_route_code
        
        if !@children.empty? || @options.has_key?(:collection) || @options.has_key?(:member)
          prefix = ["#{route_method} :#{@name} do"]
          lines = prefix + custom_methods + [@children.map {|r| r.to_route_code}.join("\n"), "end"]
        
          indent_lines(lines)
        else
          base = "#{route_method} :%s"
          indent_lines [base % [@name]]
        end
      end
      
      def custom_methods
        collection_code = generate_custom_methods_for(:collection)
        member_code = generate_custom_methods_for(:member)
        [collection_code, member_code]
      end
      
      def generate_custom_methods_for(group)
        return "" unless @options[group]
        
        method_code = []
        
        RouteRedrawer.stack << self
        @options[group].each do |k, v|
          method_code << "#{v} :#{k}"
        end
        RouteRedrawer.stack.pop
        
        indent_lines ["#{group} do", method_code, "end"].flatten
      end
      
      def route_method
        "resources"
      end
      
      def <<(val)
        @children << val
      end
      
      def last
        @children.last
      end
    end
    
    class FakeSingletonResourceRoute < FakeResourceRoute
      def route_method
        "resource"
      end
    end
    
    class RouteGenerator
      def initialize(routes)
        @routes = routes
        @new_code = ""
      end
      
      def generate
        @new_code = @routes.map do |r|
          r.to_route_code
        end.join("\n")
        
        "#{app_name.underscore.classify}::Application.routes.draw do\n#{@new_code}\nend\n"
      end
      
    private
      def app_name
        File.basename(Dir.pwd)
      end
    end
  end
end