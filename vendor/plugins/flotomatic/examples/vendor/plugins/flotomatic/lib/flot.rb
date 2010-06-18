# Author::    Michael Cowden
# Copyright:: MigraineLiving.com
# License::   Distributed under the same terms as Ruby

=begin rdoc
== Flot 
This class acts as a ruby wrapper for the flot javascript object.  It is the class used in your model / controller to setup your data sets.  It is then the object used in the <tt><%= flot_graph %></tt> helper method.

It's primary purpose is to contain the data that is used in the flot graph.
=end
class Flot
  CANVAS_DEFAULT_HTML_OPTIONS = {:style => "height: 300px"}
  SERIES_OPTIONS = %w(lines points bars shadowSize colors)
  
  attr_accessor :data, :options, :placeholder, :html_options
  alias  :canvas :placeholder
  alias  :canvas= :placeholder=

=begin rdoc
The flot object can be initialized with either a block or a hash of options.  The canvas and placeholder are used interchangeably and is provided as an optional first argument for convenience.

Initialize with a hash:
 Flot.new('graph', :options => {:points => {:show => true}}, 
    :data => [
      {:label => 'Male', :data => [[1,0], [2,2]], [3,5]},
      {:label => 'Female', :data => [[1,1], [2,3]], [3,4]}
    ]) 

Initialize with a block:
  Flot.new('graph') do |f|
    f.bars
    f.grid :hoverable => true
    f.selection :mode => "xy"
    f.filter {|collection| collection.select {|j| j.entry_num > 0}}
    f.series_for("Stress", @journals, :x => :entry_num, :y => :stress_rating)
    f.series_for("Hours of Sleep", @journals, :x => :entry_num, :y => :hours_of_sleep)
    f.series_for("Restful Night?", @journals, :x => :entry_num, :y => lambda {|record| record.restful_night ? 5 : 0 }, :options => {:points => {:show => true}, :bars => {:show => false}})
  end

Initialize (and then set data & options later):
  f = Flot.new('graph')
  f.line
  f.series "Red Line", [[0,5], [1,5], [2,5]], :color => '#f00'
  
=end
  def initialize(canvas = nil, html_opts = {})
    # TODO: :tick_formatter => enum / hash or a mapping function, also :tick_formatter => {1 => "Mon", 2 => "Tue", ...} OR if an x or y is a string/sym consider auto conversion --> a TotalFlot? or total_for(@collection, x, y)
    # TODO: define callbacks - set and clear selection, binding plotselected, etc.
    # TODO: custom functions for ticks and such

    @collection_filter = nil
    returning(self) do |flot|
      flot.data       ||= []
      flot.options    ||= {}
      flot.html_options = html_opts.reverse_merge(CANVAS_DEFAULT_HTML_OPTIONS)
      flot.canvas       = canvas if canvas
      yield flot if block_given?
    end
  end

=begin rdoc
== Graph Styles
Convenience methods for defaulting the graph to line, point, or bar.  As well as for displaying the legend.  These are the defaults for each series added to the flot object, but they can be overridden using the options hash passed to the <tt>series</tt> and <tt>series_for</tt> methods.

Each takes a set of options, which default to :show => true.

Examples:
  flot.lines 
  flot.points 
  flot.bars :show => true, :bar_width => 5, :align => 'center'
  flot.legend :no_columns => 2

  flot.[lines|points|bars|legend](opts = {:show => true})
=end
  [:lines, :points, :bars, :legend].each do |meth|
    define_method(meth) do |*args|
      merge_options(meth, arguments_to_options(args))
    end
  end
  
  # Pass other methods through to the javascript flot object.
  #
  # For instance: <tt>flot.grid(:color => "#699")</tt>
  #
  def method_missing(meth, opts = {})
    merge_options meth, opts
  end

  # Setup a filter that will be used to limit all datasets in the graph.
  #
  # For instance, to limit the graph to positive amounts:
  #   flot.filter {|collection| collection.select {|record| record.amount > 0 }}
  #
  def filter(&block)
    @collection_filter = block
  end

  # Create a series based on a collection of objects.
  #
  # For example:
  #   # People by age
  #   people = People.all
  #   @flot.series_for "Age", people, :x => :id, :y => :age, :options => {:color => '#f00'}
  #
  def series_for(label, collection, opts)
    series label, map_collection(collection, opts[:x], opts[:y]), opts[:options] || {}
  end
  
  # Add a simple series to the graph:
  # 
  #   data = [[0,5], [1,5], [2,5]]
  #   @flot.series "Horizontal Line", data
  #   @flot.series "Red Line", data, :color => '#f00'  # or is it "'#f00'"
  #
  def series(label, d, opts = {})
    if opts.blank?
      @data << series_options.merge(:label => label, :data => d)
    else
      @data << opts.merge(:label => label, :data => d)
    end
  end
  
private
  def series_options
    @options.reject {|k,v| SERIES_OPTIONS.include?(k.to_s) == false}
  end

  def map_collection(collection, x, y)
    col = @collection_filter ? @collection_filter.call(collection) : collection
    col.map {|model| [get_coordinate(model, x), get_coordinate(model, y)]}
  end

  def merge_options(name, opts)
    @options.merge!  name => opts
  end
  
  def arguments_to_options(args)
    if args.blank? 
      {:show => true}
    elsif args.is_a? Array
      args.first
    else
      args
    end
  end
  
  def get_coordinate(model, method)
    method.is_a?(Proc) ? method.call(model) : model.send(method)
  end
end