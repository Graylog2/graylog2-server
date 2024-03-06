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
import { defaultPerspective, examplePerspective } from 'fixtures/perspectives';

import PerspectivesSwitcher from './PerspectivesSwitcher';

jest.mock('components/perspectives/hooks/usePerspectives', () => jest.fn());
jest.mock('components/perspectives/hooks/useActivePerspective', () => jest.fn());
jest.mock('hooks/useFeature', () => (featureFlag: string) => featureFlag === 'frontend_perspectives');
jest.mock('routing/useHistory');

describe('PerspectivesSwitcher', () => {
  let history;
  const setActivePerspective = jest.fn();
  const mockedPerspectives = [defaultPerspective, examplePerspective];

  beforeEach(() => {
    history = mockHistory();
    asMock(useHistory).mockReturnValue(history);

    asMock(usePerspectives).mockReturnValue(mockedPerspectives);

    asMock(useActivePerspective).mockReturnValue({
      activePerspective: mockedPerspectives[0],
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
    userEvent.click(await screen.findByText(new RegExp(examplePerspective.title, 'i')));

    await waitFor(() => expect(history.push).toHaveBeenCalledWith(examplePerspective.welcomeRoute));

    expect(setActivePerspective).toHaveBeenCalledWith(examplePerspective.id);
  });
});
