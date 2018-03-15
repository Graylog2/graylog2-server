import PropTypes from 'prop-types';
import React from 'react';
import { Col, Label } from 'react-bootstrap';
import { Link } from 'react-router';

import { EntityListItem, Timestamp } from 'components/common';

import Routes from 'routing/Routes';
import DateTime from 'logic/datetimes/DateTime';

import styles from './Alert.css';

class Alert extends React.Component {
  static propTypes = {
    alert: PropTypes.object.isRequired,
    alertConditions: PropTypes.array.isRequired,
    streams: PropTypes.array.isRequired,
    conditionTypes: PropTypes.object.isRequired,
  };

  state = {
    showAlarmCallbackHistory: false,
  };

  render() {
    const alert = this.props.alert;
    const condition = this.props.alertConditions.find(alertCondition => alertCondition.id === alert.condition_id);
    const stream = this.props.streams.find(s => s.id === alert.stream_id);
    const conditionType = condition ? this.props.conditionTypes[condition.type] : {};

    let alertTitle;
    if (condition) {
      alertTitle = (
        <span>
          <Link to={Routes.show_alert(alert.id)}>
            {condition.title || 'Untitled alert'}
          </Link>
          {' '}
          <small>on stream <em>{stream.title}</em></small>
        </span>
      );
    } else {
      alertTitle = (
        <span>
          <Link to={Routes.show_alert(alert.id)}>Unknown alert</Link>
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
  }
}

export default Alert;
