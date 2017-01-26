import React from 'react';
import { Col, Label } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { EntityListItem, Timestamp } from 'components/common';

import Routes from 'routing/Routes';
import DateTime from 'logic/datetimes/DateTime';

import styles from './Alert.css';

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
      alertTitle = (
        <span>
          <LinkContainer to={Routes.show_alert(alert.id)}>
            <a>{condition.title || 'Untitled alert'}</a>
          </LinkContainer>
          {' '}
          <small>on stream <em>{stream.title}</em></small>
        </span>
      );
    } else {
      alertTitle = (
        <span>
          <LinkContainer to={Routes.show_alert(alert.id)}><a>Unknown alert</a></LinkContainer>
        </span>
      );
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
          {alert.resolved_at ?
            <span>resolved at <Timestamp dateTime={alert.resolved_at} format={DateTime.Formats.DATETIME} />.</span> :
            <span><strong>still ongoing</strong>.</span>}
        </span>
      );
    } else {
      alertTime = (
        <span>
          Triggered at {alertTime}
        </span>
      );
    }

    const content = (
      <Col md={12}>
        <dl className={`dl-horizontal ${styles.alertDescription}`}>
          <dt>Reason:</dt>
          <dd>{alert.description}</dd>
          <dt>Type:</dt>
          <dd>{conditionType.name || 'Unknown type. This usually means that the alert condition was deleted since the alert was triggered.'}</dd>
        </dl>
      </Col>
    );

    return (
      <EntityListItem key={`entry-list-${alert.id}`}
                      title={alertTitle}
                      titleSuffix={statusBadge}
                      description={alertTime}
                      contentRow={content} />
    );
  },
});

export default Alert;
