// @flow strict
import * as React from 'react';
import { render, cleanup, fireEvent } from '@testing-library/react';

import QueryTitleEditModal from './QueryTitleEditModal';

jest.mock('views/stores/QueriesStore', () => ({ QueriesActions: {} }));
jest.mock('views/stores/ViewStore', () => ({ ViewActions: {} }));

describe('QueryTitleEditModal', () => {
  afterEach(cleanup);
  const openModal = (modalRef, currentTitle = 'CurrentTitle') => {
    if (modalRef) {
      modalRef.open(currentTitle);
    }
  };

  it('shows after triggering open action', () => {
    let modalRef;
    const { queryByText } = render(
      <QueryTitleEditModal ref={(ref) => { modalRef = ref; }}
                           onTitleChange={() => Promise.resolve()}
                           selectedQueryId="selected-query-id" />,
    );
    // Modal should not be visible initially
    expect(queryByText('Edit query title')).toBeNull();
    openModal(modalRef);
    // Modal should be visible
    expect(queryByText('Edit query title')).not.toBeNull();
  });

  it('has correct initial input value', () => {
    let modalRef;
    const { getByDisplayValue } = render(
      <QueryTitleEditModal ref={(ref) => { modalRef = ref; }}
                           onTitleChange={() => Promise.resolve()}
                           selectedQueryId="selected-query-id" />,
    );
    openModal(modalRef);
    expect(getByDisplayValue('CurrentTitle')).not.toBeNull();
  });

  it('updates query title and closes', () => {
    let modalRef;
    const onTitleChangeFn = jest.fn();
    const { getByDisplayValue, getByText } = render(
      <QueryTitleEditModal ref={(ref) => { modalRef = ref; }}
                           onTitleChange={onTitleChangeFn}
                           selectedQueryId="selected-query-id" />,
    );
    openModal(modalRef);
    const titleInput = getByDisplayValue('CurrentTitle');
    const saveButton = getByText('Save');
    fireEvent.change(titleInput, { target: { value: 'NewTitle' } });
    fireEvent.click(saveButton);
    expect(onTitleChangeFn).toHaveBeenCalledTimes(1);
    expect(onTitleChangeFn).toHaveBeenCalledWith('selected-query-id', 'NewTitle');
    // TODO: Fix this test case, right now the modal is still visible
    // // Modal should not be visible anymore
    // expect(queryByText('Edit query title')).toBeNull();
  });

  // TODO: Fix this test case
  //   it('closes on click on cancel', async () => {
  //     let modalRef;
  //     const onTitleChangeFn = jest.fn();
  //     const { getByText, queryByText } = render(
  //       <QueryTitleEditModal ref={(ref) => { modalRef = ref; }}
  //                            onTitleChange={onTitleChangeFn}
  //                            selectedQueryId="selected-query-id" />,
  //     );
  //     openModal(modalRef);
  //     // Modal should be visible
  //     expect(queryByText('Edit query title')).not.toBeNull();
  //     // Modal should not be visible after click on cancel
  //     const canselButton = getByText('Cancel');
  //     fireEvent.click(canselButton);
  //     await wait();
  //     expect(queryByText('Edit query title')).toBeNull();
  //   });
});
