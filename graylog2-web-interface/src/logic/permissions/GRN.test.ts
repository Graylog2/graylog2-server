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
import Routes from 'routing/Routes';

import { createGRN, getValuesFromGRN, getShowRouteFromGRN } from './GRN';

describe('GRN', () => {
  it('createGRN should generate GRN with correct format', () => {
    expect(createGRN('dashboard', 'dashboard-id')).toBe('grn::::dashboard:dashboard-id');
  });

  it('getValuesFromGRN should extract correct values from GRN', () => {
    expect(getValuesFromGRN('grn::::stream:stream-id')).toStrictEqual({
      resourceNameType: 'grn',
      cluster: undefined,
      tenent: undefined,
      scope: undefined,
      type: 'stream',
      id: 'stream-id',
    });
  });

  describe('getShowRouteFromGRN should return correct route for', () => {
    const createsCorrectShowEntityURL = ({ grn, entityShowURL }) => {
      expect(getShowRouteFromGRN(grn)).toBe(entityShowURL);
    };

    it.each`
      type            | grn                                 | entityShowURL
      ${'user'}       | ${'grn::::user:user-id'}            | ${Routes.SYSTEM.USERS.show('user-id')}
      ${'stream'}     | ${'grn::::stream:stream-id'}        | ${Routes.stream_search('stream-id')}
    `('type $type with grn $grn', createsCorrectShowEntityURL);
  });
});
