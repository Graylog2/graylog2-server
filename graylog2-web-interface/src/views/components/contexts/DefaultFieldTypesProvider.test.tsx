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
import { render } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';

import asMock from 'helpers/mocking/AsMock';
import { simpleFields, simpleQueryFields } from 'fixtures/fields';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import Query, { filtersForQuery } from 'views/logic/queries/Query';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';

import FieldTypesContext from './FieldTypesContext';
import DefaultFieldTypesProvider from './DefaultFieldTypesProvider';

jest.mock('views/logic/queries/useCurrentQuery');
jest.mock('views/logic/fieldtypes/useFieldTypes');

describe('DefaultFieldTypesProvider', () => {
  const renderSUT = () => {
    const consume = jest.fn();

    render(
      <DefaultFieldTypesProvider>
        <FieldTypesContext.Consumer>
          {consume}
        </FieldTypesContext.Consumer>
      </DefaultFieldTypesProvider>,
    );

    return consume;
  };

  it('provides no field types with empty store', () => {
    asMock(useCurrentQuery).mockReturnValue(Query.builder().id('foobar').build());
    asMock(useFieldTypes).mockReturnValue({ data: undefined });

    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith({ all: Immutable.List(), queryFields: Immutable.Map({ foobar: Immutable.List() }) });
  });

  it('provides field types of field types store', () => {
    asMock(useCurrentQuery).mockReturnValue(Query.builder()
      .id('queryId')
      .filter(filtersForQuery(['dummyStream']))
      .build());

    asMock(useFieldTypes).mockImplementation((streams) => (streams.length === 0
      ? { data: simpleFields().toArray() }
      : { data: simpleQueryFields('foo').get('foo').toArray() }));

    const consume = renderSUT();

    const fieldTypes = { all: simpleFields(), queryFields: simpleQueryFields('queryId') };

    expect(consume).toHaveBeenCalledWith(fieldTypes);
  });
});
