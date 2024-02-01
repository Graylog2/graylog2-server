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

import { render, screen, waitFor } from 'wrappedTestingLibrary';
import React from 'react';
import Immutable, { Map } from 'immutable';
import userEvent from '@testing-library/user-event';

import type { TitlesMap } from 'views/stores/TitleTypes';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import useCurrentQueryId from 'views/logic/queries/useCurrentQueryId';
import { asMock } from 'helpers/mocking';

import OriginalAdaptableQueryTabs from './AdaptableQueryTabs';
import type QueryTitleEditModal from './queries/QueryTitleEditModal';

jest.mock('views/logic/queries/useCurrentQueryId');

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
  maxWidth: 600,
  queries: Immutable.OrderedSet(['query-id-1', 'query-id-2', 'query-id-3', 'query-id-4']),
  titles: Immutable.Map<string, string>([['query-id-1', 'Tab 1'], ['query-id-2', 'Tab 2'], ['query-id-3', 'Tab 3'], ['query-id-4', 'Tab 4']]),
  onRemove: () => Promise.resolve(),
  onTitleChange: () => Promise.resolve(Map(['tab', Map(['query-id-1', 'Tab 1'])]) as TitlesMap),
  onSelect: (id: string) => Promise.resolve(id),
  queryTitleEditModal: React.createRef<QueryTitleEditModal>(),
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

  const openMoreTabsDropdown = async () => {
    await userEvent.click(await screen.findByRole('button', { name: /more dashboard pages/i }));
    await screen.findByRole('menu');
  };

  const closeMoreTabsDropdown = async () => {
    userEvent.keyboard('{esc}');
    await waitFor(() => expect(screen.getByRole('menu')).not.toBeVisible());
  };

  const finishInitialRender = async () => {
    await screen.findByRole('menuitem', {
      name: 'Tab 4',
      hidden: true,
    });
  };

  beforeAll(loadViewsPlugin);

  beforeEach(() => {
    asMock(useCurrentQueryId).mockReturnValue('query-id-1');
  });

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
      await openMoreTabsDropdown();

      // Displays main tabs
      mainTabs.forEach((tabNr) => {
        expect(screen.getByRole(mainTabRole, {
          name: `Tab ${tabNr}`,
        })).toBeInTheDocument();

        // Does not display main tabs in dropdown
        expect(screen.queryByRole(dropdownTabRole, {
          name: `Tab ${tabNr}`,
        })).not.toBeInTheDocument();
      });

      // Displays dropdown tabs
      dropdownTabs.forEach((tabNr) => {
        expect(screen.getByRole(dropdownTabRole, {
          name: `Tab ${tabNr}`,
        })).toBeInTheDocument();

        // Does not display tabs in dropdown as main tabs
        expect(screen.queryByRole(mainTabRole, {
          name: `Tab ${tabNr}`,
        })).not.toBeInTheDocument();
      });

      await closeMoreTabsDropdown();
    });

    it('displays all tabs as main tabs if they fit in container row', async () => {
      render(<AdaptableQueryTabs {...DEFAULT_PROPS} maxWidth={2000} />);

      const mainTabs = [1, 2, 3, 4];

      await screen.findByRole(mainTabRole, {
        name: /Tab 4/i,
      });

      await openMoreTabsDropdown();

      // Displays main tabs
      mainTabs.forEach((tabNr) => {
        expect(screen.getByRole(mainTabRole, {
          name: `Tab ${tabNr}`,
        })).toBeInTheDocument();

        // Does not display main tabs in dropdown
        expect(screen.queryByRole(dropdownTabRole, {
          name: `Tab ${tabNr}`,
        })).not.toBeInTheDocument();
      });

      await closeMoreTabsDropdown();
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
      });
    });

    it('tab in dropdown is being displayed as main tab after selection', async () => {
      const { rerender } = render(<AdaptableQueryTabs {...DEFAULT_PROPS} />);
      await finishInitialRender();

      asMock(useCurrentQueryId).mockReturnValue('query-id-4');
      rerender(<AdaptableQueryTabs {...DEFAULT_PROPS} />);

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
      await openMoreTabsDropdown();

      await screen.findByRole(dropdownTabRole, { name: 'Page#5' });
    });
  });

  it('displays active tab', async () => {
    asMock(useCurrentQueryId).mockReturnValue('query-id-2');
    render(<AdaptableQueryTabs {...DEFAULT_PROPS} />);

    const tab2 = await screen.findByRole(mainTabRole, {
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
    await openMoreTabsDropdown();

    userEvent.click(await screen.findByRole(dropdownTabRole, { name: 'Tab 4' }));

    await expect(onSelectStub).toHaveBeenCalledTimes(1);

    expect(onSelectStub).toHaveBeenCalledWith('query-id-4');

    await closeMoreTabsDropdown();
  });

  it('creates new tab', async () => {
    const onSelectStub = jest.fn((id: string) => Promise.resolve(id));
    render(<AdaptableQueryTabs {...DEFAULT_PROPS} onSelect={onSelectStub} />);

    userEvent.click(await screen.findByTitle('Create New Page'));

    await expect(onSelectStub).toHaveBeenCalledTimes(1);

    expect(onSelectStub).toHaveBeenCalledWith('new');
  });

  it('should show copy page to dashboard modal', async () => {
    render(<AdaptableQueryTabs {...DEFAULT_PROPS} />);

    userEvent.click((await screen.findAllByRole('button', { name: /page actions/i }))[0]);
    userEvent.click(await screen.findByRole('menuitem', { name: /copy to dashboard/i, hidden: true }));

    await screen.findByRole('button', {
      name: /copy page/i,
      hidden: true,
    });
  });
});
