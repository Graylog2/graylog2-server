'use strict';

var React = require('react/addons');
var QueryInput = require('./QueryInput');

var queryInputContainer = document.getElementById('react-query');
if (queryInputContainer) {
  var query = queryInputContainer.getAttribute("data-query");
  React.render(<QueryInput query={query}/>, queryInputContainer);
}
