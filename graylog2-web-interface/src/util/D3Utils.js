import d3 from 'd3';

const D3Utils = {
  glColourPalette() {
    return d3.scale.ordinal().range(['#16ACE3', '#FBB040', '#ED8EEF', '#7CE255', '#8DB3ED', '#EAF253',
      '#CED945', '#EF8DB6', '#D4A8ED', '#F495D9']);
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
