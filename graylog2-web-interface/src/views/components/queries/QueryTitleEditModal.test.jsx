// @flow strict
import * as React from 'react';
import { render, cleanup, fireEvent, wait } from '@testing-library/react';

import QueryTitleEditModal from './QueryTitleEditModal';

describe('QueryTitleEditModal', () => {
  afterEach(cleanup);
  const modalHeadline = 'Editing query title';
  const openModal = (modalRef, currentTitle = 'CurrentTitle') => {
    if (modalRef) {
      modalRef.open(currentTitle);
    }
  };

  it('shows after triggering open action', () => {
    let modalRef;
    const { queryByText } = render(
      <QueryTitleEditModal ref={(ref) => { modalRef = ref; }}
                           onTitleChange={() => Promise.resolve()} />,
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
                           onTitleChange={() => Promise.resolve()} />,
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
    await wait(() => {
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
    await wait(() => {
      expect(queryByText(modalHeadline)).toBeNull();
    });
  });
});
