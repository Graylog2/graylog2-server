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
import { SYSTEM_EVENT_DEFINITION_TYPE } from 'components/event-definitions/constants';

import bindings from './bindings';

describe('event definition type bindings', () => {
  const systemType = bindings.eventDefinitionTypes.find((type) => type.type === SYSTEM_EVENT_DEFINITION_TYPE);

  it('registers the system Event Definition type', () => {
    expect(systemType).toBeDefined();
    expect(systemType.displayName).toBe('System Event Definition');
  });

  it('hides the system Event Definition type from the event definition wizard, but still offers it as a filter', () => {
    expect(systemType.hideFromCreation).toBe(true);
    expect(systemType.useCondition()).toBe(true);
  });
});
