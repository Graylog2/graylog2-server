import React from 'react';
import ReactDOM from 'react-dom';
import AppFacade from 'routing/AppFacade';

// must come first because it registers a global (ahem) singleton.
//require('./stores/metrics/mount');
require('./stores/nodes/mount');

window.onload = () => {
  ReactDOM.render(<AppFacade />, document.getElementById('app-container'));
};
