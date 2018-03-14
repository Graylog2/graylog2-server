import PropTypes from 'prop-types';
import React from 'react';

class QuickValuesHistogramWidgetCreateConfiguration extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  getInitialConfiguration = () => {
    return {};
  };

  render() {
    return null;
  }
}

export default QuickValuesHistogramWidgetCreateConfiguration;
