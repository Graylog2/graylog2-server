// @flow strict
import Widget from '../widgets/Widget';
import DuplicateCommonWidgetSettings from './DuplicateCommonWidgetSettings';
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
