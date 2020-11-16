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
// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';

import QueryTitleEditModal from './QueryTitleEditModal';

describe('QueryTitleEditModal', () => {
  const modalHeadline = 'Editing dashboard page title';

  const openModal = (modalRef, currentTitle = 'CurrentTitle') => {
    if (modalRef) {
      modalRef.open(currentTitle);
    }
  };

  it('shows after triggering open action', () => {
    let modalRef;
    const { queryByText } = render(
      <QueryTitleEditModal ref={(ref) => { modalRef = ref; }}
                           onTitleChange={() => Promise.resolve(Immutable.Map())} />,
    );

    // Modal should not be visible initially
    expect(queryByText(modalHeadline)).toBeNull();

    openModal(modalRef);

    // Modal should be visible
    expect(queryByText(modalHeadline)).not.toBeNull();
  });

  it('has correct initial input value', () => {
    let modalRef;
    const { getByDisplayValue } = render(
      <QueryTitleEditModal ref={(ref) => { modalRef = ref; }}
                           onTitleChange={() => Promise.resolve(Immutable.Map())} />,
    );

    openModal(modalRef);

    expect(getByDisplayValue('CurrentTitle')).not.toBeNull();
  });

  it('updates query title and closes', async () => {
    let modalRef;
    const onTitleChangeFn = jest.fn();
    const { getByDisplayValue, getByText, queryByText } = render(
      <QueryTitleEditModal ref={(ref) => { modalRef = ref; }}
                           onTitleChange={onTitleChangeFn} />,
    );

    openModal(modalRef);
    const titleInput = getByDisplayValue('CurrentTitle');
    const saveButton = getByText('Save');

    fireEvent.change(titleInput, { target: { value: 'NewTitle' } });
    fireEvent.click(saveButton);

    expect(onTitleChangeFn).toHaveBeenCalledTimes(1);
    expect(onTitleChangeFn).toHaveBeenCalledWith('NewTitle');

    // Modal should not be visible anymore
    await waitFor(() => {
      expect(queryByText(modalHeadline)).toBeNull();
    });
  });

  it('closes on click on cancel', async () => {
    let modalRef;
    const onTitleChangeFn = jest.fn();
    const { getByText, queryByText } = render(
      <QueryTitleEditModal ref={(ref) => { modalRef = ref; }}
                           onTitleChange={onTitleChangeFn} />,
    );

    openModal(modalRef);

    // Modal should be visible
    expect(queryByText(modalHeadline)).not.toBeNull();

    // Modal should not be visible after click on cancel
    const cancelButton = getByText('Cancel');

    fireEvent.click(cancelButton);

    await waitFor(() => {
      expect(queryByText(modalHeadline)).toBeNull();
    });
  });
});
