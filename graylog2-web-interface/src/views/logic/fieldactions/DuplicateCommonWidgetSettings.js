// @flow strict
import Widget from '../widgets/Widget';

const duplicateCommonWidgetSettings = (widgetBuilder: Widget.Builder, originalWidget: Widget) => {
  let result = widgetBuilder;
  const { filter, query, streams, timerange } = originalWidget;
  if (filter) {
    result = result.filter(filter);
  }
  if (query) {
    result = result.query(query)
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
