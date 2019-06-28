import React from 'react';
import PropTypes from 'prop-types';
import { Table } from 'react-bootstrap';

class TemplateFieldValueProviderSummary extends React.Component {
  static propTypes = {
    fieldName: PropTypes.string.isRequired,
    config: PropTypes.object.isRequired,
    keys: PropTypes.array.isRequired,
  };

  render() {
    const { fieldName, config, keys } = this.props;

    return (
      <Table condensed hover>
        <tbody>
          <tr>
            <td>Is Key?</td>
            <td>{keys.includes(fieldName) ? `Yes, in position ${keys.indexOf(fieldName) + 1}` : 'No'}</td>
          </tr>
          <tr>
            <td>Value source</td>
            <td>Template</td>
          </tr>
          <tr>
            <td>Template</td>
            <td>{config.providers[0].template}</td>
          </tr>
          <tr>
            <td>Data Type</td>
            <td>{config.data_type}</td>
          </tr>
        </tbody>
      </Table>
    );
  }
}

export default TemplateFieldValueProviderSummary;
