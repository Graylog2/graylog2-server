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
import React from 'react';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import useHistory from 'routing/useHistory';
import usePerspectives from 'components/perspectives/hooks/usePerspectives';
import { asMock } from 'helpers/mocking';
import useActivePerspective from 'components/perspectives/hooks/useActivePerspective';
import mockHistory from 'helpers/mocking/mockHistory';

import PerspectivesSwitcher from './PerspectivesSwitcher';

jest.mock('components/perspectives/hooks/usePerspectives', () => jest.fn());
jest.mock('components/perspectives/hooks/useActivePerspective', () => jest.fn());
jest.mock('hooks/useFeature', () => (featureFlag: string) => featureFlag === 'frontend_perspectives');
jest.mock('routing/useHistory');

describe('PerspectivesSwitcher', () => {
  let history;
  const setActivePerspective = jest.fn();

  beforeEach(() => {
    history = mockHistory();
    asMock(useHistory).mockReturnValue(history);

    asMock(usePerspectives).mockReturnValue([
      {
        id: 'default',
        title: 'Default Perspective',
        brandComponent: () => <div>Default perspective</div>,
        brandLink: '',
      },
      {
        id: 'example-perspective',
        title: 'Example Perspective',
        brandComponent: () => <div />,
        brandLink: '/example-perspective',
      },
    ]);

    asMock(useActivePerspective).mockReturnValue({
      activePerspective: 'default',
      setActivePerspective,
    });
  });

  it('should render brand for active perspective', async () => {
    render(<PerspectivesSwitcher />);

    await screen.findByText('Default perspective');
  });

  it('should render dropdown with available perspectives', async () => {
    render(<PerspectivesSwitcher />);

    userEvent.click(await screen.findByRole('button', { name: /change ui perspective/i }));
    userEvent.click(await screen.findByText(/example perspective/i));

    await waitFor(() => expect(history.push).toHaveBeenCalledWith('/example-perspective'));

    expect(setActivePerspective).toHaveBeenCalledWith('example-perspective');
  });
});
