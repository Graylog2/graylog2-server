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

import { asMock } from 'helpers/mocking';
import useSaveViewFormControls from 'views/hooks/useSaveViewFormControls';

import OriginalSavedSearchForm from './SavedSearchForm';

jest.mock('views/hooks/useSaveViewFormControls');

const SavedSearchForm = (props: React.ComponentProps<typeof OriginalSavedSearchForm>) => (
  <OriginalSavedSearchForm {...props}>
    <button type="button">Submit</button>
  </OriginalSavedSearchForm>
);

describe('SavedSearchForm', () => {
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
  });

  describe('render the SavedSearchForm', () => {
    it('should render create new', async () => {
      render(<SavedSearchForm {...props}
                              isCreateNew />);

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

      render(<SavedSearchForm {...props}
                              toggleModal={onToggleModal} />);

      const cancelButton = await screen.findByRole('button', { name: /cancel/i });
      userEvent.click(cancelButton);

      expect(onToggleModal).toHaveBeenCalledTimes(1);
    });

    it('should handle saveSearch', async () => {
      const onSave = jest.fn();

      render(<SavedSearchForm {...props}
                              saveSearch={onSave} />);

      const saveButton = await screen.findByRole('button', { name: /Save search/i });
      userEvent.click(saveButton);

      expect(onSave).toHaveBeenCalledTimes(1);
    });

    it('should handle saveAsSearch', async () => {
      const onSaveAs = jest.fn();

      render(<SavedSearchForm {...props}
                              saveAsSearch={onSaveAs} />);

      userEvent.type(await findTitleInput(), ' and further title');
      const saveAsButton = await screen.findByRole('button', { name: /Save as/i });
      userEvent.click(saveAsButton);

      expect(onSaveAs).toHaveBeenCalledWith('new Title and further title');
    });

    it('should not handle saveAsSearch if disabled', async () => {
      const onSaveAs = jest.fn();

      render(<SavedSearchForm {...props}
                              saveAsSearch={onSaveAs} />);

      const saveAsButton = await screen.findByRole('button', { name: /Save as/i });
      userEvent.click(saveAsButton);

      expect(onSaveAs).not.toHaveBeenCalled();
    });

    it('should handle create new', async () => {
      const onSaveAs = jest.fn();

      render(<SavedSearchForm {...props}
                              saveAsSearch={onSaveAs}
                              isCreateNew />);

      userEvent.type(await findTitleInput(), ' and further title');
      const createNewButton = await screen.findByRole('button', { name: /create new/i });
      userEvent.click(createNewButton);

      expect(onSaveAs).toHaveBeenCalledWith('new Title and further title');
    });
  });

  it('should render pluggable components', async () => {
    asMock(useSaveViewFormControls).mockReturnValue([{ component: () => <div>Pluggable component!</div>, id: 'example-plugin-component' }]);

    render(<SavedSearchForm {...props} />);

    await screen.findByText('Pluggable component!');
  });
});
