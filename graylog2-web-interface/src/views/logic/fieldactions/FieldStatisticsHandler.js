// @flow strict
import { WidgetActions } from 'views/stores/WidgetStore';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Widget from 'views/logic/widgets/Widget';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import Series from 'views/logic/aggregationbuilder/Series';
import { TitlesActions, TitleTypes } from 'views/stores/TitlesStore';
import type { FieldActionHandler } from './FieldActionHandler';
import duplicateCommonWidgetSettings from './DuplicateCommonWidgetSettings';

const NUMERIC_FIELD_SERIES = ['count', 'sum', 'avg', 'min', 'max', 'stddev', 'variance', 'card', 'percentile'];
const NONNUMERIC_FIELD_SERIES = ['count', 'card'];

const handler: FieldActionHandler = ({ field, type, contexts: { widget: origWidget = Widget.empty() } }) => {
  const series = ((type && type.isNumeric()) ? NUMERIC_FIELD_SERIES : NONNUMERIC_FIELD_SERIES)
    .map((f) => {
      if (f === 'percentile') {
        return `${f}(${field},95)`;
      }
      return `${f}(${field})`;
    })
    .map(Series.forFunction);
  const config = AggregationWidgetConfig.builder()
    .series(series)
    .visualization('table')
    .rollup(true)
    .build();
  const widgetBuilder = AggregationWidget.builder()
    .newId()
    .config(config);

  const widget = duplicateCommonWidgetSettings(widgetBuilder, origWidget).build();

  return WidgetActions.create(widget).then(newWidget => TitlesActions.set(TitleTypes.Widget, newWidget.id, `Field Statistics for ${field}`));
};

export default handler;
