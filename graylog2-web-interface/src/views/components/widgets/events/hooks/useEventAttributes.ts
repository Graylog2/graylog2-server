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
import usePluginEntities from 'hooks/usePluginEntities';
import useServerText from 'util/serverText';

const useEventAttributes = () => {
  const eventAttributes = usePluginEntities('views.components.widgets.events.attributes');

  const serverText = useServerText();

  return eventAttributes
    .filter(({ useCondition }) =>
      // eslint-disable-next-line react-hooks/rules-of-hooks
      typeof useCondition === 'function' ? useCondition() : true,
    )
    .map((attribute) => ({ ...attribute, title: serverText(attribute.title) }));
};

export default useEventAttributes;
