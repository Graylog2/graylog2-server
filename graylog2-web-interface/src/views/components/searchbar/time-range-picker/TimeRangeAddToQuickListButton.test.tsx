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
import { fireEvent, render, screen } from 'wrappedTestingLibrary';
import debounce from 'lodash/debounce';

import { StoreMock as MockStore, asMock } from 'helpers/mocking';
import mockSearchClusterConfig from 'fixtures/searchClusterConfig';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import suppressConsole from 'helpers/suppressConsole';

import TimeRangeAddToQuickListButton from './TimeRangeAddToQuickListButton';

jest.mock('hooks/useSearchConfiguration', () => jest.fn());
jest.mock('lodash/debounce', () => jest.fn());
jest.mock('logic/generateId', () => jest.fn(() => 'timerange-id'));
let mockUpdate;

jest.mock('stores/configurations/ConfigurationsStore', () => {
  mockUpdate = jest.fn().mockReturnValue(Promise.resolve());

  return ({
    ConfigurationsStore: MockStore(['getInitialState', () => ({
      configuration: {
        'org.graylog2.indexer.searches.SearchesClusterConfig': mockSearchClusterConfig,
      },
    })]),
    ConfigurationsActions: {
      list: jest.fn(() => Promise.resolve()),
      update: mockUpdate,
    },
  });
});

describe('TimeRangeDropdown', () => {
  beforeEach(() => {
    asMock(useSearchConfiguration).mockReturnValue({
      config: mockSearchClusterConfig,
      refresh: jest.fn(),
    });

    // @ts-ignore
    asMock(debounce).mockImplementation((fn) => fn);
  });

  const getOpenButton = () => screen.findByTitle('Add time range to quick access time range list');

  it('shows popover on click', async () => {
    render(
      <TimeRangeAddToQuickListButton timerange={{
        type: 'relative',
        from: 300,
      }}
                                     isTimerangeValid />);

    const buttonIcon = await getOpenButton();

    fireEvent.click(buttonIcon);

    await screen.findByText('Add to quick access list');
  });

  it('dont shows alert about duplication when there is no similar time range', async () => {
    render(
      <TimeRangeAddToQuickListButton timerange={{
        type: 'relative',
        from: 50,
      }}
                                     isTimerangeValid />);

    const buttonIcon = await getOpenButton();

    fireEvent.click(buttonIcon);

    await expect(screen.queryByText('You already have similar time range in')).not.toBeInTheDocument();
  });

  it('shows alert about duplication when there is similar time range', async () => {
    render(
      <TimeRangeAddToQuickListButton timerange={{
        type: 'relative',
        from: 300,
      }}
                                     isTimerangeValid />);

    const buttonIcon = await getOpenButton();

    fireEvent.click(buttonIcon);

    await screen.findByText('You already have similar time range in');
  });

  it('runs action to update config on submitting form', async () => {
    render(
      <TimeRangeAddToQuickListButton timerange={{
        type: 'relative',
        from: 500,
      }}
                                     isTimerangeValid />);

    const buttonIcon = await getOpenButton();

    fireEvent.click(buttonIcon);

    const descriptionInput = await screen.findByLabelText('Time range description');
    descriptionInput.focus();

    await suppressConsole(async () => {
      await fireEvent.change(descriptionInput, { target: { value: 'My new time range' } });

      const submitButton = await screen.findByTitle('Add time range');
      await fireEvent.click(submitButton);
    });

    await expect(mockUpdate)
      .toHaveBeenCalledWith('org.graylog2.indexer.searches.SearchesClusterConfig',
        {
          ...mockSearchClusterConfig,
          quick_access_timerange_presets: [
            ...mockSearchClusterConfig.quick_access_timerange_presets,
            {
              id: 'timerange-id',
              description: 'My new time range',
              timerange: {
                type: 'relative',
                from: 500,
              },
            },
          ],
        },
      );
  });

  it('not runs action to update config on submitting form when description is empty', async () => {
    render(
      <TimeRangeAddToQuickListButton timerange={{
        type: 'relative',
        from: 500,
      }}
                                     isTimerangeValid />);

    const buttonIcon = await getOpenButton();

    fireEvent.click(buttonIcon);

    const descriptionInput = await screen.findByLabelText('Time range description');
    descriptionInput.focus();

    await suppressConsole(async () => {
      await fireEvent.change(descriptionInput, { target: { value: 'Some description' } });
      await fireEvent.change(descriptionInput, { target: { value: '' } });
    });

    const submitButton = await screen.findByTitle('Add time range');

    await expect(submitButton).toHaveAttribute('disabled');
  });
});
