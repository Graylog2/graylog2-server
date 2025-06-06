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
import type { PluginNavigation } from 'graylog-web-plugin';

type Item = { position?: PluginNavigation['position']; description: string };

const sortInAfterItems = <T extends Item>(targetList: Array<T>, afterItems: Array<T>) => {
  const result = [...targetList];

  afterItems.forEach((afterItem) => {
    const index = result.findIndex((targetItem) => targetItem.description === afterItem.position?.after);
    if (index !== -1) {
      result.splice(index + 1, 0, afterItem);
    } else {
      result.push(afterItem);
    }
  });

  return result;
};

const sortNavigationItems = <T extends Item>(navigationItems: Array<T>) => {
  const withoutPositionItems = navigationItems.filter((item) => !item.position);
  const afterItems = navigationItems.filter((item) => !!item.position?.after);
  const lastItems = navigationItems.filter((item) => !!item.position?.last);

  return [...sortInAfterItems<T>(withoutPositionItems, afterItems), ...lastItems];
};

export default sortNavigationItems;
