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
import { fieldValueOrClause, orValuesClause } from './ValueActionQueryHelper';

describe('ValueActionQueryHelper', () => {
  const values = ['id1', 'id2', 'id3'].map((value) => ({ value }));

  it('joins values in an OR clause', () => {
    expect(orValuesClause(values)).toEqual('(id1 OR id2 OR id3)');
  });

  it('joins field values in a field scoped OR clause', () => {
    expect(fieldValueOrClause('associated_assets', values)).toEqual('(associated_assets:(id1 OR id2 OR id3))');
  });

  it('escapes values', () => {
    expect(orValuesClause(['a b', 'c:d'].map((value) => ({ value })))).toEqual('("a b" OR c\\:d)');
  });
});
