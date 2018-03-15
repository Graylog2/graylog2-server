import PropTypes from 'prop-types';
import React from 'react';

class NullCacheSummary extends React.Component {
  static propTypes = {
    cache: PropTypes.object.isRequired,
  };

  render() {
    return (<p>This cache has no configuration.</p>);
  }
}

export default NullCacheSummary;
