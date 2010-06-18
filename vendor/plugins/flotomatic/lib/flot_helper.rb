# Author::    Michael Cowden
# Copyright:: MigraineLiving.com
# License::   Distributed under the same terms as Ruby

=begin rdoc
== Flot Helper Methods
The following example sets up the proper javascript / stylesheet includes,
then creates a div for selecting specific data sets to display (dynamically).

flot_canvas creates a plot canvas with an id of "graph" and flot_graph uses
the @flot (Flot object) to provide the data and other preferences in plotting
on the "graph" canvas.

Within the flot_graph block, flot_plot does the actual plotting of the graph,
while flot_tooltip provides a tooltip / hover mouse over for each datapoint on
the graph.

flot_overview just creates the div to hold the smaller the zoom in / out graph.

== Example
  <%= flot_includes %>

  <h2>Graph the following items</h2>
  <div class='flot_dataset_picker'>
  	<%= flot_selections %>
  </div>

  <h2>My Graph</h2>
  <%= flot_canvas("graph") %>

  <h2>Zoom In / Out</h2>
  <%= flot_overview("asdflkjasdf") %>

  <% flot_graph("graph", @flot) do %>
  	<%= flot_plot(:dynamic => true, :overview => true) %>
  	<%= flot_tooltip %>
  <% end %>
=end
module FlotHelper
  FLOT_EXTRA_JS = %w(jquery.colorhelpers.min.js jquery.flot.crosshair.min.js jquery.flot.image.min.js jquery.flot.min.js jquery.flot.navigate.js jquery.flot.navigate.min.js jquery.flot.selection.min.js jquery.flot.stack.min.js jquery.flot.threshold.min.js)

  # Includes the 'flotomatic' stylesheet, jquery, and flotomatic javascript files
  #
  def flot_includes(options = {:jquery => true, :no_conflict => false, :include_all => false})
    return <<-EOJS
      #{stylesheet_link_tag 'flotomatic'}
  	  <!--[if IE]> #{javascript_include_tag('flotomatic/excanvas.min.js')} </script><![endif]-->
      #{javascript_include_tag('flotomatic/jquery.min.js') if options[:jquery]}
      #{javascript_tag "jQuery.noConflict();" if options[:no_conflict]}
      #{javascript_include_tag('flotomatic/jquery.flot.min.js')}
      #{javascript_include_tag('flotomatic/jquery.flot.selection.min.js') if options[:include].eql?(:selection)}
      #{flot_extra_javascripts if options[:include_all]}
      #{javascript_include_tag('flotomatic/flotomatic')}
    EOJS
  end

  # Creates the canvas div:
  #
  #   flot_canvas("graph")    # creates a canvas div with an id of "graph"
  #   flot_canvas(@flot)      # creates a canvas div with an id of @flot.placeholder (canvas)
  #                           # (along with @flot's html_options).  div's class is 'flot_canvas' by default
  #
  def flot_canvas(arg, options = {})
    if arg.is_a? Flot
      content_tag :div, "", options.reverse_merge(arg.html_options.reverse_merge(:id => arg.placeholder, :class => 'flot_canvas'))
    else # arg is the placeholder
      content_tag :div, "", options.reverse_merge(:id => arg, :class => 'flot_canvas')
    end
  end

  # Creates a div to contain the selection checkboxes (to pick the datasets to be display dynamically)
  #
  #   flot_selections(:id => 'flot_choices', :class => 'selectiony')  # :id is 'flot_choices' by default
  #--
  # TODO: Should take html_options
  #++
  #
  def flot_selections(options = {})
    # choices = flot.data.map do |dataset|
    #   label = content_tag :label, dataset[:label], :for => dataset[:label]
    #   input = content_tag :input, label, :type => 'checkbox', :name => dataset[:label], :checked => 'checked'
    #   '<br/>' + input
    # end
    content_tag :div, '', options.merge(:id => "flot_choices")
  end

  # Creates a ready function that creates a new Flotomatic object (Object-oriented flot wrapper)
  # Takes a block which can contain Javascript code and/or calls to flotomatic helper methods
  #
  # Using Helpers
  #   <% flot_graph("graph", @flot) do %>
  #     // plot the graph
  #     <%= flot_plot(:dynamic => true, :overview => true) %>
  #   <% end %>
  #
  # Using your own Javascript
  #   <% flot_graph("graph", @flot) do %>
  #     // any javascript code you want
  #     // with access to the flotomatic variable
  #   <% end %>
  #
  def flot_graph(placeholder, flot, &block)
    graph = javascript_tag <<-EOJS
      jQuery(function() {
        var data        = #{flot.data.to_json};
        var options     = #{flot.options.to_json};
        var flotomatic  = new Flotomatic('#{placeholder}', data, options);

        // Custom Javascript provided in block to flot_graph
        #{capture(&block) if block_given?}
      });
    EOJS

    return graph unless block_given?
    safe_concat graph, block.binding
  end

  # Plot the actual graph (to be called within the flot_graph block)
  #
  # Options:
  #   :dynamic => true    # use this option if you are creating a dynamic plot with flot_selections
  #   :overivew => true   # use this option if you want to zoom in & out from a flot_overview
  #
  def flot_plot(options = {:dynamic => false, :overview => false})
    return <<-EOJS
      #{options[:dynamic] ? "flotomatic.graphDynamic();" : "flotomatic.graph();"}
      #{'flotomatic.graphOverview();' if options[:overview]}
    EOJS
  end

  # Create the small overview div for zooming in and out
  #
  def flot_overview(text = '', options = {})
    content_tag(:div, text, options.merge(:id => 'flot_overview', :class => 'flot_overview'))
  end

  # Register a tooltip for data points
  #
  #   <%= flot_tooltip %>
  #
  # By default this will create a default tooltip.  The flot data array has been extended to accept a 3rd element in the array for a
  # custom tooltip.  If you want a custom tool tip, pass in the tooltip string when creating your series.
  #
  #   f.series(name.titleize, [1,21,'Custom tool tip message')
  #
  # Or if you have a collection of data objects, use series for and tell it which attribute has the tooltip string.
  #
  #   f.series_for(name.titleize, records, :x => :time, :y => :value, :tooltip => :tooltip)
  #
  # Be sure to set the hoverable option to true for the series you want the tool tip for
  #
  #   f.series_for(name.titleize, records, :x => :time, :y => :value, :tooltip => :tooltip, :link_url => :link_url, :grid => {:hoverable => true})
  #
  # or globally if you want the tooltip on all data points in all series.
  #
  #   f.grid :hoverable => true
  # TODO: specs, different defaults based on time axis
  def flot_tooltip
    "flotomatic.createTooltip();"
  end

  # Register a link for data points
  #
  #   <%= flot_link %>
  #
  # This will create a link using the url you pass in to the 4th element of the flot data array.  The flot data arrray has been extended to accept a 4th element in the array for a
  # custom link.
  #
  #   f.series(name.titleize, [1,21,'Custom tool tip message', url_for(:controller => :people, :action=> :profile, :id => person_id))
  #
  # Or if you have a collection of data objects, use series for and tell it which attribute has the url.
  #
  #   f.series_for(name.titleize, records, :x => :time, :y => :value, :tooltip => :tooltip, :link_url l=> :link_url)
  #
  # Be sure to set the clickable option to true for the series you want the link for
  #
  #   f.series_for(name.titleize, records, :x => :time, :y => :value, :tooltip => :tooltip, :link_url => :link_url, :grid => {:clickable => true})
  #
  # or globally if you want the link on all data points in all series.
  #
  #   f.grid :clickable => true
  def flot_link
    "flotomatic.createLink()"
  end

  def flot_extra_javascripts
    javascript_include_tag(*FLOT_EXTRA_JS.map {|file| "flotomatic/#{file}"})
  end

  # ActionView::Helpers::TextHelper::concat has different arity in
  # Rails 2.2.0 and later; pick the right implementation based on the
  # current Rails version to avoid a warning.
  def safe_concat(s, binding)
    if Rails::VERSION::STRING >= "2.2.0"
      concat(s)
    else
      concat(s, binding)
    end
  end
end
