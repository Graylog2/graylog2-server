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
import { OrderedMap } from 'immutable';

import FiltersForQueryParams from './FiltersForQueryParams';

const exampleFilters = OrderedMap({
  index_set_id: ['index-set-1', 'index-set-2'],
});

describe('FiltersForQueryParams', () => {
  it('should transform multiple filters', () => {
    const result = FiltersForQueryParams(exampleFilters);

    expect(result).toEqual(['index_set_id:index-set-1', 'index_set_id:index-set-2']);
  });
});
