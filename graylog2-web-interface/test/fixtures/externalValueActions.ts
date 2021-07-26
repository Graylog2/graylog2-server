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

// eslint-disable-next-line import/prefer-default-export
import { ActionDefinition } from 'views/components/actions/ActionHandler';

// eslint-disable-next-line import/prefer-default-export
export const createSimpleExternalValueAction = (overrides: Partial<ActionDefinition> = {}): ActionDefinition => ({
  type: 'http-get',
  title: 'Pivot to example.org',
  isHidden: ({ field }) => !['field1'].includes(field),
  linkTarget: () => 'the-link',
  resetFocus: false,
  ...overrides,
});
