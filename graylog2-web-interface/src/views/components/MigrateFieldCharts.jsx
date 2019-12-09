// @flow strict
import React, { useState } from 'react';
import styled from 'styled-components';

import { CurrentViewStateStore } from 'views/stores/CurrentViewStateStore';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import Store from 'logic/local-storage/Store';

import SearchActions from 'views/actions/SearchActions';

import { Alert, Button, Row, Col } from 'components/graylog';
import Spinner from 'components/common/Spinner';


type LegacyFieldChart = {
  field: string,
  renderer: string,
  valuetype: string,
  interval: string,
}

const Actions = styled.div`
  margin-top: 10px;
`;

const mapTime = (oldTimeUnit) => {
  switch (oldTimeUnit) {
    case 'quarter':
      return { unit: 'month', value: 3 };
    default:
      return { unit: oldTimeUnit, value: 1 };
  }
};

const mapSeries = (legacySeriesName, field) => {
  // TODO: How to deal with mean and cardinality?
  let seriesName;
  switch (legacySeriesName) {
    case 'total':
      seriesName = 'sum';
      break;
    case 'mean':
      seriesName = 'avg';
      break;
    case 'cardinality':
      seriesName = 'card';
      break;
    default:
      seriesName = legacySeriesName;
  }
  return `${seriesName}(${field})`;
};

const mapVisualization = (visualization: string) => {
  switch (visualization) {
    case 'scatterplot':
      return 'scatter';
    default:
      return visualization;
  }
};

const onMigrate = (legacyCharts: Array<LegacyFieldChart>, setMigrating: boolean => void) => {
  // TODO: Add widget position on create
  // TODO: Implement interpolation
  // interpolation: [linear, step-after, basis, bundle, cardinal, monotone]
  // Other fields: createdAt, query, range: {relative: 300}, rangetype: ralative,

  const newWidgets = Object.values(legacyCharts).map((chart: LegacyFieldChart) => {
    setMigrating(true);

    const { field } = chart;
    // The old field charts only have one series per chart.
    const series = new Series(mapSeries(chart.valuetype, field));
    // Setting series. The old field charts only have one series per chart.
    const rowPivots = [new Pivot('timestamp', 'time', { interval: { type: 'timeunit', ...mapTime(chart.interval) } })];
    const visualization = mapVisualization(chart.renderer);
    const widgetConfig = AggregationWidgetConfig.builder()
      .visualization(visualization)
      .visualizationConfig(undefined)
      .series([series])
      .rowPivots(rowPivots)
      .build();

    return AggregationWidget.builder()
      .newId()
      .timerange(undefined)
      .config(widgetConfig)
      .build();
  });

  CurrentViewStateStore.widgets(newWidgets).then(() => {
    SearchActions.execute(SearchExecutionStateStore.getInitialState()).then(() => {
      setMigrating(false);
      Store.set('pinned-field-charts-migrated', true);
    });
  });
};

const onCancel = () => {
  Store.set('pinned-field-charts-migrated', true);
};

const MigrateFieldCharts = () => {
  const legacyCharts = Object.values(Store.get('pinned-field-charts') || {});
  const chartAmount = legacyCharts.length;
  const [migrating, setMigrating] = useState(false);

  return (
    <Row>
      <Col>
        <Alert bsStyle="warning">
          <h2>Migrate existing search page charts</h2>
          {/* Should we inform the user here about the backend migrations? */}
          <br />
            We found {chartAmount} chart(s), created for an older version of the search.
            Do you want to migrate these chart(s) for the current search?
          <br />
            When you have run the migration and want to keep the newly created charts, you will have to save the current search as a new dashboard.
            Otherwise the charts would get lost, when leaving the search page.
          <br />
          <Actions>
            <Button bsStyle="primary"
                    onClick={() => onMigrate(legacyCharts, setMigrating)}
                    disabled={migrating}
                    className="save-button-margin">
                  Migrate {migrating && <Spinner text="" />}
            </Button>
            <Button onClick={() => onCancel()}
                    disabled={migrating}>
                  Cancel
            </Button>
          </Actions>
        </Alert>
      </Col>
    </Row>
  );
};

export default MigrateFieldCharts;
