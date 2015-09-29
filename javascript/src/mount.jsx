import React from 'react';
import AppFacade from 'routing/AppFacade';

// must come first because it registers a global (ahem) singleton.
require('./stores/metrics/mount');
require('./stores/nodes/mount');

React.render(<AppFacade />, document.body);
