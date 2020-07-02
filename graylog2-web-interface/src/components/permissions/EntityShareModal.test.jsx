// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { cleanup, render, fireEvent, wait } from 'wrappedTestingLibrary';
import { act } from 'react-dom/test-utils';
import asMock from 'helpers/mocking/AsMock';
import mockEntityShareState, { john, jane, everyone, security, viewer, owner, manager } from 'fixtures/entityShareState';
import selectEvent from 'react-select-event';

import ActiveShare from 'logic/permissions/ActiveShare';
import { EntityShareStore, EntityShareActions } from 'stores/permissions/EntityShareStore';

import EntityShareModal from './EntityShareModal';

const mockEmptyStore = { state: undefined };

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

describe('EntityShareModal', () => {
  beforeAll(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.clearAllMocks();

    cleanup();
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

    expect(EntityShareActions.prepare).toBeCalledWith(mockEntityShareState.entity);
  });

  it('updates entity share state on submit', async () => {
    const { getByText } = render(<SimpleEntityShareModal />);

    const submitButton = getByText('Save');

    fireEvent.click(submitButton);

    await wait(() => {
      expect(EntityShareActions.update).toBeCalledTimes(1);

      expect(EntityShareActions.update).toBeCalledWith(mockEntityShareState.entity, { grantee_roles: mockEntityShareState.selectedGranteeRoles });
    });
  });

  it('closes modal on cancel', async () => {
    const onClose = jest.fn();
    const { getByText } = render(<SimpleEntityShareModal onClose={onClose} />);

    const cancelButton = getByText('Cancel');

    fireEvent.click(cancelButton);

    await wait(() => {
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

    it('provided description', () => {
      const { getByText } = render(<SimpleEntityShareModal />);

      expect(getByText('The description')).not.toBeNull();
    });

    it('provided title', () => {
      const { getByText } = render(<SimpleEntityShareModal />);

      expect(getByText('The title')).not.toBeNull();
    });

    it('shareable url', () => {
      const { getByDisplayValue } = render(<SimpleEntityShareModal />);

      expect(getByDisplayValue('http://localhost/')).not.toBeNull();
    });

    it('missing dependecies warning', () => {
      const { getByText } = render(<SimpleEntityShareModal />);

      expect(getByText('There are missing dependecies for the current set of collaborators')).not.toBeNull();
    });
  });

  describe('grantee selector', () => {
    describe('adds new selected grantee', () => {
      const addGrantee = async ({ newGrantee, role }) => {
        const { getByText, getByLabelText } = render(<SimpleEntityShareModal />);

        // Select a grantee
        const granteesSelect = getByLabelText('Search for users and teams');

        await selectEvent.openMenu(granteesSelect);

        await selectEvent.select(granteesSelect, newGrantee.title);

        // Select a role
        const roleSelect = getByLabelText('Select a role');

        await selectEvent.openMenu(roleSelect);

        await act(async () => { await selectEvent.select(roleSelect, role.title); });

        // Submit form
        const submitButton = getByText('Add Collaborator');

        fireEvent.click(submitButton);

        await wait(() => {
          expect(EntityShareActions.prepare).toBeCalledTimes(2);

          expect(EntityShareActions.prepare).toBeCalledWith(mockEntityShareState.entity, {
            selected_grantee_roles: mockEntityShareState.selectedGranteeRoles.merge({ [newGrantee.id]: role.id }),
          });
        });
      };

      it.each`
        newGrantee  | granteeType   | role
        ${john}     | ${'user'}     | ${viewer}
        ${everyone} | ${'everyone'} | ${manager}
        ${security} | ${'team'}     | ${owner}
      `('sends new grantee $granteeType to preparation', addGrantee);
    });

    it('shows validation error', async () => {
      const { getByText } = render(<SimpleEntityShareModal />);

      const submitButton = getByText('Add Collaborator');

      fireEvent.click(submitButton);

      await wait(() => expect(getByText('The grantee is required.')).not.toBeNull());
    });
  });

  describe('selected grantees list', () => {
    it('lists grantees', () => {
      const ownerTitle = jane.title;
      const { getByText } = render(<SimpleEntityShareModal />);

      expect(getByText(ownerTitle)).not.toBeNull();
    });

    it('allows updating the role of a grantee', async () => {
      const ownerTitle = jane.title;
      const { getByLabelText } = render(<SimpleEntityShareModal />);

      const roleSelect = getByLabelText(`Change the role for ${ownerTitle}`);

      await selectEvent.openMenu(roleSelect);

      await act(async () => { await selectEvent.select(roleSelect, viewer.title); });

      await wait(() => {
        expect(EntityShareActions.prepare).toHaveBeenCalledTimes(2);

        expect(EntityShareActions.prepare).toHaveBeenCalledWith(mockEntityShareState.entity, {
          selected_grantee_roles: mockEntityShareState.selectedGranteeRoles.merge({ [jane.id]: viewer.id }),
        });
      });
    });

    describe('allows deleting a grantee', () => {
      // active shares
      const janeIsOwner = ActiveShare
        .builder()
        .grant('grant-id-1')
        .grantee(jane.id)
        .role(owner.id)
        .build();
      const securityIsManager = ActiveShare
        .builder()
        .grant('grant-id-2')
        .grantee(security.id)
        .role(manager.id)
        .build();
      const everyoneIsViewer = ActiveShare
        .builder()
        .grant('grant-id-3')
        .grantee(everyone.id)
        .role(viewer.id)
        .build();
      const activeShares = Immutable.List([janeIsOwner, securityIsManager, everyoneIsViewer]);
      const selectedGranteeRoles = Immutable.Map({
        [janeIsOwner.grantee]: janeIsOwner.role,
        [securityIsManager.grantee]: securityIsManager.role,
        [everyoneIsViewer.grantee]: everyoneIsViewer.role,
      });
      const enitityShareState = mockEntityShareState
        .toBuilder()
        .activeShares(activeShares)
        .selectedGranteeRoles(selectedGranteeRoles)
        .build();

      beforeEach(() => {
        asMock(EntityShareStore.getInitialState).mockReturnValueOnce({ state: enitityShareState });
      });

      const deleteGrantee = async ({ grantee }) => {
        const { getByTitle } = render(<SimpleEntityShareModal />);

        const deleteButton = getByTitle(`Delete sharing for ${grantee.title}`);

        fireEvent.click(deleteButton);

        await wait(() => {
          expect(EntityShareActions.prepare).toHaveBeenCalledTimes(2);

          expect(EntityShareActions.prepare).toHaveBeenCalledWith(mockEntityShareState.entity, {
            selected_grantee_roles: selectedGranteeRoles.remove(grantee.id),
          });
        });
      };

      it.each`
        grantee     | granteeType   | role
        ${jane}     | ${'user'}     | ${viewer}
        ${security} | ${'team'}     | ${manager}
        ${everyone} | ${'everyone'} | ${owner}
      `('sends deleted grantee $granteeType to preparation', deleteGrantee);
    });
  });
});
