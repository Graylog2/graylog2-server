import React from 'react';
import Reflux from 'reflux';
import moment from 'moment';
import { Row, Col } from 'react-bootstrap';

import CombinedProvider from 'injection/CombinedProvider';
const { AlarmCallbackHistoryStore } = CombinedProvider.get('AlarmCallbackHistory');
const { AlertNotificationsStore } = CombinedProvider.get('AlertNotifications');

import { EntityList, Spinner } from 'components/common';
import { AlarmCallbackHistory } from 'components/alarmcallbacks';

const AlarmCallbackHistoryOverview = React.createClass({
  propTypes: {
    alertId: React.PropTypes.string.isRequired,
    streamId: React.PropTypes.string.isRequired,
  },

  mixins: [Reflux.connect(AlarmCallbackHistoryStore), Reflux.connect(AlertNotificationsStore)],

  _formatHistory(history) {
    return (
      <AlarmCallbackHistory key={history.id} alarmCallbackHistory={history} types={this.state.availableNotifications} />
    );
  },
  _isLoading() {
    return !(this.state.histories && this.state.availableNotifications);
  },
  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const histories = this.state.histories
      .sort((h1, h2) => {
        const h1Time = moment(h1.created_at);
        const h2Time = moment(h2.created_at);

        return (h1Time.isBefore(h2Time) ? -1 : h2Time.isBefore(h1Time) ? 1 : 0);
      })
      .map(this._formatHistory);
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
