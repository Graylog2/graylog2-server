// @flow strict
import { WidgetActions } from 'enterprise/stores/WidgetStore';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import AggregationWidget from 'enterprise/logic/aggregationbuilder/AggregationWidget';
import Series from 'enterprise/logic/aggregationbuilder/Series';
import { TitlesActions, TitleTypes } from 'enterprise/stores/TitlesStore';
import type { FieldActionHandler } from './FieldActionHandler';
import FieldType from '../fieldtypes/FieldType';

const NUMERIC_FIELD_SERIES = ['count', 'sum', 'avg', 'min', 'max', 'stddev', 'variance', 'card', 'percentile'];
const NONNUMERIC_FIELD_SERIES = ['count', 'card'];

const handler: FieldActionHandler = (queryId: string, field: string, type: FieldType) => {
  const series = ((type && type.isNumeric()) ? NUMERIC_FIELD_SERIES : NONNUMERIC_FIELD_SERIES)
    .map(f => `${f}(${field})`)
    .map(Series.forFunction);
  const config = AggregationWidgetConfig.builder()
    .series(series)
    .visualization('table')
    .rollup(true)
    .build();
  const widget = AggregationWidget.builder()
    .newId()
    .config(config)
    .build();
  return WidgetActions.create(widget).then(newWidget => TitlesActions.set(TitleTypes.Widget, newWidget.id, `Field Statistics for ${field}`));
};

export default handler;
