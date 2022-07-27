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
import type { GRN, GRNType } from 'logic/permissions/types';
import getShowRouteForEntity from 'routing/getShowRouteForEntity';

const _convertEmptyString = (value: string) => (value === '' ? undefined : value);

export const createGRN = (type: string, id: string) => `grn::::${type}:${id}`;

export const getValuesFromGRN = (grn: GRN) => {
  const grnValues = grn.split(':');
  const [resourceNameType, cluster, tenent, scope, type, id] = grnValues.map(_convertEmptyString);

  return { resourceNameType, cluster, tenent, scope, type: type as GRNType, id };
};

export const getShowRouteFromGRN = (grn: string) => {
  const { id, type } = getValuesFromGRN(grn);

  return getShowRouteForEntity(id, type);
};
