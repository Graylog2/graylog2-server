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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import type { SearchBarControl } from 'views/types';
import mockDispatch from 'views/test/mockDispatch';
import { createSearch } from 'fixtures/searches';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';

import {
  useInitialSearchValues,
  executeSearchSubmitHandler,
  pluggableValidationPayload,
  validatePluggableValues,
} from './pluggableSearchBarControlsHandler';

describe('pluggableSearchBarControlsHandler', () => {
  const pluggableSearchBarControl: SearchBarControl = {
    id: 'pluggable-search-bar-control',
    component: () => <div />,
    useInitialSearchValues: () => ({ pluggableControl: 'Initial Value' }),
    useInitialDashboardWidgetValues: () => ({ pluggableControl: 'Initial Value' }),
    onSearchSubmit: (_values, _dispatch, entity) => Promise.resolve(entity),
    onDashboardWidgetSubmit: (_values, _dispatch, entity) => Promise.resolve(entity),
    validationPayload: (values) => {
      // @ts-ignore
      const { pluggableControl } = values;

      return ({ customKey: pluggableControl });
    },
    onValidate: () => ({}),
    placement: 'right',
  };

  // eslint-disable-next-line no-console
  const original = console.error;
  const mockConsoleError = jest.fn();

  beforeEach(() => {
    // eslint-disable-next-line no-console
    console.error = mockConsoleError;
  });

  afterEach(() => {
    // eslint-disable-next-line no-console
    console.error = original;
  });

  const dispatch = mockDispatch();

  it('useInitialSearchValues should catch errors', async () => {
    PluginStore.register(new PluginManifest({}, {
      'views.components.searchBar': [
        () => ({ ...pluggableSearchBarControl, useInitialSearchValues: () => { throw Error('something went wrong!'); } }),
      ],
    }));

    const ExampleComponent = () => {
      const initialValuesFromPlugin = useInitialSearchValues();

      return <div>Plugin initial values: {JSON.stringify(initialValuesFromPlugin)}</div>;
    };

    render(<ExampleComponent />);

    await waitFor(() => expect(mockConsoleError).toHaveBeenCalledWith(
      'An error occurred when collecting initial search bar form values from a plugin: Error: something went wrong!',
    ));

    await screen.findByText('Plugin initial values: {}');
  });

  it('executeSearchSubmitHandler should catch errors', async () => {
    const result = await executeSearchSubmitHandler(
      dispatch,
      {},
      [() => ({
        ...pluggableSearchBarControl,
        onSearchSubmit: () => Promise.reject(new Error('something went wrong!')),
      })],
    );

    await waitFor(() => expect(mockConsoleError).toHaveBeenCalledWith(
      'An error occurred when executing a submit handler from a plugin: Error: something went wrong!',
    ));

    expect(result).toStrictEqual(undefined);
  });

  const handlerContext = {
    view: createSearch(),
    executionState: SearchExecutionState.empty(),
  };

  it('pluggableValidationPayload should catch errors', async () => {
    const result = pluggableValidationPayload(
      {},
      handlerContext,
      [() => ({
        ...pluggableSearchBarControl,
        validationPayload: () => { throw Error('something went wrong!'); },
      })],
    );

    await waitFor(() => expect(mockConsoleError).toHaveBeenCalledWith(
      'An error occurred when preparing search bar validation for a plugin: Error: something went wrong!',
    ));

    expect(result).toStrictEqual({});
  });

  it('validatePluggableValues should catch errors', async () => {
    const result = validatePluggableValues(
      {},
      handlerContext,
      [() => ({ ...pluggableSearchBarControl, onValidate: () => { throw Error('something went wrong!'); } })],
    );

    await waitFor(() => expect(mockConsoleError).toHaveBeenCalledWith(
      'An error occurred when validating search bar values from a plugin: Error: something went wrong!',
    ));

    expect(result).toStrictEqual({});
  });
});
