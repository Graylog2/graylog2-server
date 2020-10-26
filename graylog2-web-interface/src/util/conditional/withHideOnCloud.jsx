// @flow strict
import * as React from 'react';

import isCloud from './isCloud';

function withHideOnCloud<Config: {}>(
  Component: React.AbstractComponent<Config>,
): React.AbstractComponent<Config> {
  return (props) => {
    if (isCloud) {
      return null;
    }

    return <Component {...props} />;
  };
}

export default withHideOnCloud;
