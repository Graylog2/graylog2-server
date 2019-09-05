import React from 'react';
import PropTypes from 'prop-types';

import { Icon, Table, Button } from 'components/graylog';

import styles from './CommonNotificationSummary.css';

class CommonNotificationSummary extends React.Component {
  static propTypes = {
    type: PropTypes.string.isRequired,
    notification: PropTypes.object.isRequired,
    definitionNotification: PropTypes.object.isRequired,
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
    const { type, notification, definitionNotification, children } = this.props;
    const { displayDetails } = this.state;
    return (
      <React.Fragment>
        <h4>{notification.title || definitionNotification.notification_id}</h4>
        <dl>
          <dd>{type}</dd>
          <dd>
            <Button bsStyle="link" className="btn-text" bsSize="xsmall" onClick={this.toggleDisplayDetails}>
              <Icon name={`caret-${displayDetails ? 'down' : 'right'}`} />&nbsp;
              {displayDetails ? 'Less details' : 'More details'}
            </Button>
            {displayDetails && (
            <Table condensed hover className={styles.fixedTable}>
              <tbody>
                <tr>
                  <td>Description</td>
                  <td>{notification.description || 'No description given'}</td>
                </tr>
                {children}
              </tbody>
            </Table>
            )}
          </dd>
        </dl>
      </React.Fragment>
    );
  }
}

export default CommonNotificationSummary;
