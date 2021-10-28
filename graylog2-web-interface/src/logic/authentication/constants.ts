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

const LOGIN_INITIALIZING_STATE = 'INITIALIZING'; // Initial State for the plugin login form set on the login page in created.
const LOGIN_INITIALIZED_STATE = 'INITIALIZED'; // Should be set once the plugin based login form is initialized
const LOGIN_TRANSITIONING_STATE = 'TRANSITIONING'; // value when the plugin based form is in transition state for example handling login callback

export {
  LOGIN_INITIALIZING_STATE,
  LOGIN_INITIALIZED_STATE,
  LOGIN_TRANSITIONING_STATE,
};
