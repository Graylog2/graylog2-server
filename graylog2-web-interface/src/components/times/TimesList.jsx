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
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import moment from 'moment';

import { Col, Row } from 'components/bootstrap';
import { Spinner, Timestamp, BrowserTime } from 'components/common';
import DateTime from 'logic/datetimes/DateTime';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import { SystemStore } from 'stores/system/SystemStore';

const TimesList = createReactClass({
  displayName: 'TimesList',
  mixins: [Reflux.connect(CurrentUserStore), Reflux.connect(SystemStore)],

  getInitialState() {
    return { time: moment() };
  },

  componentDidMount() {
    this.interval = setInterval(() => this.setState(this.getInitialState()), 1000);
  },

  componentWillUnmount() {
    clearInterval(this.interval);
  },

  render() {
    if (!this.state.system) {
      return <Spinner />;
    }

    const { time } = this.state;
    const timeFormat = 'withTz';
    const { currentUser } = this.state;
    const serverTimezone = this.state.system.timezone;

    return (
      <Row className="content">
        <Col md={12}>
          <h2>Time configuration</h2>

          <p className="description">
            Dealing with timezones can be confusing. Here you can see the timezone applied to different components of your system.
            You can check timezone settings of specific graylog-server nodes on their respective detail page.
          </p>

          <dl className="system-dl">
            <dt>User <em>{currentUser.username}</em>:</dt>
            <dd><Timestamp dateTime={time} format={timeFormat} /></dd>
            <dt>Your web browser:</dt>
            <dd><BrowserTime dateTime={time} format={timeFormat} /></dd>
            <dt>Graylog server:</dt>
            <dd><Timestamp dateTime={time} format={timeFormat} tz={serverTimezone} /></dd>
          </dl>
        </Col>
      </Row>
    );
  },
});

export default TimesList;
