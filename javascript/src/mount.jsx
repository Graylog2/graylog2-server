import React from 'react';
import $ from 'jquery';
import AppFacade from 'routing/AppFacade';

$(document).ready(() => {
  // must come first because it registers a global (ahem) singleton.
  require('./stores/metrics/mount');
  require('./stores/nodes/mount');

  React.render(<AppFacade />, document.body);
});

