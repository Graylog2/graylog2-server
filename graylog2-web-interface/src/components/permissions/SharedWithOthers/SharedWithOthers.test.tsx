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
import * as Immutable from 'immutable';
import { render, screen, within } from 'wrappedTestingLibrary';
import { renderHook } from 'wrappedTestingLibrary/hooks';
import userEvent from '@testing-library/user-event';

import { AuthorizationShares } from '@graylog/server-api';

import { asMock } from 'helpers/mocking';
import { adminUser, alice } from 'fixtures/users';
import UserNotification from 'util/UserNotification';
import useCurrentUser from 'hooks/useCurrentUser';
import type User from 'logic/users/User';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';

import SharedWithOthersCell from './SharedWithOthersCell';
import SharedWithOthersSection from './SharedWithOthersSection';
import useShareSummary from './useShareSummary';
import useSharedWithOthersSection from './useSharedWithOthersSection';
import { formatShareCounts } from './shareCounts';

jest.mock('@graylog/server-api', () => ({
  AuthorizationShares: {
    getGrantees: jest.fn(),
  },
}));
jest.mock('util/UserNotification', () => ({ error: jest.fn() }));
jest.mock('./useShareSummary');
jest.mock('hooks/useCurrentUser');
jest.mock('components/common/EntityDataTable/hooks/useExpandedSections', () => jest.fn());

const STREAM_GRN = 'grn::::stream:stream-id';

describe('Shared With Others', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useCurrentUser).mockReturnValue(adminUser);
    asMock(useExpandedSections).mockReturnValue({ toggleSection: jest.fn(), expandedSections: {} });
  });

  describe('formatShareCounts', () => {
    it('lists the counts per access level and leaves out empty levels', () => {
      expect(formatShareCounts({ own: 1, manage: 0, view: 3 })).toBe('1 Owner, 3 Viewers');
      expect(formatShareCounts({ own: 2, manage: 1, view: 1 })).toBe('2 Owners, 1 Manager, 1 Viewer');
    });
  });

  describe('SharedWithOthersCell', () => {
    const renderCell = () => render(<SharedWithOthersCell entityId="stream-id" grn={STREAM_GRN} />);

    it('shows a spinner while loading', async () => {
      asMock(useShareSummary).mockReturnValue({ data: undefined, isInitialLoading: true, isError: false });
      renderCell();

      await screen.findByText(/loading/i);
    });

    it.each([
      ['no other grantees', { data: null, isInitialLoading: false, isError: false }],
      ['a failed request', { data: undefined, isInitialLoading: false, isError: true }],
    ])('renders nothing to click for %s', (_case, result) => {
      asMock(useShareSummary).mockReturnValue(result);
      renderCell();

      expect(screen.queryByRole('button')).not.toBeInTheDocument();
      expect(screen.queryByTitle(/grantees/)).not.toBeInTheDocument();
    });

    it('shows the counts and opens the section on click', async () => {
      const toggleSection = jest.fn();
      asMock(useExpandedSections).mockReturnValue({ toggleSection, expandedSections: {} });
      asMock(useShareSummary).mockReturnValue({
        data: { own: 1, manage: 0, view: 3 },
        isInitialLoading: false,
        isError: false,
      });
      renderCell();

      await userEvent.click(await screen.findByTitle('Show grantees'));

      expect(screen.getByTitle('Show grantees')).toHaveTextContent('1 Owner, 3 Viewers');
      expect(toggleSection).toHaveBeenCalledWith('stream-id', 'shared_with_others');
    });
  });

  describe('SharedWithOthersSection', () => {
    const renderSection = () => render(<SharedWithOthersSection grn={STREAM_GRN} entityTypeTitle="stream" />);

    it('shows a spinner while loading', async () => {
      asMock(AuthorizationShares.getGrantees).mockReturnValue(new Promise(() => {}));
      renderSection();

      await screen.findByText(/loading/i);
    });

    it('shows an error and one notification when loading fails, without retry', async () => {
      asMock(AuthorizationShares.getGrantees).mockRejectedValue(new Error('Internal Server Error'));
      renderSection();

      await screen.findByText('Could not load sharing information.');

      expect(UserNotification.error).toHaveBeenCalledTimes(1);
      expect(AuthorizationShares.getGrantees).toHaveBeenCalledTimes(1);
    });

    it('shows a text for an entity without other grantees', async () => {
      asMock(AuthorizationShares.getGrantees).mockResolvedValue({ grantees: [] });
      renderSection();

      await screen.findByText('This stream is not shared with others.');
    });

    it('lists the grantees sorted by access level and name', async () => {
      asMock(AuthorizationShares.getGrantees).mockResolvedValue({
        grantees: [
          { grantee: { id: 'grn::::user:bob', type: 'user', title: 'Bob' }, capability: 'view' },
          { grantee: { id: 'grn::::builtin-team:everyone', type: 'global', title: 'Everyone' }, capability: 'view' },
          { grantee: { id: 'grn::::team:team-1', type: 'team', title: 'Team A' }, capability: 'manage' },
          { grantee: { id: 'grn::::user:alice', type: 'user', title: 'Alice' }, capability: 'own' },
        ],
      });
      renderSection();

      await screen.findByText('Alice');
      const rows = screen.getAllByRole('listitem');

      expect(rows.map((row) => row.textContent)).toEqual([
        expect.stringMatching(/Alice.*Owner/),
        expect.stringMatching(/Team A.*Manager/),
        expect.stringMatching(/Bob.*Viewer/),
        expect.stringMatching(/Everyone.*Viewer/),
      ]);
      expect(within(rows[0]).getByText('Owner')).toBeInTheDocument();
    });
  });

  describe('useSharedWithOthersSection', () => {
    const stream = { id: 'stream-id' };

    it('offers no section while the column is hidden', () => {
      const { result } = renderHook(() => useSharedWithOthersSection('stream', 'stream', false));

      expect(result.current).toEqual({});
    });

    it('offers the section to admins and owners only', () => {
      const owner = alice
        .toBuilder()
        .grnPermissions(Immutable.List([`entity:own:${STREAM_GRN}`]))
        .build();

      const isAvailableFor = (user: User) => {
        asMock(useCurrentUser).mockReturnValue(user);
        const { result } = renderHook(() => useSharedWithOthersSection('stream', 'stream', true));

        return result.current.shared_with_others.isAvailable(stream);
      };

      expect(isAvailableFor(adminUser)).toBe(true);
      expect(isAvailableFor(owner)).toBe(true);
      expect(isAvailableFor(alice)).toBe(false);
    });
  });
});
