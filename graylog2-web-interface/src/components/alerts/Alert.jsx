import React from 'react';
import { Button, Col, Label } from 'react-bootstrap';

import { EntityListItem, Spinner, Timestamp } from 'components/common';
import DateTime from 'logic/datetimes/DateTime';

const Alert = React.createClass({
  propTypes: {
    alert: React.PropTypes.object.isRequired,
  },

  getInitialState() {
    return {
      showAlarmCallbackHistory: false,
    };
  },

  render() {
    const alert = this.props.alert;
    let statusBadge;
    if (!alert.is_interval || alert.resolved_at) {
      statusBadge = <Label bsStyle="success">Resolved</Label>;
    } else {
      statusBadge = <Label bsStyle="danger">Unresolved</Label>;
    }

    let alertTime = <Timestamp dateTime={alert.triggered_at} format={DateTime.Formats.DATETIME} />;
    if (alert.is_interval) {
      alertTime = (
        <span>
          {alertTime}&nbsp;&#8211;&nbsp;
          {alert.resolved_at ? <Timestamp dateTime={alert.resolved_at} format={DateTime.Formats.DATETIME} /> : null}
        </span>
      );
    }

    const actions = <Button bsStyle="info">Show details</Button>;

    const content = (
      <Col md={12}>
        <p><b>Reason:</b> {alert.description}</p>
      </Col>
    );

    return (
      <EntityListItem key={`entry-list-${alert.id}`}
                      title={alert.condition_id}
                      titleSuffix={statusBadge}
                      description={alertTime}
                      actions={actions}
                      contentRow={content} />
    );
  },
});

export default Alert;
