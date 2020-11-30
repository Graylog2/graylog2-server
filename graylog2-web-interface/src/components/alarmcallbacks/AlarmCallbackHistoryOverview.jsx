/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import CombinedProvider from 'injection/CombinedProvider';
import { sortByDate } from 'util/SortUtils';
import { Row, Col } from 'components/graylog';
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
          <EntityList bsNoItemsStyle="info"
                      noItemsText="No notifications were triggered during the alert."
                      items={histories} />
        </Col>
      </Row>
    );
  },
});

export default AlarmCallbackHistoryOverview;
