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
import { render, fireEvent } from 'wrappedTestingLibrary';
import { List } from 'immutable';

import Direction from 'views/logic/aggregationbuilder/Direction';
import FieldType, { Property } from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';

import FieldSortSelect from './FieldSortSelect';

describe('FieldSortSelect', () => {
  const properties: Property[] = ['enumerable'];
  const fieldType1 = new FieldType('string', properties, []);
  const fieldTypeMapping1 = new FieldTypeMapping('date', fieldType1);
  const fieldType2 = new FieldType('string', properties, []);
  const fieldTypeMapping2 = new FieldTypeMapping('http_method', fieldType2);
  const fields = List([fieldTypeMapping1, fieldTypeMapping2]);
  const sort = [new SortConfig('pivot', 'http_method', Direction.Ascending)];

  it('should display current sort as selected option', () => {
    const { getByText } = render(<FieldSortSelect fields={fields} onChange={() => {}} sort={sort} />);

    expect(getByText('http_method')).not.toBeNull();
  });

  it('should open menu when focused', async () => {
    const { findByText, container } = render(<FieldSortSelect fields={fields} onChange={() => {}} sort={sort} />);
    fireEvent.focus(container.getElementsByTagName('input')[0]);
    await findByText(/2 results available./);
  });
});
