// @flow strict
import * as React from 'react';

import AppConfig from '../AppConfig';

/**
 * Higher order Component that will not render if environment is on cloud
 *
 * @param Component
 * @returns Component | null
 */
function withHideOnCloud<Config: {}>(
  Component: React.AbstractComponent<Config>,
): React.AbstractComponent<Config> {
  return (props) => {
    if (AppConfig.isCloud()) {
      return null;
    }

    return <Component {...props} />;
  };
}

export default withHideOnCloud;
