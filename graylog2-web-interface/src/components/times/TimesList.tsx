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
import { useEffect, useState } from 'react';
import moment from 'moment';

import { Col, Row } from 'components/bootstrap';
import { Spinner, Timestamp, BrowserTime } from 'components/common';
import { SystemStore } from 'stores/system/SystemStore';
import useCurrentUser from 'hooks/useCurrentUser';
import { useStore } from 'stores/connect';

const TimesList = () => {
  const [time, setTime] = useState(moment());
  const currentUser = useCurrentUser();
  const { system } = useStore(SystemStore);

  useEffect(() => {
    const interval = setInterval(() => setTime(moment()), 1000);

    return () => clearInterval(interval);
  }, []);

  if (!system) {
    return <Spinner />;
  }

  const timeFormat = 'withTz';
  const serverTimezone = system.timezone;

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
};

export default TimesList;
