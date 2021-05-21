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
import MockStore from 'helpers/mocking/StoreMock';

import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Widget from 'views/logic/widgets/Widget';
import DataTable from 'views/components/datatable/DataTable';

import bindings from './bindings';

jest.mock('views/stores/FieldTypesStore', () => ({
  FieldTypesStore: MockStore(['getInitialState', () => ({ all: {}, queryFields: {} })]),
}));

jest.mock('stores/configurations/ConfigurationsStore', () => ({
  ConfigurationsStore: MockStore(),
}));

jest.mock('stores/decorators/DecoratorsStore', () => ({
  DecoratorsStore: MockStore(),
}));

describe('Views bindings enterprise widgets', () => {
  const { enterpriseWidgets } = bindings;
  type WidgetConfig = {
    needsControlledHeight: (widget?: Widget) => boolean,
  };
  const findWidgetConfig = (type) => enterpriseWidgets.find((widgetConfig) => widgetConfig.type === type);

  describe('Aggregations', () => {
    const aggregationConfig: WidgetConfig = findWidgetConfig('AGGREGATION');

    it('is present', () => {
      expect(aggregationConfig).toBeDefined();
    });

    it('need a controlled height by default', () => {
      expect(aggregationConfig.needsControlledHeight()).toBe(true);
    });

    it('do not need controlled height when visualization has type data table', () => {
      const widget = AggregationWidget.builder()
        .id('widget1')
        .config(AggregationWidgetConfig.builder().visualization(DataTable.type).build())
        .build();

      expect(aggregationConfig.needsControlledHeight(widget)).toBe(false);
    });
  });
});
