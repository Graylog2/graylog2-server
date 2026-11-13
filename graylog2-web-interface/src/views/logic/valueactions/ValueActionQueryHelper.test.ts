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
import { MISSING_BUCKET_NAME } from 'views/Constants';

import { fieldValueOrClause, orValuesClause } from './ValueActionQueryHelper';

describe('ValueActionQueryHelper', () => {
  const values = ['id1', 'id2', 'id3'].map((value) => ({ value }));

  it('joins values in an OR clause', () => {
    expect(orValuesClause(values)).toEqual('(id1 OR id2 OR id3)');
  });

  it('joins field values in a field scoped OR clause', () => {
    expect(fieldValueOrClause('associated_assets', values)).toEqual('(associated_assets:(id1 OR id2 OR id3))');
  });

  it('matches the empty value bucket with a missing field', () => {
    expect(fieldValueOrClause('source', [{ value: 'a' }, { value: 'b' }, { value: MISSING_BUCKET_NAME }])).toEqual(
      '((source:(a OR b)) OR (NOT _exists_:source))',
    );
  });

  it('matches only a missing field when the empty value bucket is the only value', () => {
    expect(fieldValueOrClause('source', [{ value: MISSING_BUCKET_NAME }])).toEqual('(NOT _exists_:source)');
  });

  it('escapes values', () => {
    expect(orValuesClause(['a b', 'c:d'].map((value) => ({ value })))).toEqual('("a b" OR c\\:d)');
  });
});
