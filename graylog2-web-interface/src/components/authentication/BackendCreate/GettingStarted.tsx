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
// @flow strict
import * as React from 'react';

import { Row, Col } from 'components/graylog';
import { EmptyEntity } from 'components/common';

import ServiceSelect from './ServiceSelect';

type Props = {
  title?: string,
};

const GettingStarted = ({ title }: Props) => (
  <Row className="content">
    <Col md={6} mdOffset={3}>
      <EmptyEntity title={title}>
        <p>
          Beside the built-in authentication mechanisms like its internal user database or LDAP/Active Directory,
          authentication services can also be extended by plugins to support other authentication mechanisms.
          Select an authentication service to setup a new one.
        </p>
        <ServiceSelect />
      </EmptyEntity>
    </Col>
  </Row>
);

GettingStarted.defaultProps = {
  title: undefined,
};

export default GettingStarted;
