import React from 'react';
import PropTypes from 'prop-types';

import CommonFieldValueProviderSummary from './CommonFieldValueProviderSummary';

class LookupTableFieldValueProviderSummary extends React.Component {
  static propTypes = {
    fieldName: PropTypes.string.isRequired,
    config: PropTypes.object.isRequired,
    keys: PropTypes.array.isRequired,
  };

  render() {
    const { config } = this.props;
    const provider = config.providers[0];

    return (
      <CommonFieldValueProviderSummary {...this.props}>
        <React.Fragment>
          <tr>
            <td>Value source</td>
            <td>Lookup Table</td>
          </tr>
          <tr>
            <td>Take Lookup Table Key from</td>
            <td>{provider.key_context === 'source' ? 'Source log message' : 'Generated Event'}</td>
          </tr>
          <tr>
            <td>Lookup Table Key Field</td>
            <td>{provider.key_field}</td>
          </tr>
          <tr>
            <td>Lookup Table</td>
            <td>{provider.table_name}</td>
          </tr>
        </React.Fragment>
      </CommonFieldValueProviderSummary>
    );
  }
}

export default LookupTableFieldValueProviderSummary;
