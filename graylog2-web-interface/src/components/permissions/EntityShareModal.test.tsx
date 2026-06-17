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
import { render, waitFor, screen } from 'wrappedTestingLibrary';
import { act } from 'react';
import userEvent from '@testing-library/user-event';

import EntityShareDomain from 'domainActions/permissions/EntityShareDomain';
import selectEvent from 'helpers/selectEvent';
import asMock from 'helpers/mocking/AsMock';
import mockEntityShareState, {
  failedEntityShareState,
  john,
  jane,
  everyone,
  security,
  viewer,
  owner,
  manager,
} from 'fixtures/entityShareState';
import ActiveShare from 'logic/permissions/ActiveShare';
import useWindowConfirmMock from 'helpers/mocking/useWindowConfirmMock';
import useEntityShareState, { useSetEntityShareState } from 'hooks/useEntityShareState';

import EntityShareModal from './EntityShareModal';

const mockEmptyResult = { data: undefined };
const mockFailedResult = { data: failedEntityShareState };

jest.mock('domainActions/permissions/EntityShareDomain', () => ({
  __esModule: true,
  default: {
    prepare: jest.fn(() => Promise.resolve()),
    update: jest.fn(() => Promise.resolve()),
    loadUserSharesPaginated: jest.fn(() =>
      Promise.resolve({
        list: require('immutable').List(),
        pagination: { page: 1, perPage: 10, query: '', total: 0, count: 0 },
      }),
    ),
  },
}));
jest.mock('hooks/useEntityShareState', () => {
  const mockSetEntityShareState = jest.fn();

  return {
    __esModule: true,
    default: jest.fn(() => ({ data: undefined })),
    useSetEntityShareState: jest.fn(() => mockSetEntityShareState),
    entityShareQueryKey: jest.fn((grn) => ['entity-share', grn ?? 'new']),
  };
});

jest.setTimeout(10000);

const setupUser = () => userEvent.setup({ advanceTimers: jest.advanceTimersByTime });

describe('EntityShareModal', () => {
  beforeEach(() => {
    asMock(useEntityShareState).mockReturnValue({ data: mockEntityShareState } as any);
  });

  beforeAll(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  const SimpleEntityShareModal = ({ ...props }) => (
    <EntityShareModal
      description="The description"
      entityId="dashboard-id"
      entityType="dashboard"
      onClose={() => {}}
      entityTitle="The title"
      {...props}
    />
  );

  const getModalSubmitButton = () => screen.queryByRole('button', { name: /update sharing/i });

  it('fetches entity share state initially', async () => {
    render(<SimpleEntityShareModal />);

    await waitFor(() => {
      expect(EntityShareDomain.prepare).toHaveBeenCalledWith('dashboard', 'The title', mockEntityShareState.entity);
    });
  });

  it('updates entity share state on submit', async () => {
    render(<SimpleEntityShareModal />);

    await setupUser().click(await screen.findByRole('button', { name: /update sharing/i }));

    await waitFor(() => expect(EntityShareDomain.update).toHaveBeenCalledTimes(1));

    expect(EntityShareDomain.update).toHaveBeenCalledWith('dashboard', 'The title', mockEntityShareState.entity, {
      selected_grantee_capabilities: mockEntityShareState.selectedGranteeCapabilities,
    });
  });

  it('closes modal on cancel', async () => {
    const onClose = jest.fn();
    render(<SimpleEntityShareModal onClose={onClose} />);

    const cancelButton = await screen.findByRole('button', {
      name: /cancel/i,
    });

    await setupUser().click(cancelButton);

    await waitFor(() => {
      expect(onClose).toHaveBeenCalledTimes(1);
    });
  });

  describe('displays', () => {
    it('loading spinner while loading entity share state', async () => {
      asMock(useEntityShareState).mockReturnValue(mockEmptyResult as any);
      render(<SimpleEntityShareModal />);

      act(() => {
        jest.advanceTimersByTime(200);
      });

      expect(await screen.findByText('Loading...')).not.toBeNull();
    });

    it('displays an error if validation failed and disables submit', async () => {
      asMock(useEntityShareState).mockReturnValue(mockFailedResult as any);
      render(<SimpleEntityShareModal />);

      await screen.findByText('Removing the following owners will leave the entity ownerless:');

      await waitFor(() => {
        expect(getModalSubmitButton()).toBeDisabled();
      });
    });

    it('necessary information', async () => {
      render(<SimpleEntityShareModal />);

      // provided description
      expect(await screen.findByText('The description')).not.toBeNull();
      // Provided title
      expect(await screen.findByText('The title')).not.toBeNull();
      // sharable urls
      expect(await screen.findByDisplayValue('http://localhost/dashboards/dashboard-id')).not.toBeNull();
      // missing dependencies warning
      expect(
        await screen.findByText('There are missing dependencies for the current set of collaborators'),
      ).not.toBeNull();
      expect(await screen.findByText(/needs access to/)).not.toBeNull();
    });
  });

  describe('grantee selector', () => {
    useWindowConfirmMock();

    describe('adds new selected grantee', () => {
      const addGrantee = async ({ newGrantee, capability }) => {
        render(<SimpleEntityShareModal />);

        await selectEvent.chooseOption('Search for users and teams', newGrantee.title);
        await selectEvent.chooseOption('Select a capability', capability.title);

        // Submit form
        const submitButton = await screen.findByRole('button', {
          name: /add collaborator/i,
        });

        await setupUser().click(submitButton);

        await waitFor(() => {
          expect(EntityShareDomain.prepare).toHaveBeenCalledWith(
            'dashboard',
            'The title',
            mockEntityShareState.entity,
            {
              selected_grantee_capabilities: mockEntityShareState.selectedGranteeCapabilities.merge({
                [newGrantee.id]: capability.id,
              }),
            },
          );
        });
      };

      // eslint-disable-next-line jest/expect-expect
      it.each`
        newGrantee  | granteeType   | capability
        ${john}     | ${'user'}     | ${viewer}
        ${everyone} | ${'everyone'} | ${manager}
        ${security} | ${'team'}     | ${owner}
      `('sends new grantee $granteeType to preparation', addGrantee);

      it('writes the response from prepare back into the entity share cache', async () => {
        const updatedShareState = mockEntityShareState
          .toBuilder()
          .selectedGranteeCapabilities(mockEntityShareState.selectedGranteeCapabilities.merge({ [john.id]: viewer.id }))
          .build();
        // First call (from initial useEffect in EntityShareModal) returns the initial state;
        // second call (from _handleSelection after clicking Add Collaborator) returns the updated state.
        asMock(EntityShareDomain.prepare)
          .mockResolvedValueOnce(mockEntityShareState)
          .mockResolvedValueOnce(updatedShareState);

        render(<SimpleEntityShareModal />);

        await selectEvent.chooseOption('Search for users and teams', john.title);
        await selectEvent.chooseOption('Select a capability', viewer.title);

        await setupUser().click(await screen.findByRole('button', { name: /add collaborator/i }));

        const setEntityShareStateMock = (useSetEntityShareState as jest.Mock).mock.results[0]?.value;

        await waitFor(() => {
          expect(setEntityShareStateMock).toHaveBeenCalledWith(mockEntityShareState.entity, updatedShareState);
        });
      });
    });

    it('shows confirmation dialog on save if a collaborator got selected, but not added', async () => {
      render(<SimpleEntityShareModal />);

      await selectEvent.chooseOption('Search for users and teams', john.title);

      await setupUser().click(await screen.findByRole('button', { name: /update sharing/i }));

      await waitFor(() => {
        expect(window.confirm).toHaveBeenCalledWith(
          `"${john.title}" got selected but was never added as a collaborator. Do you want to continue anyway?`,
        );
      });
    });
  });

  describe('selected grantees list', () => {
    it('lists grantees', async () => {
      const ownerTitle = jane.title;
      render(<SimpleEntityShareModal />);

      await screen.findByText(ownerTitle);
    });

    it('allows updating the capability of a grantee', async () => {
      const ownerTitle = jane.title;
      render(<SimpleEntityShareModal />);

      await selectEvent.chooseOption(`Change the capability for ${ownerTitle}`, viewer.title);

      await waitFor(() => {
        expect(screen.queryAllByText(viewer.title)).toHaveLength(2);
      });

      await waitFor(() => {
        expect(EntityShareDomain.prepare).toHaveBeenCalledWith('dashboard', 'The title', mockEntityShareState.entity, {
          selected_grantee_capabilities: mockEntityShareState.selectedGranteeCapabilities.merge({
            [jane.id]: viewer.id,
          }),
        });
      });
    });

    describe('allows deleting a grantee', () => {
      // active shares
      const janeIsOwner = ActiveShare.builder().grant('grant-id-1').grantee(jane.id).capability(owner.id).build();
      const securityIsManager = ActiveShare.builder()
        .grant('grant-id-2')
        .grantee(security.id)
        .capability(manager.id)
        .build();
      const everyoneIsViewer = ActiveShare.builder()
        .grant('grant-id-3')
        .grantee(everyone.id)
        .capability(viewer.id)
        .build();
      const activeShares = Immutable.List([janeIsOwner, securityIsManager, everyoneIsViewer]);
      const selectedGranteeCapabilities = Immutable.Map({
        [janeIsOwner.grantee]: janeIsOwner.capability,
        [securityIsManager.grantee]: securityIsManager.capability,
        [everyoneIsViewer.grantee]: everyoneIsViewer.capability,
      });
      const entityShareState = mockEntityShareState
        .toBuilder()
        .activeShares(activeShares)
        .selectedGranteeCapabilities(selectedGranteeCapabilities)
        .build();

      beforeEach(() => {
        asMock(useEntityShareState).mockReturnValue({ data: entityShareState } as any);
      });

      const deleteGrantee = async ({ grantee }) => {
        render(<SimpleEntityShareModal />);

        const deleteButton = await screen.findByTitle(`Remove sharing for ${grantee.title}`);

        await setupUser().click(deleteButton);

        await waitFor(() => {
          expect(EntityShareDomain.prepare).toHaveBeenCalledWith(
            'dashboard',
            'The title',
            mockEntityShareState.entity,
            {
              selected_grantee_capabilities: selectedGranteeCapabilities.remove(grantee.id),
            },
          );
        });
      };

      // eslint-disable-next-line jest/expect-expect
      it.each`
        grantee     | granteeType   | capability
        ${jane}     | ${'user'}     | ${viewer}
        ${security} | ${'team'}     | ${manager}
        ${everyone} | ${'everyone'} | ${owner}
      `('sends deleted grantee $granteeType to preparation', deleteGrantee);
    });
  });
});
