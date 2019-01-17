// @flow strict
import * as Immutable from 'immutable';

import { WidgetActions } from 'enterprise/stores/WidgetStore';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import AggregationWidget from 'enterprise/logic/aggregationbuilder/AggregationWidget';
import { FieldTypesStore } from 'enterprise/stores/FieldTypesStore';
import { ViewMetadataStore } from 'enterprise/stores/ViewMetadataStore';
import Series from 'enterprise/logic/aggregationbuilder/Series';
import type { ValueActionHandler } from '../valueactions/ValueActionHandler';

class FieldTypeSpecificSeries {
  static NUMERIC_FIELD_SERIES = ['count', 'sum', 'avg', 'min', 'max', 'stddev', 'variance', 'card'];
  static NONNUMERIC_FIELD_SERIES = ['count', 'card'];

  activeQuery: string;
  state: {
    all: Immutable.List<any>,
    queryFields: Immutable.List<any>,
  };

  constructor() {
    this.state = FieldTypesStore.getInitialState();
    FieldTypesStore.listen((newState) => {
      this.state = newState;
    });

    this.onViewMetadataStoreUpdate(ViewMetadataStore.getInitialState());
    ViewMetadataStore.listen(this.onViewMetadataStoreUpdate);
  }

  onViewMetadataStoreUpdate = (newState) => {
    const { activeQuery } = newState;
    this.activeQuery = activeQuery;
  };

  seriesFor(field) {
    const { all, queryFields } = this.state;
    const currentQueryFields = queryFields.get(this.activeQuery, Immutable.Set());
    const fieldDefinition = currentQueryFields.concat(all).find(({ name }) => name === field);

    if (fieldDefinition && fieldDefinition.type.isNumeric()) {
      return FieldTypeSpecificSeries.NUMERIC_FIELD_SERIES;
    }
    return FieldTypeSpecificSeries.NONNUMERIC_FIELD_SERIES;
  }
}

const handler: ValueActionHandler = (queryId: string, field: string) => {
  const fieldTypeSpecificSeries = new FieldTypeSpecificSeries();
  const series = fieldTypeSpecificSeries.seriesFor(field).map(f => `${f}(${field})`).map(Series.forFunction);
  const config = AggregationWidgetConfig.builder()
    .series(series)
    .visualization('table')
    .rollup(true)
    .build();
  const widget = AggregationWidget.builder()
    .newId()
    .config(config)
    .build();
  return WidgetActions.create(widget);
};

export default handler;
