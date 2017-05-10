import d3 from 'd3';

const D3Utils = {
  glColourPalette() {
    return d3.scale.ordinal().range(
     [ "#3498db",
     "#1abc9c",
     "#8e44ad",
     "#f1c40f",
     "#e67e22",
     "#c0392b"]
     );
  },

  // Add a data element to the given D3 selection to show a bootstrap tooltip
  tooltipRenderlet(graph, selector, callback) {
    graph.on('renderlet', (chart) => {
      d3.select(chart.root()[0][0]).selectAll(selector)
        .attr('rel', 'tooltip')
        .attr('data-original-title', callback);
    });
  },
};

export default D3Utils;
