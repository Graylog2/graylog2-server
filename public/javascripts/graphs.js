$(document).ready(function() {

	// Sidebar graph. (histogram message counts)
	if (document.querySelector("#sidebar-graph") != null) {
		var sidebarGraph = new Rickshaw.Graph.Ajax({
			element: document.querySelector("#sidebar-graph"),
			width: $("#sidebar").width()-5,
			height: 100,
			renderer: 'bar',
			stroke: true,
			dataURL: '/a/messagecounts/histogram?timerange=' + 2*60*60, // last two hours
			onData: function(d) { return transformData(d) },
			onError: function(d) {
				error = "<span class='alert alert-error'><i class='icon-warning-sign'></i> Could not load graph</span>";
				$("#sidebar-graph").html(error);
			},
			onComplete: function(transport) {
				graph = transport.graph;
				
				new Rickshaw.Graph.Axis.Time({
				      graph: graph,
				      ticksTreatment: "glow"
				});
				
				new Rickshaw.Graph.Axis.Y({
				    graph: graph,
				    tickFormat: Rickshaw.Fixtures.Number.formatKMBT
				});

				graph.update();
			}
		});
	}

	function transformData(d) {
		Rickshaw.Series.zeroFill(d);
		return d;
	}

});