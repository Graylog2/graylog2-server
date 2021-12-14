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
import type { ActionDefinition } from 'views/components/actions/ActionHandler';

import AppConfig from '../AppConfig';

function filterValueActions(
  items: Array<ActionDefinition>,
  toExclude: Array<string>,
): Array<ActionDefinition> {
  return items.filter((item) => !toExclude.includes(item.type));
}

export function filterCloudValueActions(
  valueActions: Array<ActionDefinition>,
  toExclude: Array<string>,
): Array<ActionDefinition> {
  if (!AppConfig.isCloud()) {
    return valueActions;
  }

  return filterValueActions(valueActions, toExclude);
}

export default filterValueActions;
