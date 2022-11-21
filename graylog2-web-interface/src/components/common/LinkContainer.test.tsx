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
import { render, waitFor, screen } from 'wrappedTestingLibrary';
import { fireEvent } from '@testing-library/react';

import history from 'util/History';
import { Button } from 'components/bootstrap';

import { LinkContainer } from './router';

jest.mock('util/History');

describe('LinkContainer', () => {
  const hasHref = (element: HTMLElement | HTMLAnchorElement): element is HTMLAnchorElement => {
    return 'href' in element;
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should use component passed in children', async () => {
    render(
      <LinkContainer to="/">
        <Button bsStyle="info">All Alerts</Button>
      </LinkContainer>,
    );

    fireEvent.click(await screen.findByText('All Alerts'));

    expect(history.push).toHaveBeenCalledWith('/');
  });

  it('should call onClick', async () => {
    const onClick = jest.fn();

    render(
      <LinkContainer to="/" onClick={onClick}>
        <Button bsStyle="info">All Alerts</Button>
      </LinkContainer>,
    );

    fireEvent.click(await screen.findByText('All Alerts'));

    expect(onClick).toHaveBeenCalled();
  });

  it('should call onClick of children', async () => {
    const onClick = jest.fn();
    const { findByText } = render((
      <LinkContainer to="/">
        <Button bsStyle="info" onClick={onClick}>All Alerts</Button>
      </LinkContainer>
    ));

    fireEvent.click(await findByText('All Alerts'));

    expect(onClick).toHaveBeenCalled();
  });

  it('should not call onClick of children for ctrl+click', async () => {
    const onClick = jest.fn();

    render(
      <LinkContainer to="/">
        <Button bsStyle="info" onClick={onClick}>All Alerts</Button>
      </LinkContainer>,
    );

    fireEvent.click(await screen.findByText('All Alerts'), { ctrlKey: true });

    expect(onClick).not.toHaveBeenCalled();
  });

  it('should add target URL as href to children', async () => {
    render(
      <LinkContainer to="/alerts">
        <Button bsStyle="info" onClick={jest.fn()}>Alerts</Button>
      </LinkContainer>,
    );

    const button = await screen.findByText('Alerts');

    expect(hasHref(button) ? button.href : null).toEqual('http://localhost/alerts');
  });

  it('should stop event in generated `onClick`', async () => {
    const onClick = jest.fn();
    const childOnClick = jest.fn();

    render(
      // eslint-disable-next-line jsx-a11y/click-events-have-key-events,jsx-a11y/no-static-element-interactions
      <div onClick={onClick}>
        <LinkContainer to="/">
          <Button bsStyle="info" onClick={childOnClick}>All Alerts</Button>
        </LinkContainer>
      </div>,
    );

    fireEvent.click(await screen.findByText('All Alerts'));

    await waitFor(() => expect(childOnClick).toHaveBeenCalled());

    expect(onClick).not.toHaveBeenCalled();
  });

  it('should not redirect onclick, when children is disabled', async () => {
    render(
      <LinkContainer to="/">
        <Button bsStyle="info" disabled>All Alerts</Button>
      </LinkContainer>,
    );

    fireEvent.click(await screen.findByText('All Alerts'));

    expect(history.push).not.toHaveBeenCalled();
  });

  it('should add target URL as href to children, when children is disabled', async () => {
    render(
      <LinkContainer to="/alerts">
        <Button bsStyle="info" onClick={jest.fn()} disabled>Alerts</Button>
      </LinkContainer>,
    );

    const button = await screen.findByText('Alerts');

    expect(hasHref(button) ? button.href : null).toBeNull();
  });
});
