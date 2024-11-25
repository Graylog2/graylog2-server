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
import { render, fireEvent, waitFor, screen } from 'wrappedTestingLibrary';
import { act } from 'react';
import selectEvent from 'react-select-event';

import asMock from 'helpers/mocking/AsMock';
import mockEntityShareState, { failedEntityShareState, john, jane, everyone, security, viewer, owner, manager } from 'fixtures/entityShareState';
import ActiveShare from 'logic/permissions/ActiveShare';
import { EntityShareStore, EntityShareActions } from 'stores/permissions/EntityShareStore';

import EntityShareModal from './EntityShareModal';

const mockEmptyStore = { state: undefined };
const mockFailedStore = { state: failedEntityShareState };

jest.mock('stores/permissions/EntityShareStore', () => ({
  EntityShareActions: {
    prepare: jest.fn(() => Promise.resolve()),
    update: jest.fn(() => Promise.resolve()),
  },
  EntityShareStore: {
    listen: jest.fn(),
    getInitialState: jest.fn(),
  },
}));

jest.setTimeout(10000);

describe('EntityShareModal', () => {
  beforeEach(() => {
    asMock(EntityShareStore.getInitialState).mockReturnValue({ state: mockEntityShareState });
  });

  beforeAll(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  const SimpleEntityShareModal = ({ ...props }) => (
    <EntityShareModal description="The description"
                      entityId="dashboard-id"
                      entityType="dashboard"
                      onClose={() => {}}
                      entityTitle="The title"
                      {...props} />
  );

  const getModalSubmitButton = () => screen.queryByRole('button', { name: /update sharing/i, hidden: true });

  it('fetches entity share state initially', async () => {
    render(<SimpleEntityShareModal />);

    await waitFor(() => {
      expect(EntityShareActions.prepare).toHaveBeenCalledWith('dashboard', 'The title', mockEntityShareState.entity);
    });
  });

  it('updates entity share state on submit', async () => {
    render(<SimpleEntityShareModal />);

    fireEvent.click(await screen.findByRole('button', { name: /update sharing/i, hidden: true }));

    await waitFor(() => expect(EntityShareActions.update).toHaveBeenCalledTimes(1));

    expect(EntityShareActions.update).toHaveBeenCalledWith('dashboard', 'The title', mockEntityShareState.entity, {
      selected_grantee_capabilities: mockEntityShareState.selectedGranteeCapabilities,
    });
  });

  it('closes modal on cancel', async () => {
    const onClose = jest.fn();
    render(<SimpleEntityShareModal onClose={onClose} />);

    const cancelButton = await screen.findByRole('button', {
      name: /cancel/i,
      hidden: true,
    });

    fireEvent.click(cancelButton);

    await waitFor(() => {
      expect(onClose).toHaveBeenCalledTimes(1);
    });
  });

  describe('displays', () => {
    it('loading spinner while loading entity share state', async () => {
      asMock(EntityShareStore.getInitialState).mockReturnValue(mockEmptyStore);
      render(<SimpleEntityShareModal />);

      act(() => { jest.advanceTimersByTime(200); });

      expect(await screen.findByText('Loading...')).not.toBeNull();
    });

    it('displays an error if validation failed and disables submit', async () => {
      asMock(EntityShareStore.getInitialState).mockReturnValue(mockFailedStore);
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
      expect(await screen.findByText('There are missing dependencies for the current set of collaborators')).not.toBeNull();
      expect(await screen.findByText(/needs access to/)).not.toBeNull();
    });
  });

  describe('grantee selector', () => {
    let oldConfirm;

    beforeEach(() => {
      oldConfirm = window.confirm;
      window.confirm = jest.fn(() => true);
    });

    afterEach(() => {
      window.confirm = oldConfirm;
    });

    describe('adds new selected grantee', () => {
      const addGrantee = async ({ newGrantee, capability }) => {
        render(<SimpleEntityShareModal />);

        // Select a grantee
        const granteesSelect = await screen.findByLabelText('Search for users and teams');

        await act(async () => {
          await selectEvent.openMenu(granteesSelect);
        });

        await act(async () => {
          await selectEvent.select(granteesSelect, newGrantee.title);
        });

        // Select a capability
        const capabilitySelect = await screen.findByLabelText('Select a capability');

        await act(async () => {
          await selectEvent.openMenu(capabilitySelect);
        });

        await act(async () => { await selectEvent.select(capabilitySelect, capability.title); });

        // Submit form
        const submitButton = await screen.findByRole('button', {
          name: /add collaborator/i,
          hidden: true,
        });

        fireEvent.click(submitButton);

        await waitFor(() => {
          expect(EntityShareActions.prepare).toHaveBeenCalledWith('dashboard', 'The title', mockEntityShareState.entity, {
            selected_grantee_capabilities: mockEntityShareState.selectedGranteeCapabilities.merge({ [newGrantee.id]: capability.id }),
          });
        });
      };

      // eslint-disable-next-line jest/expect-expect
      it.each`
        newGrantee  | granteeType   | capability
        ${john}     | ${'user'}     | ${viewer}
        ${everyone} | ${'everyone'} | ${manager}
        ${security} | ${'team'}     | ${owner}
      `('sends new grantee $granteeType to preparation', addGrantee);
    });

    it('shows confirmation dialog on save if a collaborator got selected, but not added', async () => {
      render(<SimpleEntityShareModal />);

      // Select a grantee
      const granteesSelect = screen.getByLabelText('Search for users and teams');

      await selectEvent.openMenu(granteesSelect);

      await selectEvent.select(granteesSelect, john.title);

      fireEvent.click(await screen.findByRole('button', { name: /update sharing/i, hidden: true }));

      await waitFor(() => {
        expect(window.confirm).toHaveBeenCalledWith(`"${john.title}" got selected but was never added as a collaborator. Do you want to continue anyway?`);
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

      const capabilitySelect = await screen.findByLabelText(`Change the capability for ${ownerTitle}`);

      await act(async () => {
        await selectEvent.openMenu(capabilitySelect);
      });

      await act(async () => { await selectEvent.select(capabilitySelect, viewer.title); });

      await waitFor(() => {
        expect(screen.queryAllByText(viewer.title)).toHaveLength(2);
      });

      await waitFor(() => {
        expect(EntityShareActions.prepare).toHaveBeenCalledWith('dashboard', 'The title', mockEntityShareState.entity, {
          selected_grantee_capabilities: mockEntityShareState.selectedGranteeCapabilities.merge({ [jane.id]: viewer.id }),
        });
      });
    });

    describe('allows deleting a grantee', () => {
      // active shares
      const janeIsOwner = ActiveShare
        .builder()
        .grant('grant-id-1')
        .grantee(jane.id)
        .capability(owner.id)
        .build();
      const securityIsManager = ActiveShare
        .builder()
        .grant('grant-id-2')
        .grantee(security.id)
        .capability(manager.id)
        .build();
      const everyoneIsViewer = ActiveShare
        .builder()
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
        asMock(EntityShareStore.getInitialState).mockReturnValue({ state: entityShareState });
      });

      const deleteGrantee = async ({ grantee }) => {
        render(<SimpleEntityShareModal />);

        const deleteButton = await screen.findByTitle(`Remove sharing for ${grantee.title}`);

        fireEvent.click(deleteButton);

        await waitFor(() => {
          expect(EntityShareActions.prepare).toHaveBeenCalledWith('dashboard', 'The title', mockEntityShareState.entity, {
            selected_grantee_capabilities: selectedGranteeCapabilities.remove(grantee.id),
          });
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
