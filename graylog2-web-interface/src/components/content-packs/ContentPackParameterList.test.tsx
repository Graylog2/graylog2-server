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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import ContentPack from 'logic/content-packs/ContentPack';
import Entity from 'logic/content-packs/Entity';
import ContentPackParameterList from 'components/content-packs/ContentPackParameterList';

import { SEARCH_DEBOUNCE_THRESHOLD } from '../common/SearchForm';

jest.mock('logic/generateId', () => jest.fn(() => 'dead-beef'));

describe('<ContentPackParameterList />', () => {
  beforeAll(() => {
    jest.useFakeTimers();
  });

  afterAll(() => {
    jest.useRealTimers();
  });

  it('should render with empty parameters with readOnly', async () => {
    const contentPack = ContentPack.builder().build();
    render(<ContentPackParameterList contentPack={contentPack} readOnly />);

    await screen.findByText('Parameters list');
  });

  it('should render with parameters with readOnly', async () => {
    const parameters = [{
      name: 'PARAM',
      title: 'A parameter title',
      description: 'A parameter descriptions',
      type: 'string',
      default_value: 'test',
    }];
    const contentPack = ContentPack.builder()
      .parameters(parameters)
      .build();
    render(<ContentPackParameterList contentPack={contentPack} readOnly />);

    await screen.findByText('Parameters list');
  });

  it('should render with empty parameters without readOnly', async () => {
    const contentPack = ContentPack.builder().build();
    render(<ContentPackParameterList contentPack={contentPack} />);

    await screen.findByText('Parameters list');
  });

  it('should render with parameters without readOnly', async () => {
    const parameters = [{
      name: 'PARAM',
      title: 'A parameter title',
      description: 'A parameter descriptions',
      type: 'string',
      default_value: 'test',
    }];
    const contentPack = ContentPack.builder()
      .parameters(parameters)
      .build();
    render(<ContentPackParameterList contentPack={contentPack} />);

    await screen.findByText('Parameters list');
  });

  it('should delete a parameter', async () => {
    const deleteFn = jest.fn();
    const parameters = [{
      name: 'PARAM',
      title: 'A parameter title',
      description: 'A parameter descriptions',
      type: 'string',
      default_value: 'test',
    }];
    const contentPack = ContentPack.builder()
      .parameters(parameters)
      .build();

    render(<ContentPackParameterList contentPack={contentPack}
                                     onDeleteParameter={deleteFn} />);

    (await screen.findByRole('button', { name: 'Delete Parameter' })).click();

    await waitFor(() => {
      expect(deleteFn).toHaveBeenCalledWith(expect.objectContaining({ name: 'PARAM' }));
    });
  });

  it('should not delete a used parameter', async () => {
    const entity = Entity.builder()
      .v(1)
      .type('input')
      .id('dead-beef')
      .data({ title: { '@type': 'parameter', '@value': 'PARAM' } })
      .build();
    const deleteFn = jest.fn();
    const parameters = [{
      name: 'PARAM',
      title: 'A parameter title',
      description: 'A parameter descriptions',
      type: 'string',
      default_value: 'test',
    }];
    const appliedParameter = {
      'dead-beef': [{ paramName: 'PARAM', configKey: 'title' }],
    };
    const contentPack = ContentPack.builder()
      .parameters(parameters)
      .entities([entity])
      .build();

    render(<ContentPackParameterList contentPack={contentPack}
                                     onDeleteParameter={deleteFn}
                                     appliedParameter={appliedParameter} />);

    const deleteButton = await screen.findByRole('button', { name: 'Still in use' });

    expect(deleteButton).toBeDisabled();

    deleteButton.click();

    expect(deleteFn).not.toHaveBeenCalled();
  });

  it('should filter parameters', async () => {
    const parameters = [{
      name: 'PARAM',
      title: 'A parameter title',
      description: 'A parameter descriptions',
      type: 'string',
      default_value: 'test',
    }, {
      name: 'BAD_PARAM',
      title: 'real bad',
      description: 'The dark ones own parameter',
      type: 'string',
      default_value: 'test',
    }];
    const contentPack = ContentPack.builder()
      .parameters(parameters)
      .build();
    render(<ContentPackParameterList contentPack={contentPack} />);

    await screen.findByText('PARAM');

    const input = await screen.findByPlaceholderText('Enter search query...');
    userEvent.type(input, 'Bad');

    jest.advanceTimersByTime(SEARCH_DEBOUNCE_THRESHOLD);

    await waitFor(() => {
      expect(screen.queryByText('PARAM')).not.toBeInTheDocument();
    });

    (await screen.findByRole('button', { name: 'Reset search' })).click();

    await screen.findByText('PARAM');
  });
});
