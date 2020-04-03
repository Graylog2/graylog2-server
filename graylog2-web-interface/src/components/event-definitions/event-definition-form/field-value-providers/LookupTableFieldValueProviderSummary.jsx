import React from 'react';
import PropTypes from 'prop-types';

import { IfPermitted } from 'components/common';

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
        <IfPermitted permissions="lookuptables:read">
          <>
            <tr>
              <td>Value source</td>
              <td>Lookup Table</td>
            </tr>
            <tr>
              <td>Lookup Table</td>
              <td>{provider.table_name}</td>
            </tr>
            <tr>
              <td>Lookup Table Key Field</td>
              <td>{provider.key_field}</td>
            </tr>
          </>
        </IfPermitted>
      </CommonFieldValueProviderSummary>
    );
  }
}

export default LookupTableFieldValueProviderSummary;
