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
import { render, waitFor } from 'wrappedTestingLibrary';
import { fireEvent } from '@testing-library/react';

import { Button } from 'components/graylog';
import history from 'util/History';

import { LinkContainer } from './router';

jest.mock('util/History');

describe('LinkContainer', () => {
  it('should use component passed in children', async () => {
    const { findByText } = render((
      <LinkContainer to="/">
        <Button bsStyle="info">All Alerts</Button>
      </LinkContainer>
    ));

    const button = await findByText('All Alerts');

    fireEvent.click(button);

    expect(history.push).toHaveBeenCalledWith('/');
  });

  it('should call onClick', async () => {
    const onClick = jest.fn();
    const { findByText } = render((
      <LinkContainer to="/" onClick={onClick}>
        <Button bsStyle="info">All Alerts</Button>
      </LinkContainer>
    ));

    const button = await findByText('All Alerts');

    fireEvent.click(button);

    expect(onClick).toHaveBeenCalled();
  });

  it('should call onClick of children', async () => {
    const onClick = jest.fn();
    const { findByText } = render((
      <LinkContainer to="/">
        <Button bsStyle="info" onClick={onClick}>All Alerts</Button>
      </LinkContainer>
    ));

    const button = await findByText('All Alerts');

    fireEvent.click(button);

    expect(onClick).toHaveBeenCalled();
  });

  it('should not call onClick of children for ctrl+click', async () => {
    const onClick = jest.fn();
    const { findByText } = render((
      <LinkContainer to="/">
        <Button bsStyle="info" onClick={onClick}>All Alerts</Button>
      </LinkContainer>
    ));

    const button = await findByText('All Alerts');

    fireEvent.click(button, { ctrlKey: true });

    expect(onClick).not.toHaveBeenCalled();
  });

  it('should add target URL as href to children', async () => {
    const { findByText } = render((
      <LinkContainer to="/alerts">
        <Button bsStyle="info" onClick={jest.fn()}>Alerts</Button>
      </LinkContainer>
    ));

    const button = await findByText('Alerts');

    expect(button.href).toEqual('http://localhost/alerts');
  });

  it('should stop event in generated `onClick`', async () => {
    const onClick = jest.fn();
    const childOnClick = jest.fn();
    const { findByText } = render((
      // eslint-disable-next-line jsx-a11y/click-events-have-key-events,jsx-a11y/no-static-element-interactions
      <div onClick={onClick}>
        <LinkContainer to="/">
          <Button bsStyle="info" onClick={childOnClick}>All Alerts</Button>
        </LinkContainer>
      </div>
    ));

    const button = await findByText('All Alerts');

    fireEvent.click(button);

    await waitFor(() => expect(childOnClick).toHaveBeenCalled());

    expect(onClick).not.toHaveBeenCalled();
  });
});
