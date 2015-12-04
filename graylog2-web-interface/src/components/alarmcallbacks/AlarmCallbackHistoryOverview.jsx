import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import AlarmCallbackHistoryStore from 'stores/alarmcallbacks/AlarmCallbackHistoryStore';
import AlarmCallbacksStore from 'stores/alarmcallbacks/AlarmCallbacksStore';

import { Spinner } from 'components/common';
import { AlarmCallbackHistory } from 'components/alarmcallbacks';

const AlarmCallbackHistoryOverview = React.createClass({
  propTypes: {
    alertId: React.PropTypes.string.isRequired,
    streamId: React.PropTypes.string.isRequired,
  },
  mixins: [Reflux.connect(AlarmCallbacksStore)],
  getInitialState() {
    return {};
  },
  componentDidMount() {
    this.loadData();
  },
  loadData() {
    AlarmCallbackHistoryStore.listForAlert(this.props.streamId, this.props.alertId).done((result) => {
      this.setState({histories: result.histories});
    });
  },
  _formatHistory(history) {
    return <AlarmCallbackHistory key={history._id} alarmCallbackHistory={history} types={this.state.types}/>;
  },
  render() {
    if (!this.state.histories && !this.state.types) {
      return <Spinner />;
    }

    if (this.state.histories.length > 0) {
      return (
        <div><i>No history available.</i></div>
      );
    }

    const histories = this.state.histories.map(this._formatHistory);
    return (
      <Row>
        <Col md={12}>
          {histories}
        </Col>
      </Row>
    );
  },
});

export default AlarmCallbackHistoryOverview;
