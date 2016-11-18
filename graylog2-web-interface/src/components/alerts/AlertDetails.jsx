import React from 'react';
import { Col, Row } from 'react-bootstrap';

import { AlertMessages, AlertTimeline } from 'components/alerts';
import { AlarmCallbackHistoryOverview } from 'components/alarmcallbacks';
import { Spinner } from 'components/common';

import CombinedProvider from 'injection/CombinedProvider';
const { AlarmCallbackHistoryActions } = CombinedProvider.get('AlarmCallbackHistory');
const { AlarmCallbacksActions } = CombinedProvider.get('AlarmCallbacks');
const { StreamsStore } = CombinedProvider.get('Streams');

const AlertDetails = React.createClass({
  propTypes: {
    alert: React.PropTypes.object.isRequired,
    condition: React.PropTypes.object,
    conditionType: React.PropTypes.object,
  },

  getInitialState() {
    return {
      stream: undefined,
    };
  },

  componentDidMount() {
    this._loadData();
  },

  _loadData() {
    StreamsStore.get(this.props.alert.stream_id, (stream) => {
      this.setState({ stream: stream });
    });
    AlarmCallbacksActions.available(this.props.alert.stream_id);
    AlarmCallbackHistoryActions.list(this.props.alert.stream_id, this.props.alert.id);
  },

  _isLoading() {
    return !this.state.stream;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const alert = this.props.alert;

    return (
      <div>
        <Row className="content">
          <Col md={12}>
            <h2>Alert timeline</h2>
            <p>
              This is a timeline of events occurred during the alert, you can see more information about some events
              below.
            </p>
            <AlertTimeline alert={alert} stream={this.state.stream} condition={this.props.condition}
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
            <h2>Messages evaluated</h2>
            <p>These are the messages evaluated during the alert.</p>
            <AlertMessages alert={alert} />
          </Col>
        </Row>
      </div>
    );
  },
});

export default AlertDetails;
