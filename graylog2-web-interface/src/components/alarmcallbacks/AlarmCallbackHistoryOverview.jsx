import React from 'react';
import { Row, Col } from 'react-bootstrap';

import StoreProvider from 'injection/StoreProvider';
const AlarmCallbackHistoryStore = StoreProvider.getStore('AlarmCallbackHistory');
// eslint-disable-next-line no-unused-vars
const AlarmCallbacksStore = StoreProvider.getStore('AlarmCallbacks');

import ActionsProvider from 'injection/ActionsProvider';
const AlarmCallbacksActions = ActionsProvider.getActions('AlarmCallbacks');

import { Spinner } from 'components/common';
import { AlarmCallbackHistory } from 'components/alarmcallbacks';

const AlarmCallbackHistoryOverview = React.createClass({
  propTypes: {
    alertId: React.PropTypes.string.isRequired,
    streamId: React.PropTypes.string.isRequired,
  },
  getInitialState() {
    return {};
  },
  componentDidMount() {
    this.loadData();
  },
  loadData() {
    AlarmCallbacksActions.available(this.props.streamId).then((types) => {
      this.setState({ types: types });
    });
    AlarmCallbackHistoryStore.listForAlert(this.props.streamId, this.props.alertId).done((histories) => {
      this.setState({ histories: histories });
    });
  },
  _formatHistory(history) {
    return (
      <li key={history._id}>
        <AlarmCallbackHistory alarmCallbackHistory={history} types={this.state.types}/>
      </li>
    );
  },
  _isLoading() {
    return !(this.state.histories && this.state.types);
  },
  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    if (this.state.histories.length === 0) {
      return (
        <div><i>No history available.</i></div>
      );
    }

    const histories = this.state.histories.map(this._formatHistory);
    return (
      <Row>
        <Col md={12}>
          <ul className="alarm-callbacks">
            {histories}
          </ul>
        </Col>
      </Row>
    );
  },
});

export default AlarmCallbackHistoryOverview;
