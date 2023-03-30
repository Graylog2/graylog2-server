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

import Store from 'logic/local-storage/Store';
import asMock from 'helpers/mocking/AsMock';
import usePluginEntities from 'hooks/usePluginEntities';

import PluggableSearchBarControls from './PluggableSearchBarControls';

jest.mock('hooks/usePluginEntities');
jest.mock('hooks/useFeature', () => (key) => key === 'search_filter');

jest.mock('logic/local-storage/Store', () => ({
  get: jest.fn(),
  set: jest.fn(),
}));

describe('PluggableSearchBarControls', () => {
  beforeEach(() => {
    asMock(usePluginEntities).mockReturnValue([]);
    Store.get.mockReturnValue(false);
  });

  const createPluggableSearchBarControl = (overrides = {}) => () => ({
    id: 'example-component',
    placement: 'right',
    component: () => <div>Example Component</div>,
    ...overrides,
  });

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

  it('should not render anything when there are no pluggable controls and search filter preview is hidden', () => {
    Store.get.mockReturnValue(true);
    const { container } = render(<PluggableSearchBarControls />);

    expect(container.firstChild).toBeNull();
  });

  it('should not render anything when there are no pluggable controls and `showLeftControls` is `false`', () => {
    const { container } = render(<PluggableSearchBarControls showLeftControls={false} />);

    expect(container.firstChild).toBeNull();
  });
});
