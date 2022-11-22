import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import EntityListItem from 'components/welcome/EntityListItem';

jest.mock('routing/Routes', () => ({ pluginRoute: (type) => (id) => `/route/${type}/${id}` }));

describe('EntityListItem', () => {
  it('Show type', async () => {
    render(<EntityListItem id="1" type="dashboard" title="Title 1" />);

    await screen.findByText('dashboard');
  });

  it('Show correct link', async () => {
    render(<EntityListItem id="1" type="dashboard" title="Title 1" />);

    const title = await screen.findByText('Title 1');
    const link = title.closest('a');

    expect(link).toHaveAttribute('href', '/route/DASHBOARDS_VIEWID/1');
  });
});
