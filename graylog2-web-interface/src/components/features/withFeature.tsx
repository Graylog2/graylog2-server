import React from 'react';
import PropTypes from 'prop-types';

import Feature from './Feature';

// High Order Component API
const withFeature = <Props extends {}>(
  featureName: string,
  Component: React.ComponentType<Props>,
): React.ComponentType<Props> => {
  return (props) => (
    <Feature name={featureName}>
      <Component {...props} />
    </Feature>
  );
};

withFeature.propTypes = {
  featureName: PropTypes.string,
  Component: PropTypes.node,
};

export default withFeature;
