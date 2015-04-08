'use strict';

var d3 = require('d3');

var D3Utils = {
    glColourPalette() {
        return d3.scale.ordinal().range(['#16ACE3', '#6DCFF6', '#F7941E', '#FBB040', '#BE1E2D',
                                         '#FF3B00', '#333333', '#9E1F63', '#8DC63F', '#BFD730',
                                         '#D7DF23', '#E3E5E5', '#F1F2F2']);
    },
    // Add a data element to the given D3 selection to show a bootstrap tooltip
    tooltipRenderlet(graph, selector, callback) {
        graph.on('renderlet', (chart) => {
            d3.select(chart.root()[0][0]).selectAll(selector)
                .attr('rel', 'tooltip')
                .attr('data-original-title', callback);
        });
    }
};

module.exports = D3Utils;