// @flow strict
import * as React from 'react';

import HideOnCloud from './HideOnCloud';

/**
 * Higher order Component that will not render if environment is on cloud
 *
 * @param Component
 * @returns Component | null
 */
function withHideOnCloud<Config: {}>(
  Component: React.AbstractComponent<Config>,
): React.AbstractComponent<Config> {
  return (props) => <HideOnCloud><Component {...props} /></HideOnCloud>;
}

export default withHideOnCloud;
