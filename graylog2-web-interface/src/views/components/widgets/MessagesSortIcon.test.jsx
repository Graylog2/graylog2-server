// @flow stric
import React from 'react';
import { render, fireEvent, cleanup, wait } from 'wrappedTestingLibrary';

import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';

import MessagesSortIcon from './MessagesSortIcon';

describe('MessagesSorticon', () => {
  const currentSort = new SortConfig(SortConfig.PIVOT_TYPE, 'timestamp', Direction.Descending);
  const config = new MessagesWidgetConfig(['timestamp', 'source'], true, [], [currentSort]);

  afterEach(cleanup);

  it('should set descending sort on click, if field sort is not defined', () => {
    const onChangeStub = jest.fn(() => Promise.resolve());
    const { getByTestId } = render(<MessagesSortIcon config={config} fieldName="source" onConfigChange={onChangeStub} setLoadingState={() => {}} />);
    const sortIcon = getByTestId('messages-sort-icon');

    fireEvent.click(sortIcon);

    const expectedSort = new SortConfig(SortConfig.PIVOT_TYPE, 'source', Direction.Descending);
    const expectedConfig = config.toBuilder().sort([expectedSort]).build();
    expect(onChangeStub).toHaveBeenCalledTimes(1);
    expect(onChangeStub).toHaveBeenCalledWith(expectedConfig);
  });

  it('should set ascending sort on click, if field sort is descending', () => {
    const onChangeStub = jest.fn(() => Promise.resolve());
    const { getByTestId } = render(<MessagesSortIcon config={config} fieldName="timestamp" onConfigChange={onChangeStub} setLoadingState={() => {}} />);
    const sortIcon = getByTestId('messages-sort-icon');

    fireEvent.click(sortIcon);

    const expectedSort = new SortConfig(SortConfig.PIVOT_TYPE, 'timestamp', Direction.Ascending);
    const expectedConfig = config.toBuilder().sort([expectedSort]).build();
    expect(onChangeStub).toHaveBeenCalledTimes(1);
    expect(onChangeStub).toHaveBeenCalledWith(expectedConfig);
  });

  it('should set ascending sort on click, if field sort is descending', () => {
    const initialSort = new SortConfig(SortConfig.PIVOT_TYPE, 'source', Direction.Ascending);
    const initialConfig = new MessagesWidgetConfig(['timestamp', 'source'], true, [], [initialSort]);
    const onChangeStub = jest.fn(() => Promise.resolve());

    const { getByTestId } = render(<MessagesSortIcon config={initialConfig} fieldName="source" onConfigChange={onChangeStub} setLoadingState={() => {}} />);
    const sortIcon = getByTestId('messages-sort-icon');

    fireEvent.click(sortIcon);

    const expectedSort = new SortConfig(SortConfig.PIVOT_TYPE, 'source', Direction.Descending);
    const expectedConfig = config.toBuilder().sort([expectedSort]).build();
    expect(onChangeStub).toHaveBeenCalledTimes(1);
    expect(onChangeStub).toHaveBeenCalledWith(expectedConfig);
  });

  it('should set loading state while changing sort', async () => {
    const onChangeStub = jest.fn(() => Promise.resolve());
    const setLoadingStateStub = jest.fn();
    const { getByTestId } = render(<MessagesSortIcon config={config} fieldName="source" onConfigChange={onChangeStub} setLoadingState={setLoadingStateStub} />);
    const sortIcon = getByTestId('messages-sort-icon');

    fireEvent.click(sortIcon);

    expect(setLoadingStateStub).toHaveBeenCalledTimes(1);
    expect(setLoadingStateStub).toHaveBeenCalledWith(true);
    await wait(() => expect(setLoadingStateStub).toHaveBeenCalledWith(false));
    expect(setLoadingStateStub).toHaveBeenCalledTimes(2);
  });
});
