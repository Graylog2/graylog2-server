import React from 'react';
import PropTypes from 'prop-types';

import { Table, Button } from 'components/graylog';
import { Icon } from 'components/common';

import styles from './CommonFieldValueProviderSummary.css';

class CommonFieldValueProviderSummary extends React.Component {
  static propTypes = {
    fieldName: PropTypes.string.isRequired,
    config: PropTypes.object.isRequired,
    keys: PropTypes.array.isRequired,
    children: PropTypes.element.isRequired,
  };

  state = {
    displayDetails: false,
  };

  toggleDisplayDetails = () => {
    const { displayDetails } = this.state;
    this.setState({ displayDetails: !displayDetails });
  };

  render() {
    const { fieldName, config, keys, children } = this.props;
    const { displayDetails } = this.state;

    return (
      <dl className={styles.field}>
        <dt>{fieldName}</dt>
        <dd>
          <Button bsStyle="link" className="btn-text" bsSize="xsmall" onClick={this.toggleDisplayDetails}>
            <Icon name={`caret-${displayDetails ? 'down' : 'right'}`} />&nbsp;
            {displayDetails ? 'Less details' : 'More details'}
          </Button>
          {displayDetails && (
            <Table condensed hover className={styles.fixedTable}>
              <tbody>
                <tr>
                  <td>Is Key?</td>
                  <td>{keys.includes(fieldName) ? 'Yes' : 'No'}</td>
                </tr>
                <tr>
                  <td>Data Type</td>
                  <td>{config.data_type}</td>
                </tr>
                {children}
              </tbody>
            </Table>
          )}
        </dd>
      </dl>
    );
  }
}

export default CommonFieldValueProviderSummary;
