// @flow strict
import React, { useState } from 'react';
import styled from 'styled-components';
import { maxBy, values, isEmpty } from 'lodash';
import Immutable from 'immutable';

import { CurrentViewStateStore } from 'views/stores/CurrentViewStateStore';
import { ViewStatesActions } from 'views/stores/ViewStatesStore';
import SearchActions from 'views/actions/SearchActions';

import Store from 'logic/local-storage/Store';
import { widgetDefinition } from 'views/logic/Widgets';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import LineVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/LineVisualizationConfig';
import AreaVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
import type { InterpolationMode } from 'views/logic/aggregationbuilder/visualizations/Interpolation';

import { Alert, Button, Row, Col } from 'components/graylog';
import Spinner from 'components/common/Spinner';

type LegacySeries = 'mean' | 'max' | 'min' | 'total' | 'count' | 'cardinality';
type LegacyInterpolation = 'linear' | 'step-after' | 'basis' | 'bundle' | 'cardinal' | 'monotone';
type LegacyInterval = 'minute' | 'hour' | 'day' | 'week' | 'month' | 'quarter' | 'year'
type LegacyVisualization = 'bar' | 'area' | 'line' | 'scatterplot'
type LegacyFieldChart = {
  field: string,
  renderer: LegacyVisualization,
  interpolation: LegacyInterpolation,
  valuetype: LegacySeries,
  interval: LegacyInterval,
}

const Actions = styled.div`
  margin-top: 10px;
`;

const mapSeries = (legacySeries: LegacySeries, field: string) => {
  let seriesName;
  switch (legacySeries) {
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
      seriesName = legacySeries;
  }
  return `${seriesName}(${field})`;
};

const mapVisualization = (legacyVisualization: LegacyVisualization) => {
  switch (legacyVisualization) {
    case 'scatterplot':
      return 'scatter';
    default:
      return legacyVisualization;
  }
};

const mapTime = (legacyTime: string) => {
  switch (legacyTime) {
    case 'quarter':
      return { unit: 'months', value: 3 };
    default:
      return { unit: `${legacyTime}s`, value: 1 };
  }
};

const createVisualizationConfig = (legacyInterpolation: LegacyInterpolation, visualization: string) => {
  let interpolation: InterpolationMode;
  switch (legacyInterpolation) {
    case 'basis':
    case 'bundle':
    case 'cardinal':
    case 'monotone':
      interpolation = 'spline';
      break;
    case 'linear':
    case 'step-after':
      interpolation = legacyInterpolation;
      break;
    default:
      interpolation = 'linear';
  }

  switch (visualization) {
    case 'line':
      return new LineVisualizationConfig(interpolation);
    case 'area':
      return new AreaVisualizationConfig(interpolation);
    default:
      return undefined;
  }
};

const _initialRowOffset = (widgetPositions) => {
  const lastWidgetPosition = maxBy(values(widgetPositions), (position: WidgetPosition): number => position.row);
  return lastWidgetPosition ? (lastWidgetPosition.row + lastWidgetPosition.height) : 0;
};

const _onMigrate = (legacyCharts: Array<LegacyFieldChart>, setMigrating: boolean => void, setMigrationFinished: boolean => void) => {
  setMigrating(true);
  const { defaultHeight } = widgetDefinition('AGGREGATION');
  const currentView = CurrentViewStateStore.getInitialState();
  const newWidgetPositions = { ...currentView.state.widgetPositions };
  const initialRowOffset = _initialRowOffset(newWidgetPositions);

  const newWidgets = legacyCharts.map((chart: LegacyFieldChart, index: number) => {
    const { field } = chart;
    // The old field charts only have one series per chart.
    // The series always relates to the selected field.
    const series = new Series(mapSeries(chart.valuetype, field));
    // Because all field charts show the results for the defined timerange,
    // the new row pivot always contains the timestamp field.
    const rowPivotConfig = { interval: { type: 'timeunit', ...mapTime(chart.interval) } };
    const rowPivot = new Pivot('timestamp', 'time', rowPivotConfig);
    const visualization = mapVisualization(chart.renderer);
    const visualizationConfig = createVisualizationConfig(chart.interpolation, visualization);
    // create widget with migrated data
    const widgetConfig = AggregationWidgetConfig.builder()
      .visualization(visualization)
      .visualizationConfig(visualizationConfig)
      .series([series])
      .rowPivots([rowPivot])
      .build();
    const newWidget = AggregationWidget.builder()
      .newId()
      .timerange(undefined)
      .config(widgetConfig)
      .build();
    // create widget position for new widget
    const migratedWidgetsOffset = defaultHeight * index;
    const widgetRowPos = initialRowOffset + migratedWidgetsOffset;
    newWidgetPositions[newWidget.id] = new WidgetPosition(1, widgetRowPos || 1, defaultHeight, Infinity);
    return newWidget;
  });

  const newViewState = currentView.state
    .toBuilder()
    .widgets(Immutable.List([...currentView.state.widgets, ...newWidgets]))
    .widgetPositions(newWidgetPositions)
    .build();

  ViewStatesActions.update(currentView.activeQuery, newViewState).then(
    () => SearchActions.executeWithCurrentState().then(() => {
      Store.set('pinned-field-charts-migrated', true);
      setMigrating(false);
      setMigrationFinished(true);
    }),
  );
};

const _onCancel = (setMigrationFinished) => {
  Store.set('pinned-field-charts-migrated', true);
  setMigrationFinished(true);
};

const MigrateFieldCharts = () => {
  const [migrating, setMigrating] = useState(false);
  const [migrationFinished, setMigrationFinished] = useState(!!Store.get('pinned-field-charts-migrated'));
  const legacyCharts: Array<LegacyFieldChart> = values(Store.get('pinned-field-charts'));
  const chartAmount = legacyCharts.length;

  if (migrationFinished || isEmpty(legacyCharts)) {
    return null;
  }

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
            Otherwise the charts will get lost, when leaving the search page.
          <br />
          <Actions>
            <Button bsStyle="primary"
                    onClick={() => _onMigrate(legacyCharts, setMigrating, setMigrationFinished)}
                    disabled={migrating}
                    className="save-button-margin">
                  Migrate {migrating && <Spinner text="" />}
            </Button>
            <Button onClick={() => _onCancel(setMigrationFinished)}
                    disabled={migrating}>
                  Discard charts
            </Button>
          </Actions>
        </Alert>
      </Col>
    </Row>
  );
};

export default MigrateFieldCharts;
