import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import CombinedProvider from 'injection/CombinedProvider';
const { AlarmCallbackHistoryStore } = CombinedProvider.get('AlarmCallbackHistory');
const { AlarmCallbacksStore } = CombinedProvider.get('AlarmCallbacks');

import { EntityList, Spinner } from 'components/common';
import { AlarmCallbackHistory } from 'components/alarmcallbacks';

const AlarmCallbackHistoryOverview = React.createClass({
  propTypes: {
    alertId: React.PropTypes.string.isRequired,
    streamId: React.PropTypes.string.isRequired,
  },

  mixins: [Reflux.connect(AlarmCallbackHistoryStore), Reflux.connect(AlarmCallbacksStore)],

  _formatHistory(history) {
    return (
      <AlarmCallbackHistory key={history.id} alarmCallbackHistory={history} types={this.state.availableAlarmCallbacks}/>
    );
  },
  _isLoading() {
    return !(this.state.histories && this.state.availableAlarmCallbacks);
  },
  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const histories = this.state.histories.map(this._formatHistory);
    return (
      <Row>
        <Col md={12}>
          <EntityList bsNoItemsStyle="info" noItemsText="No notifications were triggered during the alert."
                      items={histories} />
        </Col>
      </Row>
    );
  },
});

export default AlarmCallbackHistoryOverview;
