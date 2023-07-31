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
// eslint-disable-next-line no-restricted-imports
import type { DebouncedFunc } from 'lodash';
import { Form, Formik } from 'formik';

import { StoreMock as MockStore, asMock } from 'helpers/mocking';
import mockSearchClusterConfig from 'fixtures/searchClusterConfig';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import suppressConsole from 'helpers/suppressConsole';
import type { TimeRange } from 'views/logic/queries/Query';

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

describe('TimeRangeAddToQuickListButton', () => {
  const SUT = ({ timeRange }: { timeRange: TimeRange }) => (
    <Formik initialValues={{ nextTimeRange: timeRange }} onSubmit={() => {}}>
      <Form>
        <TimeRangeAddToQuickListButton />
      </Form>
    </Formik>
  );

  beforeEach(() => {
    asMock(useSearchConfiguration).mockReturnValue({
      config: mockSearchClusterConfig,
      refresh: jest.fn(),
    });

    asMock(debounce as DebouncedFunc<(...args: any) => any>).mockImplementation((fn) => fn);
  });

  const getOpenButton = () => screen.findByTitle('Save current time range as preset');

  it('shows popover on click', async () => {
    render(
      <SUT timeRange={{
        type: 'relative',
        from: 300,
      }} />);

    const buttonIcon = await getOpenButton();

    fireEvent.click(buttonIcon);

    await screen.findByText('Save as preset');
  });

  it('dont shows alert about duplication when there is no similar time range', async () => {
    render(
      <SUT timeRange={{
        type: 'relative',
        from: 50,
      }} />);

    const buttonIcon = await getOpenButton();

    fireEvent.click(buttonIcon);

    await expect(screen.queryByText('You already have similar time range in')).not.toBeInTheDocument();
  });

  it('shows alert about duplication when there is similar time range', async () => {
    render(
      <SUT timeRange={{
        type: 'relative',
        from: 300,
      }} />);

    const buttonIcon = await getOpenButton();

    fireEvent.click(buttonIcon);

    await screen.findByText('You already have similar time range in');
  });

  it('runs action to update config on submitting form', async () => {
    render(
      <SUT timeRange={{
        type: 'relative',
        from: 500,
      }} />);

    const buttonIcon = await getOpenButton();

    fireEvent.click(buttonIcon);

    const descriptionInput = await screen.findByLabelText('Time range description');
    descriptionInput.focus();

    await suppressConsole(async () => {
      await fireEvent.change(descriptionInput, { target: { value: 'My new time range' } });

      const submitButton = await screen.findByTitle('Save preset');
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
      <SUT timeRange={{
        type: 'relative',
        from: 500,
      }} />);

    const buttonIcon = await getOpenButton();

    fireEvent.click(buttonIcon);

    const descriptionInput = await screen.findByLabelText('Time range description');
    descriptionInput.focus();

    await suppressConsole(async () => {
      await fireEvent.change(descriptionInput, { target: { value: 'Some description' } });
      await fireEvent.change(descriptionInput, { target: { value: '' } });
    });

    const submitButton = await screen.findByTitle('Save preset');

    await expect(submitButton).toHaveAttribute('disabled');
  });
});
