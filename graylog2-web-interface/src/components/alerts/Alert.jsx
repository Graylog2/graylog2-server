import React from 'react';
import { Button, Col, Label } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { EntityListItem, Timestamp } from 'components/common';

import Routes from 'routing/Routes';
import DateTime from 'logic/datetimes/DateTime';

const Alert = React.createClass({
  propTypes: {
    alert: React.PropTypes.object.isRequired,
    alertConditions: React.PropTypes.array.isRequired,
    streams: React.PropTypes.array.isRequired,
    conditionTypes: React.PropTypes.object.isRequired,
  },

  getInitialState() {
    return {
      showAlarmCallbackHistory: false,
    };
  },

  render() {
    const alert = this.props.alert;
    const condition = this.props.alertConditions.find(alertCondition => alertCondition.id === alert.condition_id);
    const stream = this.props.streams.find(s => s.id === alert.stream_id);
    const conditionType = condition ? this.props.conditionTypes[condition.type] : {};

    let alertTitle;
    if (condition) {
      alertTitle = <span>{condition.title} <small>on stream <em>{stream.title}</em></small></span>;
    } else {
      alertTitle = <span><em>Unknown alert</em></span>;
    }

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
          Triggered at {alertTime},&nbsp;
          {alert.resolved_at ? <span>resolved at <Timestamp dateTime={alert.resolved_at} format={DateTime.Formats.DATETIME} /></span> : 'still ongoing'}
        </span>
      );
    } else {
      alertTime = (
        <span>
          Triggered at {alertTime}
        </span>
      );
    }

    const actions = (
      <LinkContainer to={Routes.show_alert(alert.id)}>
        <Button bsStyle="info">Show details</Button>
      </LinkContainer>
    );

    const content = (
      <Col md={12}>
        <ul className="no-padding">
          <li><b>Reason:</b> {alert.description}</li>
          <li><b>Alert type:</b> {conditionType.name || 'Unknown type. This usually means that the alert condition was deleted since the alert was triggered.'}</li>
        </ul>
      </Col>
    );

    return (
      <EntityListItem key={`entry-list-${alert.id}`}
                      title={alertTitle}
                      titleSuffix={statusBadge}
                      description={alertTime}
                      actions={actions}
                      contentRow={content} />
    );
  },
});

export default Alert;
