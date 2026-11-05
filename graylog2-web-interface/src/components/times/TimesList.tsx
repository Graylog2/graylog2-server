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
import * as React from 'react';
import { useEffect, useState } from 'react';
import moment from 'moment';
import { Trans, useTranslation } from 'react-i18next';

import { Col, Row } from 'components/bootstrap';
import { Spinner, Timestamp, BrowserTime } from 'components/common';
import useCurrentUser from 'hooks/useCurrentUser';
import useSystemInfo from 'hooks/useSystemStore';
import useProductName from 'brand-customization/useProductName';

const TimesList = () => {
  const { t } = useTranslation('common');
  const productName = useProductName();
  const [time, setTime] = useState(moment());
  const currentUser = useCurrentUser();
  const { data: system } = useSystemInfo();

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
        <h2>{t('times_list.title')}</h2>

        <p className="description">{t('times_list.description', { productName })}</p>

        <dl className="system-dl">
          <dt>
            <Trans
              t={t}
              i18nKey="times_list.user"
              values={{ username: currentUser.username }}
              components={{ bold: <em /> }}
            />
          </dt>
          <dd>
            <Timestamp dateTime={time} format={timeFormat} />
          </dd>
          <dt>{t('times_list.web_browser')}</dt>
          <dd>
            <BrowserTime dateTime={time} format={timeFormat} />
          </dd>
          <dt>{t('times_list.server', { productName })}</dt>
          <dd>
            <Timestamp dateTime={time} format={timeFormat} tz={serverTimezone} />
          </dd>
        </dl>
      </Col>
    </Row>
  );
};

export default TimesList;
