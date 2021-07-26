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

import type { MessageEventType } from 'views/types/messageEventTypes';

// eslint-disable-next-line import/prefer-default-export
export const createSimpleMessageEventType = (index = 1, overrides: Partial<MessageEventType> = {}): MessageEventType => ({
  gl2EventTypeCode: `event-type-code-${index}`,
  summaryTemplate: '{field1} - {field2}',
  category: 'success',
  eventActions: ['action-id-1'],
  requiredFields: ['field1', 'field2'],
  optionalFields: ['field3'],
  ...overrides,
});
