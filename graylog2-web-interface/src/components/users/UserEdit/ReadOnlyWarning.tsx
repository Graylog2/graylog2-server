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
import type { $PropertyType } from 'utility-types';

import { Alert, Col, Row } from 'components/bootstrap';
import type User from 'logic/users/User';

type Props = {
  fullName: $PropertyType<User, 'fullName'>,
};

const ReadOnlyWarning = ({ fullName }: Props) => (
  <Row className="content">
    <Col xs={12}>
      <Alert bsStyle="danger">
        The selected user {fullName} can&apos;t be edited.
      </Alert>
    </Col>
  </Row>
);

export default ReadOnlyWarning;
