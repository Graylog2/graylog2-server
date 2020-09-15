import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import { upperFirst } from 'lodash';

import OperatingSystemIcon from './OperatingSystemIcon';

const CollectorIndicator = createReactClass({
  propTypes: {
    collector: PropTypes.string.isRequired,
    operatingSystem: PropTypes.string,
  },

  getDefaultProps() {
    return {
      operatingSystem: undefined,
    };
  },

  render() {
    const { collector, operatingSystem } = this.props;

    return (
      <span>
        <OperatingSystemIcon operatingSystem={operatingSystem} /> {collector}
        {operatingSystem && <span> on {upperFirst(operatingSystem)}</span>}
      </span>
    );
  },
});

export default CollectorIndicator;
