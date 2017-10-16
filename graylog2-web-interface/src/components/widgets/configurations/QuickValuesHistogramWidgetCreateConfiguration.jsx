import PropTypes from 'prop-types';
import React from 'react';

const QuickValuesHistogramWidgetCreateConfiguration = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  },

  getInitialConfiguration() {
    return {};
  },

  render() {
    return null;
  },
});

export default QuickValuesHistogramWidgetCreateConfiguration;
