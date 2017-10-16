import PropTypes from 'prop-types';
import React from 'react';
import { Input } from 'components/bootstrap';

const NoopRotationStrategyConfiguration = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
    jsonSchema: PropTypes.object.isRequired,
    updateConfig: PropTypes.func.isRequired,
  },

  getInitialState() {
    return {
    };
  },

  render() {
  },
});

export default NoopRotationStrategyConfiguration;
