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
import generateId from 'logic/generateId';

jest.mock('views/logic/valueactions/createEventDefinition/hooks/useModalReducer', () => jest.fn());
jest.mock('logic/generateId');

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

    await screen.findByText('Exactly this value');
    await screen.findByText('Any in row');
    await screen.findByText('Any in column');
    await screen.findByText('Any in widget');
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

  it('doesnt show Any in row and Any in column when no rowValuePath and columnValuePath', async () => {
    asMock(useModalReducer).mockReturnValue([
      {
        strategy: 'EXACT',
        checked,
        showDetails: false,
      },
      mockedDispatch,
    ]);

    renderCreateDefinitionModal({});

    await screen.findByText('Exactly this value');
    await screen.findByText('Any in widget');
    await screen.findByText('Custom');
    await screen.findByText('Show strategy details');

    expect(screen.queryByText('Any in row')).not.toBeInTheDocument();
    expect(screen.queryByText('Any in column')).not.toBeInTheDocument();
  });

  describe('run dispatch for', () => {
    it('Exactly this value', async () => {
      asMock(useModalReducer).mockReturnValue([
        {
          strategy: 'ALL',
          checked,
          showDetails: true,
        },
        mockedDispatch,
      ]);

      renderWithAllChecked();

      const allButton = await screen.findByText('Exactly this value');
      fireEvent.click(allButton);

      expect(mockedDispatch).toHaveBeenCalledWith({
        type: 'SET_EXACT_STRATEGY',
      });
    });

    it('Any in row', async () => {
      asMock(useModalReducer).mockReturnValue([
        {
          strategy: 'EXACT',
          checked,
          showDetails: true,
        },
        mockedDispatch,
      ]);

      renderWithAllChecked();

      const rowButton = await screen.findByText('Any in row');
      fireEvent.click(rowButton);

      expect(mockedDispatch).toHaveBeenCalledWith({
        type: 'SET_ROW_STRATEGY',
      });
    });

    it('Any in column', async () => {
      asMock(useModalReducer).mockReturnValue([
        {
          strategy: 'EXACT',
          checked,
          showDetails: true,
        },
        mockedDispatch,
      ]);

      renderWithAllChecked();

      const colButton = await screen.findByText('Any in column');
      fireEvent.click(colButton);

      expect(mockedDispatch).toHaveBeenCalledWith({
        type: 'SET_COL_STRATEGY',
      });
    });

    it('Any in widget', async () => {
      asMock(useModalReducer).mockReturnValue([
        {
          strategy: 'EXACT',
          checked,
          showDetails: true,
        },
        mockedDispatch,
      ]);

      renderWithAllChecked();

      const widgetButton = await screen.findByText('Any in widget');
      fireEvent.click(widgetButton);

      expect(mockedDispatch).toHaveBeenCalledWith({
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
      fireEvent.click(customButton);

      expect(mockedDispatch).toHaveBeenCalledWith({
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
      fireEvent.click(checkbox);

      expect(mockedDispatch).toHaveBeenCalledWith({
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
    asMock(generateId).mockReturnValue('session-id');

    asMock(useModalReducer).mockReturnValue([
      {
        strategy: 'EXACT',
        checked,
        showDetails: true,
      },
      mockedDispatch,
    ]);

    renderWithAllChecked();

    const linkButton = await screen.findByRole<HTMLAnchorElement>('link', {
      name: /continue configuration/i,
      hidden: true,
    });

    expect(linkButton.href).toContain('/alerts/definitions/new?step=condition&session-id=cedfv-session-id');
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
