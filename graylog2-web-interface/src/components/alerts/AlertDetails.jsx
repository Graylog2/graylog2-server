import React from 'react';
import { Col, Row } from 'react-bootstrap';

import { AlertMessages, AlertTimeline } from 'components/alerts';
import { AlarmCallbackHistoryOverview } from 'components/alarmcallbacks';

import CombinedProvider from 'injection/CombinedProvider';
const { AlarmCallbackHistoryActions } = CombinedProvider.get('AlarmCallbackHistory');
const { AlertNotificationsActions } = CombinedProvider.get('AlertNotifications');

const AlertDetails = React.createClass({
  propTypes: {
    alert: React.PropTypes.object.isRequired,
    condition: React.PropTypes.object,
    conditionType: React.PropTypes.object,
    stream: React.PropTypes.object.isRequired,
  },

  componentDidMount() {
    this._loadData();
  },

  _loadData() {
    AlertNotificationsActions.available();
    AlarmCallbackHistoryActions.list(this.props.alert.stream_id, this.props.alert.id);
  },

  render() {
    const alert = this.props.alert;
    const stream = this.props.stream;

    return (
      <div>
        <Row className="content">
          <Col md={12}>
            <h2>Alert timeline</h2>
            <p>
              This is a timeline of events occurred during the alert, you can see more information about some events
              below.
            </p>
            <AlertTimeline alert={alert} stream={stream} condition={this.props.condition}
                           conditionType={this.props.conditionType} />
          </Col>
        </Row>

        <Row className="content">
          <Col md={12}>
            <h2>Triggered notifications</h2>
            <p>
              These are the notifications triggered during the alert, including the configuration they had at the time.
            </p>
            <AlarmCallbackHistoryOverview alertId={alert.id} streamId={alert.stream_id} />
          </Col>
        </Row>

        <Row className="content">
          <Col md={12}>
            <AlertMessages alert={alert} stream={stream} />
          </Col>
        </Row>
      </div>
    );
  },
});

export default AlertDetails;
