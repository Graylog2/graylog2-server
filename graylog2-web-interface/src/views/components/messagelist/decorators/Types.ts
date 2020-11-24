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
export type Decorator = {
  id: string,
  order: number,
  type: string,
  stream: string | undefined | null,
};

// Not properly typed yet, but not needed for the current scope.
export type RequestedConfiguration = {};

export type DecoratorType = {
  type: string,
  name: string,
  human_name: string,
  requested_configuration: RequestedConfiguration,
  link_to_docs: string,
};
