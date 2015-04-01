'use strict';

var React = require('react/addons');
var Throughput = require('./Throughput');

var throughput = document.getElementById('global-throughput');
if (throughput) {
    React.render(<Throughput />, throughput);
}
