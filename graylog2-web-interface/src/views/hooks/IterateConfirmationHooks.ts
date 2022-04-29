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
const iterateConfirmationHooks = async <Args extends Array<any>>(hooks: Array<(...args: Args) => Promise<boolean | null>>, ...args: Args) => {
  // eslint-disable-next-line no-restricted-syntax
  for (const hook of hooks) {
    try {
      // eslint-disable-next-line no-await-in-loop
      const result = await hook(...args);

      if (result !== null) {
        return result === true;
      }
    } catch (e) {
      // eslint-disable-next-line no-console
      console.trace('Exception occurred in deletion confirmation hook: ', e);
    }
  }

  return null;
};

export default iterateConfirmationHooks;
