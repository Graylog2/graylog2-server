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
import DuplicateCommonWidgetSettings from './DuplicateCommonWidgetSettings';

import Widget from '../widgets/Widget';
import { createElasticsearchQueryString } from '../queries/Query';

describe('DuplicateCommonWidgetSettings', () => {
  it('does not do anything if no query/filter/timerange/streams are defined', () => {
    const widget = Widget.builder().build();
    const widgetBuilder = Widget.builder();

    const result = DuplicateCommonWidgetSettings(widgetBuilder, widget);

    expect(result).toEqual(widgetBuilder);
  });

  it('duplicates query if present', () => {
    const widget = Widget.builder()
      .query(createElasticsearchQueryString('hello:world'))
      .build();
    const widgetBuilder = Widget.builder();

    const result = DuplicateCommonWidgetSettings(widgetBuilder, widget);

    expect(result.build().query).toEqual(createElasticsearchQueryString('hello:world'));
  });

  it('duplicates filter if present', () => {
    const widget = Widget.builder()
      .filter('hello:world')
      .build();
    const widgetBuilder = Widget.builder();

    const result = DuplicateCommonWidgetSettings(widgetBuilder, widget);

    expect(result.build().filter).toEqual('hello:world');
  });

  it('duplicates timerange if present', () => {
    const widget = Widget.builder()
      .timerange({ type: 'relative', range: 3600 })
      .build();
    const widgetBuilder = Widget.builder();

    const result = DuplicateCommonWidgetSettings(widgetBuilder, widget);

    expect(result.build().timerange).toEqual({ type: 'relative', range: 3600 });
  });

  it('duplicates streams if present', () => {
    const widget = Widget.builder()
      .streams(['stream1', 'stream23', 'stream42'])
      .build();
    const widgetBuilder = Widget.builder();

    const result = DuplicateCommonWidgetSettings(widgetBuilder, widget);

    expect(result.build().streams).toEqual(['stream1', 'stream23', 'stream42']);
  });
});
