import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import CombinedProvider from 'injection/CombinedProvider';
import { sortByDate } from 'util/SortUtils';

import { EntityList, Spinner } from 'components/common';
import { AlarmCallbackHistory } from 'components/alarmcallbacks';

const { AlarmCallbackHistoryStore } = CombinedProvider.get('AlarmCallbackHistory');
const { AlertNotificationsStore } = CombinedProvider.get('AlertNotifications');

const AlarmCallbackHistoryOverview = createReactClass({
  displayName: 'AlarmCallbackHistoryOverview',

  propTypes: {
    alertId: PropTypes.string.isRequired,
    streamId: PropTypes.string.isRequired,
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
      .sort((h1, h2) => sortByDate(h1.created_at, h2.created_at))
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
