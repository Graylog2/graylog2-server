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
import userEvent from '@testing-library/user-event';

import Widget from 'views/logic/widgets/Widget';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import useViewsPlugin from 'views/test/testViewsPlugin';
import StreamsContext from 'contexts/StreamsContext';

import StandaloneWidgetEditor from './StandaloneWidgetEditor';

jest.mock('views/logic/fieldtypes/useFieldTypes', () => jest.fn(() => ({ data: [] })));
jest.mock('hooks/useHotkey', () => jest.fn());
jest.mock('views/logic/slices/createSearch', () => jest.fn(async (search) => search));
jest.mock('views/logic/slices/viewSlice', () => ({
  ...jest.requireActual('views/logic/slices/viewSlice'),
  executeActiveQuery: jest.fn(() => async () => {}),
}));

const widget = Widget.builder()
  .id('deadbeef')
  .type('dummy')
  .query(createElasticsearchQueryString('source:example.org'))
  .timerange({ type: 'relative', from: 300 })
  .config({})
  .build();

const renderEditor = (props: Partial<React.ComponentProps<typeof StandaloneWidgetEditor>> = {}) =>
  render(
    <StreamsContext.Provider value={[]}>
      <StandaloneWidgetEditor
        widget={widget}
        title="Example widget"
        onSave={() => Promise.resolve()}
        onCancel={() => {}}
        {...props}
      />
    </StreamsContext.Provider>,
  );

describe('StandaloneWidgetEditor', () => {
  useViewsPlugin();

  it('hands the edited widget and its title to `onSave`', async () => {
    const onSave = jest.fn(() => Promise.resolve());
    renderEditor({ onSave });

    await userEvent.click(await screen.findByRole('button', { name: /update widget/i }));

    await waitFor(() =>
      expect(onSave).toHaveBeenCalledWith(
        expect.objectContaining({ id: 'deadbeef', query: createElasticsearchQueryString('source:example.org') }),
        'Example widget',
      ),
    );
  });

  it('calls `onCancel` when cancelling', async () => {
    const onCancel = jest.fn();
    renderEditor({ onCancel });

    await userEvent.click(await screen.findByRole('button', { name: /cancel/i }));

    expect(onCancel).toHaveBeenCalled();
  });
});
