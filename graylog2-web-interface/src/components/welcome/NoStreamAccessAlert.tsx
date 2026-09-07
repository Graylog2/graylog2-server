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
import { useState } from 'react';
import styled from 'styled-components';

import { Alert, Row, Col } from 'components/bootstrap';
import Store from 'logic/local-storage/Store';

const NO_STREAM_ACCESS_DISMISSED_KEY = 'welcome-metrics-no-stream-access-dismissed';

const StyledAlert = styled(Alert)`
  margin: 0;
`;

const NoStreamAccessAlert = () => {
  const [noStreamAccessDismissed, setNoStreamAccessDismissed] = useState(!!Store.get(NO_STREAM_ACCESS_DISMISSED_KEY));

  const onDismiss = () => {
    Store.set(NO_STREAM_ACCESS_DISMISSED_KEY, true);
    setNoStreamAccessDismissed(true);
  };

  if (noStreamAccessDismissed) {
    return null;
  }

  return (
    <Row className="content">
      <Col xs={12}>
        <StyledAlert onDismiss={onDismiss}>
          Once you have access to a stream, your message metrics will show up here.
        </StyledAlert>
      </Col>
    </Row>
  );
};

export default NoStreamAccessAlert;
