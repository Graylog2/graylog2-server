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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import {
  AssistantPageContextProvider,
  useAssistantPageContext,
  useAssistantPageSnapshot,
  useAssistantPageTools,
  useAssistantPanelContext,
} from './AssistantPageContext';
import type { Page, PageTools, Panel } from './types';

jest.mock('routing/useLocation', () => () => ({ pathname: '/search/66b2' }));

const searchPage: Page = { type: 'search', view: { id: '66b2' }, dirty: false };
const eventPanel: Panel = { type: 'event', event_id: '01J' };
const tools: PageTools = { add_widget: () => ({ summary: 'Added' }) };

const SearchPage = () => {
  useAssistantPageContext(searchPage);
  useAssistantPageTools(tools);

  return null;
};

const EventSidebar = () => {
  useAssistantPanelContext(eventPanel);

  return null;
};

const Snapshot = () => {
  const { getSnapshot, getTools } = useAssistantPageSnapshot();
  const [snapshot, setSnapshot] = useState<string>();

  return (
    <>
      <button type="button" onClick={() => setSnapshot(JSON.stringify({ ...getSnapshot(), tools: Object.keys(getTools()) }))}>
        Read
      </button>
      <pre>{snapshot}</pre>
    </>
  );
};

const readSnapshot = async () => {
  await userEvent.click(screen.getByRole('button', { name: 'Read' }));

  return JSON.parse(screen.getByText(/route/).textContent);
};

describe('AssistantPageContext', () => {
  it('combines the published page, panels and tools', async () => {
    render(
      <AssistantPageContextProvider>
        <SearchPage />
        <EventSidebar />
        <Snapshot />
      </AssistantPageContextProvider>,
    );

    expect(await readSnapshot()).toEqual(
      expect.objectContaining({
        version: 1,
        route: '/search/66b2',
        page: searchPage,
        panels: [eventPanel],
        tools: ['add_widget'],
      }),
    );
  });

  it('falls back to a generic page once the page unmounts', async () => {
    const { rerender } = render(
      <AssistantPageContextProvider>
        <SearchPage />
        <Snapshot />
      </AssistantPageContextProvider>,
    );

    rerender(
      <AssistantPageContextProvider>
        <Snapshot />
      </AssistantPageContextProvider>,
    );

    expect(await readSnapshot()).toEqual(expect.objectContaining({ page: { type: 'generic' }, panels: [], tools: [] }));
  });
});
