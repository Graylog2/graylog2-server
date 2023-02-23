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

import { render, screen } from 'wrappedTestingLibrary';
import React from 'react';
import Immutable, { Map } from 'immutable';
import userEvent from '@testing-library/user-event';

import type { TitlesMap } from 'views/stores/TitleTypes';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';

import OriginalAdaptableQueryTabs from './AdaptableQueryTabs';
import type QueryTitleEditModal from './queries/QueryTitleEditModal';

jest.mock('views/components/dashboard/hooks/useDashboards', () => () => ({
  data: {
    pagination: {
      total: 0,
      page: 1,
      perPage: 5,
      count: 0,
    },
    list: [],
  },
  refetch: () => {},
}));

Object.defineProperties(window.HTMLElement.prototype, {
  offsetWidth: {
    get() {
      return 100;
    },
  },
});

const DEFAULT_PROPS = {
  maxWidth: 500,
  queries: Immutable.OrderedSet(['query-id-1', 'query-id-2', 'query-id-3', 'query-id-4']),
  titles: Immutable.Map<string, string>([['query-id-1', 'Tab 1'], ['query-id-2', 'Tab 2'], ['query-id-3', 'Tab 3'], ['query-id-4', 'Tab 4']]),
  onRemove: () => Promise.resolve(),
  onTitleChange: () => Promise.resolve(Map(['tab', Map(['query-id-1', 'Tab 1'])]) as TitlesMap),
  onSelect: (id: string) => Promise.resolve(id),
  queryTitleEditModal: React.createRef<QueryTitleEditModal>(),
  activeQueryId: 'query-id-1',
  dashboardId: 'dashboard-id',
};

const AdaptableQueryTabs = (props: React.ComponentProps<typeof OriginalAdaptableQueryTabs>) => (
  <TestStoreProvider>
    <OriginalAdaptableQueryTabs {...props} />
  </TestStoreProvider>
);

describe('AdaptableQueryTabs', () => {
  const mainTabRole = 'button';
  const dropdownTabRole = 'menuitem';

  const finishInitialRender = async () => {
    await screen.findByRole('menuitem', {
      name: 'Tab 4',
      hidden: true,
    });
  };

  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  describe('renders main tabs and more tabs dropdown based on container width', () => {
    // Defaults widths: Container width = 500px, create tab button + more tabs dropdown button with = 215px, width of one main tab = 100px
    // Available space for main tabs: 500 - 215 = 285px.
    // Initial navigation sate: main tabs = 1. & 2. tab (= 200px), dropdown tabs: 3. & 4. tab.

    it('displays tabs in dropdown if they do not fit in container row', async () => {
      render(<AdaptableQueryTabs {...DEFAULT_PROPS} />);
      const mainTabs = [1, 2];
      const dropdownTabs = [3, 4];

      await finishInitialRender();

      // Displays main tabs
      mainTabs.forEach((tabNr) => {
        expect(screen.getByRole(mainTabRole, {
          name: `Tab ${tabNr}`,
        })).toBeInTheDocument();

        // Does not display main tabs in dropdown
        expect(screen.getByRole(dropdownTabRole, {
          name: `Tab ${tabNr}`,
          hidden: true,
        })).toBeInTheDocument();
      });

      // Displays dropdown tabs
      dropdownTabs.forEach((tabNr) => {
        expect(screen.getByRole(dropdownTabRole, {
          name: `Tab ${tabNr}`,
        })).toBeInTheDocument();

        // Does not display tabs in dropdown as main tabs
        expect(screen.getByRole(mainTabRole, {
          name: `Tab ${tabNr}`,
          hidden: true,
        })).toBeInTheDocument();
      });
    });

    it('displays all tabs as main tabs if they fit in container row', async () => {
      render(<AdaptableQueryTabs {...DEFAULT_PROPS} maxWidth={2000} />);

      const mainTabs = [1, 2, 3, 4];

      await screen.findByRole(mainTabRole, {
        name: /Tab 4/i,
      });

      // Displays main tabs
      mainTabs.forEach((tabNr) => {
        expect(screen.getByRole(mainTabRole, {
          name: `Tab ${tabNr}`,
        })).toBeInTheDocument();

        // Does not display main tabs in dropdown
        expect(screen.getByRole(dropdownTabRole, {
          name: `Tab ${tabNr}`,
          hidden: true,
        })).toBeInTheDocument();
      });
    });

    it('displays all tabs as main tabs if they fit in container row after resizing', async () => {
      const { rerender } = render(<AdaptableQueryTabs {...DEFAULT_PROPS} />);

      await finishInitialRender();

      rerender(<AdaptableQueryTabs {...DEFAULT_PROPS} maxWidth={2000} />);

      const mainTabs = [1, 2, 3, 4];

      await screen.findByRole(mainTabRole, {
        name: /Tab 4/i,
      });

      // Displays main tabs
      mainTabs.forEach((tabNr) => {
        expect(screen.getByRole(mainTabRole, {
          name: `Tab ${tabNr}`,
        })).toBeInTheDocument();

        // Does not display main tabs in dropdown
        expect(screen.getByRole(dropdownTabRole, {
          name: `Tab ${tabNr}`,
          hidden: true,
        })).toBeInTheDocument();
      });
    });

    it('tab in dropdown is being displayed as main tab after selection', async () => {
      const { rerender } = render(<AdaptableQueryTabs {...DEFAULT_PROPS} />);

      await finishInitialRender();

      rerender(<AdaptableQueryTabs {...DEFAULT_PROPS} activeQueryId="query-id-4" />);

      const newActiveTab = await screen.findByRole(mainTabRole, {
        name: 'Tab 4',
      });

      // eslint-disable-next-line testing-library/no-node-access
      expect(newActiveTab.parentNode).toHaveClass('active');
    });

    it('displays new tab in dropdown if it does not fit in container row', async () => {
      const { rerender } = render(<AdaptableQueryTabs {...DEFAULT_PROPS} />);

      await finishInitialRender();

      const newQueries = DEFAULT_PROPS.queries.add('query-id-5');
      rerender(<AdaptableQueryTabs {...DEFAULT_PROPS} queries={newQueries} />);

      await screen.findByRole(dropdownTabRole, {
        name: 'Page#5',
      });

      await screen.findByRole(mainTabRole, {
        name: 'Page#5',
        hidden: true,
      });
    });
  });

  it('displays active tab', () => {
    render(<AdaptableQueryTabs {...DEFAULT_PROPS} activeQueryId="query-id-2" />);

    const tab2 = screen.getByRole(mainTabRole, {
      name: 'Tab 2',
    });

    expect(tab2).toBeVisible();
    // eslint-disable-next-line testing-library/no-node-access
    expect(tab2.parentNode).toHaveClass('active');
  });

  it('selects main tab', async () => {
    const onSelectStub = jest.fn((id: string) => Promise.resolve(id));
    render(<AdaptableQueryTabs {...DEFAULT_PROPS} onSelect={onSelectStub} />);

    const tab2 = await screen.findByRole(mainTabRole, {
      name: 'Tab 2',
    });
    userEvent.click(tab2);

    await expect(onSelectStub).toHaveBeenCalledTimes(1);

    expect(onSelectStub).toHaveBeenCalledWith('query-id-2');
  });

  it('selects dropdown tab', async () => {
    const onSelectStub = jest.fn((id: string) => Promise.resolve(id));
    render(<AdaptableQueryTabs {...DEFAULT_PROPS} onSelect={onSelectStub} />);

    await finishInitialRender();

    const tab4 = screen.getByRole(dropdownTabRole, {
      name: 'Tab 4',
      hidden: true,
    });
    userEvent.click(tab4);

    await expect(onSelectStub).toHaveBeenCalledTimes(1);

    expect(onSelectStub).toHaveBeenCalledWith('query-id-4');
  });

  it('creates new tab', async () => {
    const onSelectStub = jest.fn((id: string) => Promise.resolve(id));
    render(<AdaptableQueryTabs {...DEFAULT_PROPS} onSelect={onSelectStub} />);

    const createTabButton = await screen.findByTitle('Create New Page');
    userEvent.click(createTabButton);

    await expect(onSelectStub).toHaveBeenCalledTimes(1);

    expect(onSelectStub).toHaveBeenCalledWith('new');
  });

  it('should show copy page to dashboard modal', async () => {
    render(<AdaptableQueryTabs {...DEFAULT_PROPS} activeQueryId="query-id-1" />);

    userEvent.click((await screen.findAllByTitle(/page actions/i))[0]);
    userEvent.click(await screen.findByRole('menuitem', { name: /copy to dashboard/i }));

    await screen.findByRole('button', {
      name: /copy page/i,
      hidden: true,
    });
  });
});
