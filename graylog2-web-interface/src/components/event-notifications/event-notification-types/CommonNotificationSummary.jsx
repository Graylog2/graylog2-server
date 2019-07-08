import React from 'react';
import PropTypes from 'prop-types';
import { Button } from 'react-bootstrap';

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
          <Button bsStyle="link" className="btn-text" bsSize="xsmall" onClick={this.toggleDisplayDetails}>
            <i className={`fa fa-caret-${displayDetails ? 'down' : 'right'}`} />&nbsp;
            {displayDetails ? 'Less details' : 'More details'}
          </Button>
          {displayDetails && (
            <dl>
              <dt>Description</dt>
              <dd>{notification.description || 'No description given'}</dd>
              {children}
            </dl>
          )}
        </dl>
      </React.Fragment>
    );
  }
}

export default CommonNotificationSummary;
