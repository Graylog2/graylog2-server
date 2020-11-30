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
// @flow strict

import type { ListenableAction, PromiseProvider } from 'stores/StoreTypes';

const listenable = () => ({ listen: jest.fn(() => jest.fn()) });

const noop: PromiseProvider = jest.fn(() => Promise.resolve());

function mockAction(): ListenableAction<typeof noop>;
function mockAction<R extends PromiseProvider>(fn: R): ListenableAction<R>;
function mockAction(fn = noop) {
  return Object.assign(fn, listenable(), {
    completed: listenable(),
    promise: jest.fn(),
  });
}

export default mockAction;
