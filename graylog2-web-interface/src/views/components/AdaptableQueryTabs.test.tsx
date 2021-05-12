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

import { TitlesMap } from 'views/stores/TitleTypes';

import AdaptableQueryTabs from './AdaptableQueryTabs';
import QueryTitleEditModal from './queries/QueryTitleEditModal';

Object.defineProperties(window.HTMLElement.prototype, {
  offsetWidth: {
    get() {
      return 100;
    },
  },
});

const DEFAULT_PROPS = {
  maxWidth: 500,
  queries: Immutable.List(['query-id-1', 'query-id-2', 'query-id-3', 'query-id-4']),
  titles: Immutable.Map<string, string>([['query-id-1', 'Tab 1'], ['query-id-2', 'Tab 2'], ['query-id-3', 'Tab 3'], ['query-id-4', 'Tab 4']]),
  selectedQueryId: 'query-id-1',
  onRemove: () => Promise.resolve(),
  onTitleChange: () => Promise.resolve(Map(['tab', Map(['query-id-1', 'Tab 1'])]) as TitlesMap),
  onSelect: (id: string) => Promise.resolve(id),
  queryTitleEditModal: React.createRef<QueryTitleEditModal>(),
};

describe('AdaptableQueryTabs', () => {
  const mainTabRole = 'button';
  const dropdownTabRole = 'menuitem';

  const finishInitialRender = async () => {
    await screen.findByRole('menuitem', {
      name: /Tab 4/i,
      hidden: true,
    });
  };

  describe('renders main tabs and more tabs dropdown based on container width', () => {
    // Defaults widths: Container width = 500px, create tab button + more tabs dropdown button with = 215px, width of one main tab = 100px
    // Available space for main tabs: 500 - 215 = 285px.
    // Initial navigation sate: main tabs = 1. & 2. tab (= 200px), dropdown tabs: 3. & 4. tab.

    it('displays tabs in dropdown if they do not fit in container row', async () => {
      render(<AdaptableQueryTabs {...DEFAULT_PROPS} />);
      const mainTabs = [1, 2];
      const dropdownTabs = [2, 3];

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

      rerender(<AdaptableQueryTabs {...DEFAULT_PROPS} selectedQueryId="query-id-4" />);

      const newActiveTab = await screen.findByRole(mainTabRole, {
        name: 'Tab 4',
      });

      expect(newActiveTab.parentNode).toHaveClass('active');
    });

    it('displays new tab in dropdown if it does not fit in container row', async () => {
      const { rerender } = render(<AdaptableQueryTabs {...DEFAULT_PROPS} />);

      await finishInitialRender();

      const newQueries = DEFAULT_PROPS.queries.push('query-id-5');
      rerender(<AdaptableQueryTabs {...DEFAULT_PROPS} queries={newQueries} />);

      await screen.findByRole(dropdownTabRole, {
        name: 'Page#5',
      });

      expect(screen.getByRole(mainTabRole, {
        name: 'Page#5',
        hidden: true,
      })).toBeInTheDocument();
    });
  });

  it('displays active tab', () => {
    render(<AdaptableQueryTabs {...DEFAULT_PROPS} selectedQueryId="query-id-2" />);

    const tab2 = screen.getByRole(mainTabRole, {
      name: /Tab 2/i,
    });

    expect(tab2).toBeVisible();
    expect(tab2.parentNode).toHaveClass('active');
  });
});
