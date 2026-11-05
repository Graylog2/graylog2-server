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
import { useTranslation } from 'react-i18next';

import { Row, Col } from 'components/bootstrap';
import { EmptyEntity } from 'components/common';

import ServiceSelect from './ServiceSelect';

type Props = {
  title?: string;
};

const GettingStarted = ({ title = undefined }: Props) => {
  const { t } = useTranslation('authentication');

  return (
    <Row className="content">
      <Col md={6} mdOffset={3}>
        <EmptyEntity title={title}>
          <p>{t('getting_started.description')}</p>
          <ServiceSelect />
        </EmptyEntity>
      </Col>
    </Row>
  );
};

export default GettingStarted;
