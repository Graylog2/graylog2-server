// @flow strict
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Widget from 'views/logic/widgets/Widget';
import DataTable from 'views/components/datatable/DataTable';
import bindings from './bindings';

describe('Views bindings enterprise widgets', () => {
  const { enterpriseWidgets } = bindings;
  type WidgetCondig = {
    needsControlledHeight: (widget?: Widget) => boolean
  }
  const findWidgetConfig = type => enterpriseWidgets.find(widgetConfig => widgetConfig.type === type);

  describe('Aggregations', () => {
    // $FlowFixMe: We are assuming here it is generally present
    const aggregationConfig: WidgetCondig = findWidgetConfig('AGGREGATION');
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
