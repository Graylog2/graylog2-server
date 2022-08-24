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
import { render, screen } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import usePluginEntities from 'hooks/usePluginEntities';

import PluggableSearchBarControls from './PluggableSearchBarControls';

jest.mock('hooks/usePluginEntities');
jest.mock('hooks/useFeature', () => (key) => key === 'search_filter');

describe('PluggableSearchBarControls', () => {
  const createPluggableSearchBarControl = (overrides = {}) => {
    return () => ({
      id: 'example-component',
      placement: 'right',
      component: () => <div>Example Component</div>,
      ...overrides,
    });
  };

  it('should render left search bar controls from plugins', () => {
    const example = createPluggableSearchBarControl({ placement: 'left' });

    asMock(usePluginEntities).mockImplementation((entityKey) => ({ 'views.components.searchBar': [example] }[entityKey]));
    render(<PluggableSearchBarControls />);

    expect(screen.getByText('Example Component')).toBeInTheDocument();
  });

  it('should render right search bar controls from plugins', () => {
    const example = createPluggableSearchBarControl({ placement: 'right' });
    asMock(usePluginEntities).mockImplementation((entityKey) => ({ 'views.components.searchBar': [example] }[entityKey]));
    render(<PluggableSearchBarControls />);

    expect(screen.getByText('Example Component')).toBeInTheDocument();
  });

  it('should render fallback for search bar filters', () => {
    asMock(usePluginEntities).mockImplementation((entityKey) => ({ 'views.components.searchBar': [] }[entityKey]));
    render(<PluggableSearchBarControls />);

    expect(screen.getByText('Filters')).toBeInTheDocument();
  });

  it('should not render fallback when search bar filters are defined', () => {
    const example = createPluggableSearchBarControl({ id: 'search-filters', placement: 'left' });
    asMock(usePluginEntities).mockImplementation((entityKey) => ({ 'views.components.searchBar': [example] }[entityKey]));
    render(<PluggableSearchBarControls />);

    expect(screen.queryByText('Filters')).not.toBeInTheDocument();
  });
});
