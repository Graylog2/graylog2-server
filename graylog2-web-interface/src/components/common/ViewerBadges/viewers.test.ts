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
import { currentUserFirst, initials, viewerLabel } from './viewers';

const jane = { id: 'user-1', username: 'jane', full_name: 'Jane Q. Doe' };
const bob = { id: 'user-2', username: 'bob' };

describe('viewers', () => {
  it.each([
    [jane, 'JD'],
    [{ ...jane, full_name: 'Jane' }, 'JA'],
    [{ ...jane, full_name: '  ' }, 'JA'],
    [bob, 'BO'],
    [{ ...bob, username: 'b' }, 'B'],
    [{ ...bob, full_name: 'Émile zola' }, 'ÉZ'],
  ])('derives initials of %o', (viewer, expected) => {
    expect(initials(viewer)).toBe(expected);
  });

  it('labels viewers with name and username', () => {
    expect(viewerLabel(jane, false)).toBe('Jane Q. Doe (jane)');
    expect(viewerLabel(bob, false)).toBe('bob');
    expect(viewerLabel(jane, true)).toBe('You (Jane Q. Doe)');
  });

  it('puts the current user first', () => {
    expect(currentUserFirst([bob, jane], 'user-1')).toEqual([jane, bob]);
    expect(currentUserFirst([bob, jane], undefined)).toEqual([bob, jane]);
  });
});
