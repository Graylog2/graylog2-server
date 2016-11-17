import React from 'react';
import { Col, Row } from 'react-bootstrap';

import { AlarmCallbackHistoryOverview } from 'components/alarmcallbacks';

import CombinedProvider from 'injection/CombinedProvider';
const { AlarmCallbackHistoryActions } = CombinedProvider.get('AlarmCallbackHistory');
const { AlarmCallbacksActions } = CombinedProvider.get('AlarmCallbacks');

const AlertDetails = React.createClass({
  propTypes: {
    alert: React.PropTypes.object.isRequired,
  },

  componentDidMount() {
    this._loadData();
  },

  _loadData() {
    AlarmCallbacksActions.available(this.props.alert.stream_id);
    AlarmCallbackHistoryActions.list(this.props.alert.stream_id, this.props.alert.id);
  },

  render() {
    const alert = this.props.alert;

    return (
      <Row className="content">
        <Col md={12}>
          <h2>Triggered notifications</h2>
          <p>
            These are the notifications triggered during the alert, including the configuration they had at the time.
          </p>
          <AlarmCallbackHistoryOverview alertId={alert.id} streamId={alert.stream_id} />
        </Col>
      </Row>
    );
  },
});

export default AlertDetails;
