// @flow stric
import React from 'react';
import { render, fireEvent, cleanup, wait } from 'wrappedTestingLibrary';

import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';

import FieldSortIcon from './FieldSortIcon';

describe('FieldSortIcon', () => {
  const currentSort = new SortConfig(SortConfig.PIVOT_TYPE, 'timestamp', Direction.Descending);
  const config = new MessagesWidgetConfig(['timestamp', 'source'], true, [], [currentSort]);

  afterEach(cleanup);

  it('should set descending sort on click, if field sort is not defined', () => {
    const onSortChangeStub = jest.fn(() => Promise.resolve());
    const { getByTestId } = render(<FieldSortIcon config={config} fieldName="source" onSortChange={onSortChangeStub} setLoadingState={() => {}} />);

    const sortIcon = getByTestId('messages-sort-icon');
    fireEvent.click(sortIcon);

    const expectedSort = [new SortConfig(SortConfig.PIVOT_TYPE, 'source', Direction.Descending)];
    expect(onSortChangeStub).toHaveBeenCalledTimes(1);
    expect(onSortChangeStub).toHaveBeenCalledWith(expectedSort);
  });

  it('should set ascending sort on click, if field sort is descending', () => {
    const onSortChangeStub = jest.fn(() => Promise.resolve());
    const { getByTestId } = render(<FieldSortIcon config={config} fieldName="timestamp" onSortChange={onSortChangeStub} setLoadingState={() => {}} />);

    const sortIcon = getByTestId('messages-sort-icon');
    fireEvent.click(sortIcon);

    const expectedSort = [new SortConfig(SortConfig.PIVOT_TYPE, 'timestamp', Direction.Ascending)];
    expect(onSortChangeStub).toHaveBeenCalledTimes(1);
    expect(onSortChangeStub).toHaveBeenCalledWith(expectedSort);
  });

  it('should set ascending sort on click, if field sort is descending', () => {
    const initialSort = new SortConfig(SortConfig.PIVOT_TYPE, 'source', Direction.Ascending);
    const initialConfig = new MessagesWidgetConfig(['timestamp', 'source'], true, [], [initialSort]);
    const onSortChangeStub = jest.fn(() => Promise.resolve());

    const { getByTestId } = render(<FieldSortIcon config={initialConfig} fieldName="source" onSortChange={onSortChangeStub} setLoadingState={() => {}} />);

    const sortIcon = getByTestId('messages-sort-icon');
    fireEvent.click(sortIcon);

    const expectedSort = [new SortConfig(SortConfig.PIVOT_TYPE, 'source', Direction.Descending)];
    expect(onSortChangeStub).toHaveBeenCalledTimes(1);
    expect(onSortChangeStub).toHaveBeenCalledWith(expectedSort);
  });

  it('should set loading state while changing sort', async () => {
    const onSortChangeStub = jest.fn(() => Promise.resolve());
    const setLoadingStateStub = jest.fn();
    const { getByTestId } = render(<FieldSortIcon config={config} fieldName="source" onSortChange={onSortChangeStub} setLoadingState={setLoadingStateStub} />);

    const sortIcon = getByTestId('messages-sort-icon');
    fireEvent.click(sortIcon);

    expect(setLoadingStateStub).toHaveBeenCalledTimes(1);
    expect(setLoadingStateStub).toHaveBeenCalledWith(true);
    await wait(() => expect(setLoadingStateStub).toHaveBeenCalledWith(false));
    expect(setLoadingStateStub).toHaveBeenCalledTimes(2);
  });
});
