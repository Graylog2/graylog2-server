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

import getShowRouteForEntity from 'routing/getShowRouteForEntity';
import Routes from 'routing/Routes';

describe('getShowRouteFromGRN should return correct route', () => {
  const createsCorrectShowEntityURL = ({ id, type, entityShowURL }) => {
    expect(getShowRouteForEntity(id, type)).toBe(entityShowURL);
  };

  // eslint-disable-next-line jest/expect-expect
  it.each`
      id              | type               | entityShowURL
      ${'user-id'}    | ${'user'}          | ${Routes.SYSTEM.USERS.show('user-id')}
      ${'stream-id'}  | ${'stream'}        | ${Routes.stream_search('stream-id')}
    `('for $type with id $id', createsCorrectShowEntityURL);
});
