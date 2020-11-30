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
import Widget from '../widgets/Widget';

const duplicateCommonWidgetSettings = (
// @ts-ignore
  widgetBuilder: Widget.Builder,
  originalWidget: Widget,
) => {
  let result = widgetBuilder;
  const { filter, query, streams, timerange } = originalWidget;

  if (filter) {
    result = result.filter(filter);
  }

  if (query) {
    result = result.query(query);
  }

  if (streams) {
    result = result.streams(streams);
  }

  if (timerange) {
    result = result.timerange(timerange);
  }

  return result;
};

export default duplicateCommonWidgetSettings;
