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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { useFormikContext } from 'formik';

import selectEvent from 'helpers/selectEvent';
import { asMock } from 'helpers/mocking';
import useSaveViewFormControls from 'views/hooks/useSaveViewFormControls';
import { createEntityShareState, everyone, viewer } from 'fixtures/entityShareState';
import { EntityShareStore } from 'stores/permissions/EntityShareStore';

import OriginalSavedSearchForm from './SavedSearchForm';

jest.mock('formik', () => ({
  ...jest.requireActual('formik'),
  useFormikContext: jest.fn(),
}));
jest.mock('views/hooks/useSaveViewFormControls');
jest.mock('stores/permissions/EntityShareStore', () => ({
  __esModule: true,
  EntityShareActions: {
    prepare: jest.fn(() => Promise.resolve()),
    update: jest.fn(() => Promise.resolve()),
  },
  EntityShareStore: {
    listen: jest.fn(),
    getInitialState: jest.fn(),
  },
}));
const shareWithCollaborator = async () => {
  await selectEvent.chooseOption('Search for users and teams', everyone.title);
  await selectEvent.chooseOption('Select a capability', viewer.title);

  const addCollaborator = await screen.findByRole('button', {
    name: /add collaborator/i,
  });

  await setupUser().click(addCollaborator);

  await screen.findByText(/everyone/i);
};

const SavedSearchForm = ({ ...props }: React.ComponentProps<typeof OriginalSavedSearchForm>) => (
  <OriginalSavedSearchForm {...props}>
    <button type="button">Submit</button>
  </OriginalSavedSearchForm>
);
jest.setTimeout(10000);

const mockFormDirtyState = (dirty: boolean) =>
  asMock(useFormikContext)
    // @ts-expect-error context return type is not complete
    .mockReturnValue({ dirty });

const setupUser = () => userEvent.setup({ advanceTimers: jest.advanceTimersByTime });

describe('SavedSearchForm', () => {
  beforeEach(() => {
    asMock(EntityShareStore.getInitialState).mockReturnValue({ state: createEntityShareState });
  });

  beforeAll(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  const props = {
    show: true,
    value: 'new Title',
    saveAsSearch: () => {},
    disableCreateNew: false,
    toggleModal: () => {},
    isCreateNew: false,
    target: undefined,
    saveSearch: () => {},
  };
  const findByHeadline = () => screen.findByRole('heading', { name: /name of search/i });

  beforeEach(() => {
    asMock(useSaveViewFormControls).mockReturnValue([]);
    mockFormDirtyState(false);
  });

  describe('render the SavedSearchForm', () => {
    it('should render create new', async () => {
      render(<SavedSearchForm {...props} isCreateNew />);

      await findByHeadline();
    });

    it('should render save', async () => {
      render(<SavedSearchForm {...props} />);

      await findByHeadline();
    });

    it('should render disabled create new', async () => {
      render(<SavedSearchForm {...props} />);

      await findByHeadline();
    });
  });

  describe('callbacks', () => {
    const findTitleInput = () => screen.getByRole('textbox', { name: /title/i });

    it('should handle toggleModal', async () => {
      const onToggleModal = jest.fn();

      render(<SavedSearchForm {...props} toggleModal={onToggleModal} />);

      const cancelButton = await screen.findByRole('button', { name: /cancel/i });
      await setupUser().click(cancelButton);

      expect(onToggleModal).toHaveBeenCalledTimes(1);
    });

    it('should handle saveSearch', async () => {
      const onSave = jest.fn();

      render(<SavedSearchForm {...props} saveSearch={onSave} />);

      const saveButton = await screen.findByRole('button', { name: /Save search/i });
      await setupUser().click(saveButton);

      expect(onSave).toHaveBeenCalledTimes(1);
    });

    it('should handle saveAsSearch', async () => {
      const onSaveAs = jest.fn();

      render(<SavedSearchForm {...props} saveAsSearch={onSaveAs} />);

      await setupUser().type(await findTitleInput(), ' and further title');
      const saveAsButton = await screen.findByRole('button', { name: /Save as/i });
      await setupUser().click(saveAsButton);

      expect(onSaveAs).toHaveBeenCalledWith('new Title and further title', null);
    });

    it('should not handle saveAsSearch if disabled', async () => {
      const onSaveAs = jest.fn();

      render(<SavedSearchForm {...props} saveAsSearch={onSaveAs} />);

      const saveAsButton = await screen.findByRole('button', { name: /Save as/i });
      await setupUser().click(saveAsButton);

      expect(onSaveAs).not.toHaveBeenCalled();
    });

    it('should handle create new', async () => {
      const onSaveAs = jest.fn();

      render(<SavedSearchForm {...props} saveAsSearch={onSaveAs} isCreateNew />);

      await setupUser().type(await findTitleInput(), ' and further title');
      const createNewButton = await screen.findByRole('button', { name: /create new/i });
      await setupUser().click(createNewButton);

      expect(onSaveAs).toHaveBeenCalledWith('new Title and further title', null);
    });

    it('should handle saveSearch with share settings', async () => {
      const onSaveAs = jest.fn();
      render(<SavedSearchForm {...props} saveAsSearch={onSaveAs} isCreateNew />);
      await setupUser().type(await findTitleInput(), ' and further title');
      const createNewButton = await screen.findByRole('button', { name: /create new/i });

      await shareWithCollaborator();

      await setupUser().click(createNewButton);

      expect(onSaveAs).toHaveBeenCalledTimes(1);
    });
  });

  describe('unconfirmed changes warning', () => {
    it('should show warning when form has unconfirmed changes', async () => {
      mockFormDirtyState(true);

      render(<SavedSearchForm {...props} />);

      await screen.findByText(/unconfirmed changes to the search parameters/i);
    });

    it('should not show warning when form has no unconfirmed changes', async () => {
      mockFormDirtyState(false);

      render(<SavedSearchForm {...props} />);

      await findByHeadline();

      expect(screen.queryByText(/unconfirmed changes to the search parameters/i)).not.toBeInTheDocument();
    });
  });

  it('should render pluggable components', async () => {
    asMock(useSaveViewFormControls).mockReturnValue([
      { component: () => <div>Pluggable component!</div>, id: 'example-plugin-component' },
    ]);

    render(<SavedSearchForm {...props} />);

    await screen.findByText('Pluggable component!');
  });
});
