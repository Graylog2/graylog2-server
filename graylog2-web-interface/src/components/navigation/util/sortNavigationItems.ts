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

// Items whose target is not inserted yet are retried, so an item can be positioned after another positioned item,
// independent of the registration order.
const sortInAfterItems = <T extends Item>(targetList: Array<T>, afterItems: Array<T>): Array<T> => {
  const result = [...targetList];
  const remainingItems: Array<T> = [];

  afterItems.forEach((afterItem) => {
    const index = result.findIndex((targetItem) => targetItem.description === afterItem.position?.after);

    if (index !== -1) {
      result.splice(index + 1, 0, afterItem);
    } else {
      remainingItems.push(afterItem);
    }
  });

  if (remainingItems.length > 0 && remainingItems.length < afterItems.length) {
    return sortInAfterItems(result, remainingItems);
  }

  return [...result, ...remainingItems];
};

const sortNavigationItems = <T extends Item>(navigationItems: Array<T>) => {
  const withoutPositionItems = navigationItems.filter((item) => !item.position);
  const afterItems = navigationItems.filter((item) => !!item.position?.after);
  const lastItems = navigationItems.filter((item) => !!item.position?.last);

  return [...sortInAfterItems<T>(withoutPositionItems, afterItems), ...lastItems];
};

export default sortNavigationItems;
