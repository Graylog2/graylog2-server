import React from 'react';
import PropTypes from 'prop-types';

import CommonFieldValueProviderSummary from './CommonFieldValueProviderSummary';

class TemplateFieldValueProviderSummary extends React.Component {
  static propTypes = {
    fieldName: PropTypes.string.isRequired,
    config: PropTypes.object.isRequired,
    keys: PropTypes.array.isRequired,
  };

  render() {
    const { config } = this.props;

    return (
      <CommonFieldValueProviderSummary {...this.props}>
        <React.Fragment>
          <tr>
            <td>Value source</td>
            <td>Template</td>
          </tr>
          <tr>
            <td>Template</td>
            <td>{config.providers[0].template}</td>
          </tr>
        </React.Fragment>
      </CommonFieldValueProviderSummary>
    );
  }
}

export default TemplateFieldValueProviderSummary;
