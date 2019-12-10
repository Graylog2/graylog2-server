// @flow strict
import React, { useState } from 'react';
import styled from 'styled-components';
import { Map } from 'immutable';

import { CurrentViewStateStore } from 'views/stores/CurrentViewStateStore';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import Store from 'logic/local-storage/Store';

import SearchActions from 'views/actions/SearchActions';

import { Alert, Button, Row, Col } from 'components/graylog';
import Spinner from 'components/common/Spinner';
import LineVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/LineVisualizationConfig';
import AreaVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
import type { InterpolationMode } from 'views/logic/aggregationbuilder/visualizations/Interpolation';


type LegacyFieldChart = {
  field: string,
  renderer: string,
  interpolation: string,
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

const createVisualizationConfig = (interpolation: string, visualization: string) => {
  let interpolationName: InterpolationMode;
  switch (interpolation) {
    case 'basis':
    case 'bundle':
    case 'cardinal':
    case 'monotone':
      interpolationName = 'spline';
      break;
    case 'linear':
    case 'step-after':
      interpolationName = interpolation;
      break;
    default:
      interpolationName = 'linear';
  }

  switch (visualization) {
    case 'line':
      return new LineVisualizationConfig(interpolationName);
    case 'area':
      return new AreaVisualizationConfig(interpolationName);
    default:
      return undefined;
  }
};

const onMigrate = (legacyCharts: Array<LegacyFieldChart>, setMigrating: boolean => void) => {
  // TODO: Add widget position on create


  const newWidgets = legacyCharts.map((chart: LegacyFieldChart) => {
    setMigrating(true);

    const { field } = chart;
    // The old field charts only have one series per chart.
    const series = new Series(mapSeries(chart.valuetype, field));
    // Setting series. The old field charts only have one series per chart.
    const rowPivots = [new Pivot('timestamp', 'time', { interval: { type: 'timeunit', ...mapTime(chart.interval) } })];
    const visualization = mapVisualization(chart.renderer);
    const visualizationConfig = createVisualizationConfig(chart.interpolation, visualization);
    const widgetConfig = AggregationWidgetConfig.builder()
      .visualization(visualization)
      .visualizationConfig(visualizationConfig)
      .series([series])
      .rowPivots(rowPivots)
      .build();

    return AggregationWidget.builder()
      .newId()
      .type('SEARCH')
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

const onCancel = () => Store.set('pinned-field-charts-migrated', true);

const MigrateFieldCharts = () => {
  const legacyCharts: Array<LegacyFieldChart> = Object.values(Store.get('pinned-field-charts') || {});
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
