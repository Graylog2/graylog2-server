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
import type { IndexSetFieldRestriction } from 'stores/indices/IndexSetsStore';

// eslint-disable-next-line import/prefer-default-export
export const parseFieldRestrictions = (
  field_restrictions?: IndexSetFieldRestriction[],
): { immutableFields: Array<string>; hiddenFields: Array<string> } => {
  if (!field_restrictions || field_restrictions.length < 1) return { immutableFields: [], hiddenFields: [] };

  const getHidden = () =>
    Object.keys(field_restrictions).filter(
      (field) => field_restrictions[field].filter((restriction) => restriction.type === 'hidden').length > 0,
    );

  const getImmutable = () =>
    Object.keys(field_restrictions).filter(
      (field) => field_restrictions[field].filter((restriction) => restriction.type === 'immutable').length > 0,
    );

  return { immutableFields: getImmutable(), hiddenFields: getHidden() };
};
