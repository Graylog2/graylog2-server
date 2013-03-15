$(document).ready(function() {

	// Sidebar graph. (total message counts)
	var sidebarGraph = new Rickshaw.Graph.Ajax({
		element: document.querySelector("#sidebar-graph"),
		width: $("#sidebar").width()-20,
		height: 100,
		renderer: 'area',
		interpolation: 'step-after',
		stroke: true,
		dataURL: '/a/messagecounts/total?timerange=' + 2*60*60, // last two hours
		onData: function(d) { return transformData(d) },
		onComplete: function(transport) {
			graph = transport.graph;
			
			new Rickshaw.Graph.Axis.Time({
			      graph: graph
			});
			
			new Rickshaw.Graph.Axis.Y({
			    graph: graph,
			    tickFormat: Rickshaw.Fixtures.Number.formatKMBT
			});
			
			new Rickshaw.Graph.HoverDetail( {
			    graph: graph,
			    formatter: function(series, x, y) {
					var date = new Date(x * 1000).toUTCString();
					var content = "<strong>"  + parseInt(y) + " " + series.name + "</strong>" + '<br />' + date;
					return content;
				}
			});
			
			graph.update();
		}
	});

	function transformData(d) {
		Rickshaw.Series.zeroFill(d);
		return d;
	}

});