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
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';
import { act } from 'react-dom/test-utils';
import asMock from 'helpers/mocking/AsMock';
import mockEntityShareState, { failedEntityShareState, john, jane, everyone, security, viewer, owner, manager } from 'fixtures/entityShareState';
import selectEvent from 'react-select-event';

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
    getInitialState: jest.fn(() => ({ state: mockEntityShareState })),
  },
}));

jest.setTimeout(10000);

describe('EntityShareModal', () => {
  beforeAll(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  const SimpleEntityShareModal = ({ ...props }) => {
    return (
      <EntityShareModal description="The description"
                        entityId="dashboard-id"
                        entityType="dashboard"
                        onClose={() => {}}
                        entityTitle="The title"
                        {...props} />
    );
  };

  it('fetches entity share state initially', () => {
    render(<SimpleEntityShareModal />);

    expect(EntityShareActions.prepare).toBeCalledTimes(1);

    expect(EntityShareActions.prepare).toBeCalledWith('dashboard', 'The title', mockEntityShareState.entity);
  });

  it('updates entity share state on submit', async () => {
    const { getByText } = render(<SimpleEntityShareModal />);

    const submitButton = getByText('Save');

    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(EntityShareActions.update).toBeCalledTimes(1);

      expect(EntityShareActions.update).toBeCalledWith('dashboard', 'The title', mockEntityShareState.entity, {
        selected_grantee_capabilities: mockEntityShareState.selectedGranteeCapabilities,
      });
    });
  });

  it('closes modal on cancel', async () => {
    const onClose = jest.fn();
    const { getByText } = render(<SimpleEntityShareModal onClose={onClose} />);

    const cancelButton = getByText('Discard changes');

    fireEvent.click(cancelButton);

    await waitFor(() => {
      expect(onClose).toHaveBeenCalledTimes(1);
    });
  });

  describe('displays', () => {
    it('loading spinner while loading entity share state', () => {
      asMock(EntityShareStore.getInitialState).mockReturnValueOnce(mockEmptyStore);
      const { getByText } = render(<SimpleEntityShareModal />);

      act(() => jest.advanceTimersByTime(200));

      expect(getByText('Loading...')).not.toBeNull();
    });

    it('displays an error if validation failed and disables submit', () => {
      asMock(EntityShareStore.getInitialState).mockReturnValueOnce(mockFailedStore);
      const { getByText } = render(<SimpleEntityShareModal />);

      expect(getByText('Removing the following owners will leave the entity ownerless:')).not.toBeNull();
      expect(getByText('Save')).toBeDisabled();
    });

    it('necessary information', () => {
      const { getByText, getByDisplayValue } = render(<SimpleEntityShareModal />);

      // provided description
      expect(getByText('The description')).not.toBeNull();
      // Provided title
      expect(getByText('The title')).not.toBeNull();
      // sharable urls
      expect(getByDisplayValue('http://localhost/dashboards/dashboard-id')).not.toBeNull();
      // missing dependencies warning
      expect(getByText('There are missing dependencies for the current set of collaborators')).not.toBeNull();
      expect(getByText(/needs access to/)).not.toBeNull();
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
        const { getByText, getByLabelText } = render(<SimpleEntityShareModal />);

        // Select a grantee
        const granteesSelect = getByLabelText('Search for users and teams');

        await selectEvent.openMenu(granteesSelect);

        await selectEvent.select(granteesSelect, newGrantee.title);

        // Select a capability
        const capabilitySelect = getByLabelText('Select a capability');

        await selectEvent.openMenu(capabilitySelect);

        await act(async () => { await selectEvent.select(capabilitySelect, capability.title); });

        // Submit form
        const submitButton = getByText('Add Collaborator');

        fireEvent.click(submitButton);

        await waitFor(() => expect(EntityShareActions.prepare).toHaveBeenCalledTimes(1));

        expect(EntityShareActions.prepare).toBeCalledWith('dashboard', 'The title', mockEntityShareState.entity, {
          selected_grantee_capabilities: mockEntityShareState.selectedGranteeCapabilities.merge({ [newGrantee.id]: capability.id }),
        });
      };

      it.each`
        newGrantee  | granteeType   | capability
        ${john}     | ${'user'}     | ${viewer}
        ${everyone} | ${'everyone'} | ${manager}
        ${security} | ${'team'}     | ${owner}
      `('sends new grantee $granteeType to preparation', addGrantee);
    });

    it('shows confirmation dialog on save if a collaborator got selected, but not added', async () => {
      const { getByText, getByLabelText } = render(<SimpleEntityShareModal />);

      // Select a grantee
      const granteesSelect = getByLabelText('Search for users and teams');

      await selectEvent.openMenu(granteesSelect);

      await selectEvent.select(granteesSelect, john.title);

      const submitButton = getByText('Save');

      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(window.confirm).toHaveBeenCalledTimes(1);
        expect(window.confirm).toHaveBeenCalledWith(`"${john.title}" got selected but was never added as a collaborator. Do you want to continue anyway?`);
      });
    });
  });

  describe('selected grantees list', () => {
    it('lists grantees', () => {
      const ownerTitle = jane.title;
      const { getByText } = render(<SimpleEntityShareModal />);

      expect(getByText(ownerTitle)).not.toBeNull();
    });

    it('allows updating the capability of a grantee', async () => {
      const ownerTitle = jane.title;
      const { getByLabelText } = render(<SimpleEntityShareModal />);

      const capabilitySelect = getByLabelText(`Change the capability for ${ownerTitle}`);

      await selectEvent.openMenu(capabilitySelect);

      await act(async () => { await selectEvent.select(capabilitySelect, viewer.title); });

      await waitFor(() => {
        expect(EntityShareActions.prepare).toHaveBeenCalledTimes(2);

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
        asMock(EntityShareStore.getInitialState).mockReturnValueOnce({ state: entityShareState });
      });

      const deleteGrantee = async ({ grantee }) => {
        const { getByTitle } = render(<SimpleEntityShareModal />);

        const deleteButton = getByTitle(`Remove sharing for ${grantee.title}`);

        fireEvent.click(deleteButton);

        await waitFor(() => {
          expect(EntityShareActions.prepare).toHaveBeenCalledTimes(2);

          expect(EntityShareActions.prepare).toHaveBeenCalledWith('dashboard', 'The title', mockEntityShareState.entity, {
            selected_grantee_capabilities: selectedGranteeCapabilities.remove(grantee.id),
          });
        });
      };

      it.each`
        grantee     | granteeType   | capability
        ${jane}     | ${'user'}     | ${viewer}
        ${security} | ${'team'}     | ${manager}
        ${everyone} | ${'everyone'} | ${owner}
      `('sends deleted grantee $granteeType to preparation', deleteGrantee);
    });
  });
});
