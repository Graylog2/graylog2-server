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
import { render, screen, fireEvent } from 'wrappedTestingLibrary';

import { mappedDataResult, modalDataResult } from 'fixtures/createEventDefinitionFromValue';
import CreateEventDefinitionModal from 'views/logic/valueactions/createEventDefinition/CreateEventDefinitionModal';
import asMock from 'helpers/mocking/AsMock';
import useModalReducer from 'views/logic/valueactions/createEventDefinition/hooks/useModalReducer';

jest.mock('views/logic/valueactions/createEventDefinition/hooks/useModalReducer', () => jest.fn());

const checked = {
  aggCondition: true,
  columnGroupBy: true,
  lutParameters: true,
  queryWithReplacedParams: true,
  rowGroupBy: true,
  searchFilterQuery: true,
  searchFromValue: true,
  searchWithinMs: true,
  streams: true,
};
const onClose = jest.fn();
const renderCreateDefinitionModal = ({
  modalData = modalDataResult,
  mappedData = mappedDataResult,
}) => render(
  <CreateEventDefinitionModal onClose={onClose} modalData={modalData} mappedData={mappedData} show />,
);

const renderWithAllChecked = () => renderCreateDefinitionModal({
  mappedData: {
    ...mappedDataResult,
    rowValuePath: 'action:login',
    columnValuePath: 'action:login',
  },
});
const mockedDispatch = jest.fn();

describe('CreateEventDefinitionModal', () => {
  it('shows strategies and show details button', async () => {
    asMock(useModalReducer).mockReturnValue([
      {
        strategy: 'EXACT',
        checked,
        showDetails: false,
      },
      mockedDispatch,
    ]);

    renderCreateDefinitionModal({
      mappedData: {
        ...mappedDataResult,
        rowValuePath: 'action:login',
        columnValuePath: 'action:login',
      },
    });

    await screen.findByText('All searches');
    await screen.findByText('Row pivots');
    await screen.findByText('Column pivots');
    await screen.findByText('Widget');
    await screen.findByText('Custom');
    await screen.findByText('Show strategy details');

    await expect(screen.queryByText('count(action): 400')).not.toBeInTheDocument();
  });

  it('shows hide details button and all values', async () => {
    asMock(useModalReducer).mockReturnValue([
      {
        strategy: 'EXACT',
        checked,
        showDetails: true,
      },
      mockedDispatch,
    ]);

    renderCreateDefinitionModal({});

    await screen.findByText('Hide strategy details');
    await screen.findByText('count(action): 400');
  });

  it('doesnt show Row pivots and Column pivots when no rowValuePath and columnValuePath', async () => {
    asMock(useModalReducer).mockReturnValue([
      {
        strategy: 'EXACT',
        checked,
        showDetails: false,
      },
      mockedDispatch,
    ]);

    renderCreateDefinitionModal({});

    await screen.findByText('All searches');
    await screen.findByText('Widget');
    await screen.findByText('Custom');
    await screen.findByText('Show strategy details');

    expect(screen.queryByText('Row pivots')).not.toBeInTheDocument();
    expect(screen.queryByText('Column pivots')).not.toBeInTheDocument();
  });

  describe('run dispatch for', () => {
    it('All searches', async () => {
      asMock(useModalReducer).mockReturnValue([
        {
          strategy: 'ALL',
          checked,
          showDetails: true,
        },
        mockedDispatch,
      ]);

      renderWithAllChecked();

      const allButton = await screen.findByText('All searches');
      await fireEvent.click(allButton);

      await expect(mockedDispatch).toHaveBeenCalledWith({
        type: 'SET_EXACT_STRATEGY',
      });
    });

    it('Row pivots', async () => {
      asMock(useModalReducer).mockReturnValue([
        {
          strategy: 'EXACT',
          checked,
          showDetails: true,
        },
        mockedDispatch,
      ]);

      renderWithAllChecked();

      const rowButton = await screen.findByText('Row pivots');
      await fireEvent.click(rowButton);

      await expect(mockedDispatch).toHaveBeenCalledWith({
        type: 'SET_ROW_STRATEGY',
      });
    });

    it('Column pivots', async () => {
      asMock(useModalReducer).mockReturnValue([
        {
          strategy: 'EXACT',
          checked,
          showDetails: true,
        },
        mockedDispatch,
      ]);

      renderWithAllChecked();

      const colButton = await screen.findByText('Column pivots');
      await fireEvent.click(colButton);

      await expect(mockedDispatch).toHaveBeenCalledWith({
        type: 'SET_COL_STRATEGY',
      });
    });

    it('Widget', async () => {
      asMock(useModalReducer).mockReturnValue([
        {
          strategy: 'EXACT',
          checked,
          showDetails: true,
        },
        mockedDispatch,
      ]);

      renderWithAllChecked();

      const widgetButton = await screen.findByText('Widget');
      await fireEvent.click(widgetButton);

      await expect(mockedDispatch).toHaveBeenCalledWith({
        type: 'SET_ALL_STRATEGY',
      });
    });

    it('Custom', async () => {
      asMock(useModalReducer).mockReturnValue([
        {
          strategy: 'EXACT',
          checked,
          showDetails: true,
        },
        mockedDispatch,
      ]);

      renderWithAllChecked();

      const customButton = await screen.findByText('Custom');
      await fireEvent.click(customButton);

      await expect(mockedDispatch).toHaveBeenCalledWith({
        type: 'SET_CUSTOM_STRATEGY',
      });
    });

    it('checkbox', async () => {
      asMock(useModalReducer).mockReturnValue([
        {
          strategy: 'EXACT',
          checked,
          showDetails: true,
        },
        mockedDispatch,
      ]);

      renderWithAllChecked();

      const checkbox = await screen.findByText('count(action): 400');
      await fireEvent.click(checkbox);

      await expect(mockedDispatch).toHaveBeenCalledWith({
        type: 'UPDATE_CHECKED_ITEMS',
        payload: {
          aggCondition: false,
          columnGroupBy: true,
          rowGroupBy: true,
        },
      });
    });
  });

  it('has correct link', async () => {
    asMock(useModalReducer).mockReturnValue([
      {
        strategy: 'EXACT',
        checked,
        showDetails: true,
      },
      mockedDispatch,
    ]);

    renderWithAllChecked();

    const link = await screen.findByText('Continue configuration');

    expect(link).toHaveAttribute('href', '/alerts/definitions/new?step=condition&config={"type":"aggregation-v1","query":" (http_method:GET) AND ((http_method:GET)) AND (action:show)","group_by":[],"loc_query_parameters":[{"data_type":"any","default_value":"GET","description":"","key":"lt","lookup_table":"http_method","name":"newParameter","optional":false,"title":"lt","type":"lut-parameter-v1"}],"search_within_ms":300000,"streams":["streamId"]}');
  });

  it('takes strategy from hook', () => {
    asMock(useModalReducer).mockReturnValue([
      {
        strategy: 'CUSTOM',
        checked,
        showDetails: false,
      },
      mockedDispatch,
    ]);

    renderWithAllChecked();
    const input = screen.getByDisplayValue('CUSTOM');

    expect(input).toHaveAttribute('checked', '');
  });
});
