'use strict';

var QueryInput = require('./QueryInput');

var queryInputContainer = document.getElementById('universalsearch-query');
if (queryInputContainer) {
  var queryInput = new QueryInput(queryInputContainer);
  queryInput.display();
}
