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
import { render, screen } from 'wrappedTestingLibrary';

import Col from './Col';
import Row from './Row';

const COL_TESTID = 'graylog-col';

describe('<Col />', () => {
  it('renders children', () => {
    render(
      <Row>
        <Col md={6} data-testid={COL_TESTID}>
          hello
        </Col>
      </Row>,
    );

    expect(screen.getByTestId(COL_TESTID)).toHaveTextContent('hello');
  });

  it('passes className through to the rendered column', () => {
    render(
      <Row>
        <Col md={6} data-testid={COL_TESTID} className="my-col">
          cell
        </Col>
      </Row>,
    );

    expect(screen.getByTestId(COL_TESTID)).toHaveClass('my-col');
  });

  it('passes id through to the rendered column', () => {
    render(
      <Row>
        <Col md={6} data-testid={COL_TESTID} id="my-col-id">
          cell
        </Col>
      </Row>,
    );

    expect(screen.getByTestId(COL_TESTID)).toHaveAttribute('id', 'my-col-id');
  });

  it('renders componentClass as the underlying element', () => {
    render(
      <Row>
        <Col md={6} componentClass="section" data-testid={COL_TESTID}>
          section content
        </Col>
      </Row>,
    );

    expect(screen.getByTestId(COL_TESTID).tagName).toBe('SECTION');
  });

  it('renders without errors when used outside of a Row', () => {
    render(
      <Col md={6} data-testid={COL_TESTID}>
        standalone
      </Col>,
    );

    expect(screen.getByTestId(COL_TESTID)).toHaveTextContent('standalone');
  });
});
