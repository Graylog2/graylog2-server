import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import AlarmCallbackHistoryStore from 'stores/alarmcallbacks/AlarmCallbackHistoryStore';
import AlarmCallbacksStore from 'stores/alarmcallbacks/AlarmCallbacksStore';

import AlarmCallbacksActions from 'actions/alarmcallbacks/AlarmCallbacksActions';

import { Spinner } from 'components/common';
import { AlarmCallbackHistory } from 'components/alarmcallbacks';

const AlarmCallbackHistoryOverview = React.createClass({
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
    if (this.state.histories && this.state.types) {
      if (this.state.histories.length > 0) {
        var histories = this.state.histories.map(this._formatHistory);
        return (
          <Row>
            <Col md={12}>
              {histories}
            </Col>
          </Row>
        );
      } else {
        return (
          <div><i>No history available.</i></div>
        );
      }
    } else {
      return <Spinner />;
    }
  }
});

export default AlarmCallbackHistoryOverview;
