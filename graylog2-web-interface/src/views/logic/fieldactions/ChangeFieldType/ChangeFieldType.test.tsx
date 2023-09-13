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
import { render, screen } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import CreateEventDefinition from 'views/logic/valueactions/createEventDefinition/CreateEventDefinition';
import FieldType from 'views/logic/fieldtypes/FieldType';
import { AdditionalContext } from 'views/logic/ActionContext';
import { mappedDataResult, mockedContexts, modalDataResult } from 'fixtures/createEventDefinitionFromValue';
import useMappedData from 'views/logic/valueactions/createEventDefinition/hooks/useMappedData';
import useModalData from 'views/logic/valueactions/createEventDefinition/hooks/useModalData';

const onClose = jest.fn();
const renderCreateDefinitionAction = ({
  queryId = 'query-id',
  field = 'field',
  type = FieldType.create('STRING'),
  value = 'value',
  contexts = mockedContexts,
}) => render(
  <AdditionalContext.Provider value={contexts}>
    <CreateEventDefinition onClose={onClose} queryId={queryId} field={field} type={type} value={value} />
  </AdditionalContext.Provider>,
);

jest.mock('views/logic/valueactions/createEventDefinition/hooks/useModalData', () => jest.fn());
jest.mock('views/logic/valueactions/createEventDefinition/hooks/useMappedData', () => jest.fn());

describe('CreateEventDefinition', () => {
  it('runs hooks with correct params and shows modal', async () => {
    asMock(useMappedData).mockReturnValue(mappedDataResult);
    asMock(useModalData).mockReturnValue(modalDataResult);
    renderCreateDefinitionAction({});

    expect(useMappedData).toHaveBeenCalledWith({
      contexts: mockedContexts,
      field: 'field',
      queryId: 'query-id',
      value: 'value',
    });

    expect(useModalData).toHaveBeenCalledWith(mappedDataResult);

    await screen.findByText('Configure new event definition');
  });
});
