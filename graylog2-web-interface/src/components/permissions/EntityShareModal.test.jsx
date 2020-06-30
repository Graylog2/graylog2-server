// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { cleanup, render, fireEvent, wait } from 'wrappedTestingLibrary';
import { act } from 'react-dom/test-utils';
import asMock from 'helpers/mocking/AsMock';
import mockEntityShareState, { john, jane, viewer } from 'fixtures/entityShareState';
import selectEvent from 'react-select-event';

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
                        title="The title"
                        {...props} />
    );
  };

  it('fetches entity share state initially', () => {
    render(<SimpleEntityShareModal />);

    expect(EntityShareActions.prepare).toBeCalledTimes(1);

    expect(EntityShareActions.prepare).toBeCalledWith(mockEntityShareState.entity);
  });

  it('updates entiy share state on submit', async () => {
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
      expect(onClose).toHaveBeenCalled();
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

    it('sharable url', () => {
      const { getByDisplayValue } = render(<SimpleEntityShareModal />);

      expect(getByDisplayValue('http://localhost/')).not.toBeNull();
    });
  });

  describe('grantee selector', () => {
    it('adds new selected grantee', async () => {
      const newSelectedGrantee = john;
      const initialRole = viewer;
      const { getByText, getByLabelText } = render(<SimpleEntityShareModal />);

      const granteesSelect = getByLabelText('Search for users and teams');

      await selectEvent.openMenu(granteesSelect);

      await selectEvent.select(granteesSelect, newSelectedGrantee.title);

      const submitButton = getByText('Add Collaborator');

      fireEvent.click(submitButton);

      await wait(() => {
        expect(EntityShareActions.prepare).toBeCalledTimes(2);

        expect(EntityShareActions.prepare).toBeCalledWith(mockEntityShareState.entity, {
          selected_grantee_roles: mockEntityShareState.selectedGranteeRoles.merge({ [newSelectedGrantee.id]: initialRole.id }),
        });
      });
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

      await selectEvent.select(roleSelect, viewer.title);

      await wait(() => {
        expect(EntityShareActions.prepare).toHaveBeenCalledTimes(2);

        expect(EntityShareActions.prepare).toHaveBeenCalledWith(mockEntityShareState.entity, {
          selected_grantee_roles: mockEntityShareState.selectedGranteeRoles.merge({ [jane.id]: viewer.id }),
        });
      });
    });

    it('allows deleting a grantee', async () => {
      const ownerTitle = jane.title;
      const { getByTitle } = render(<SimpleEntityShareModal />);

      const deleteButton = getByTitle(`Delete sharing for ${ownerTitle}`);

      fireEvent.click(deleteButton);

      await wait(() => {
        expect(EntityShareActions.prepare).toHaveBeenCalledTimes(2);

        expect(EntityShareActions.prepare).toHaveBeenCalledWith(mockEntityShareState.entity, {
          selected_grantee_roles: Immutable.Map(),
        });
      });
    });
  });
});
