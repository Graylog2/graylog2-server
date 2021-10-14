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

import Row from './Row';
import { Col } from './imports';

// eslint-disable-next-line import/prefer-default-export
export const ColExample = () => {
  return (
    <>
      <Row>
        <Col xs={12} md={8}>
          <code>{'<Col xs={12} md={8} />'}</code>
        </Col>
        <Col xs={6} md={4}>
          <code>{'<Col xs={6} md={4} />'}</code>
        </Col>
      </Row>

      <Row>
        <Col xs={6} md={4}>
          <code>{'<Col xs={6} md={4} />'}</code>
        </Col>
        <Col xs={6} md={4}>
          <code>{'<Col xs={6} md={4} />'}</code>
        </Col>
        <Col xsHidden md={4}>
          <code>{'<Col xsHidden md={4} />'}</code>
        </Col>
      </Row>

      <Row>
        <Col xs={6} xsOffset={6}>
          <code>{'<Col xs={6} xsOffset={6} />'}</code>
        </Col>
      </Row>

      <Row>
        <Col md={6} mdPush={6}>
          <code>{'<Col md={6} mdPush={6} />'}</code>
        </Col>
        <Col md={6} mdPull={6}>
          <code>{'<Col md={6} mdPull={6} />'}</code>
        </Col>
      </Row>
    </>
  );
};
