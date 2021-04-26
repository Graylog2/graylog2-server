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
import Immutable from 'immutable';

import AdaptableQueryTabs from './AdaptableQueryTabs';
import QueryTitleEditModal from './queries/QueryTitleEditModal';

const DEFAULT_PROPS = {
  maxWidth: 100,
  queries: ['qwerty', 'asdfgh', 'zxcvbn'],
  titles: Immutable.Map<string, string>([['qwerty', 'Tab 1'], ['asdfgh', 'Tab 2'], ['zxcvbn', 'Tab 3']]),
  selectedQueryId: 'qwerty',
  onRemove: () => Promise.resolve(),
  onSelect: (id: string) => Promise.resolve(id),
  queryTitleEditModal: React.createRef<QueryTitleEditModal>(),
};

describe('AdaptableQueryTabs', () => {
  it('renders main tabs', () => {
    render(<AdaptableQueryTabs {...DEFAULT_PROPS} />);

    const tab1 = screen.getByRole('button', {
      name: /Tab 1/i,
    });
    const tab2 = screen.getByRole('button', {
      name: /Tab 1/i,
    });
    const tab3 = screen.getByRole('button', {
      name: /Tab 1/i,
    });

    expect(tab1).toBeVisible();
    expect(tab1.parentNode).toHaveClass('active');
    expect(tab2).toBeVisible();
    expect(tab3).toBeVisible();
  });

  it('renders more dropdown & options', () => {
    render(<AdaptableQueryTabs {...DEFAULT_PROPS} />);

    const moreBtn = screen.getByRole('button', {
      name: /more dashboard tabs/i,
    });

    const more1 = screen.getByRole('menuitem', {
      name: /Tab 1/i,
    });
    const more2 = screen.getByRole('menuitem', {
      name: /Tab 1/i,
    });
    const more3 = screen.getByRole('menuitem', {
      name: /Tab 1/i,
    });

    expect(moreBtn).toBeInTheDocument();
    expect(moreBtn.parentNode).toHaveClass('hidden');
    expect(more1).toBeInTheDocument();
    expect(more2).toBeInTheDocument();
    expect(more3).toBeInTheDocument();
  });
});
