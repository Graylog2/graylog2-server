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
import * as React from 'react';
import { useCallback, useState } from 'react';
import styled from 'styled-components';
import values from 'lodash/values';
import isEmpty from 'lodash/isEmpty';
import * as Immutable from 'immutable';

import Store from 'logic/local-storage/Store';
import { widgetDefinition } from 'views/logic/Widgets';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import type { TimeUnits } from 'views/Constants';
import Series from 'views/logic/aggregationbuilder/Series';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import LineVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/LineVisualizationConfig';
import AreaVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
import type { InterpolationMode } from 'views/logic/aggregationbuilder/visualizations/Interpolation';
import { Alert, Button, Row, Col } from 'components/bootstrap';
import Spinner from 'components/common/Spinner';
import { TIMESTAMP_FIELD } from 'views/Constants';
import type { AppDispatch } from 'stores/useAppDispatch';
import useAppDispatch from 'stores/useAppDispatch';
import { updateViewState } from 'views/logic/slices/viewSlice';
import { execute } from 'views/logic/slices/searchExecutionSlice';
import type { WidgetPositions, GetState } from 'views/types';
import { selectActiveViewState, selectActiveQuery } from 'views/logic/slices/viewSelectors';

// localStorage keys
const FIELD_CHARTS_KEY = 'pinned-field-charts';
const FIELD_CHARTS_MIGRATED_KEY = 'pinned-field-charts-migrated';

type LegacySeries = 'mean' | 'max' | 'min' | 'total' | 'count' | 'cardinality';
type LegacyInterpolation = 'linear' | 'step-after' | 'basis' | 'bundle' | 'cardinal' | 'monotone';
type LegacyInterval = 'minute' | 'hour' | 'day' | 'week' | 'month' | 'quarter' | 'year';
type LegacyVisualization = 'bar' | 'area' | 'line' | 'scatterplot';
type LegacyFieldChart = {
  field: string,
  renderer: LegacyVisualization,
  interpolation: LegacyInterpolation,
  valuetype: LegacySeries,
  interval: LegacyInterval,
};

const Actions = styled.div`
  margin-top: 10px;
`;

const mapSeriesFunction = (legacySeries: LegacySeries) => {
  switch (legacySeries) {
    case 'total':
      return 'sum';
    case 'mean':
      return 'avg';
    case 'cardinality':
      return 'card';
    default:
      return legacySeries;
  }
};

const mapSeries = (legacySeries: LegacySeries, field: string) => {
  const seriesFunction = mapSeriesFunction(legacySeries);

  return `${seriesFunction}(${field})`;
};

const mapVisualization = (legacyVisualization: LegacyVisualization) => {
  switch (legacyVisualization) {
    case 'scatterplot':
      return 'scatter';
    case 'line':
    case 'area':
    case 'bar':
      return legacyVisualization;
    default:
      throw new Error(`Unsupported visualization ${legacyVisualization}`);
  }
};

const mapTime = (legacyTime: string): { value: number, unit: keyof typeof TimeUnits } => {
  switch (legacyTime) {
    case 'quarter':
      return { unit: 'months', value: 3 };
    default:
      return { unit: `${legacyTime}s` as keyof typeof TimeUnits, value: 1 };
  }
};

const mapInterpolation = (legacyInterpolation: LegacyInterpolation): InterpolationMode => {
  switch (legacyInterpolation) {
    case 'basis':
    case 'bundle':
    case 'cardinal':
    case 'monotone':
      return 'spline';
    case 'linear':
    case 'step-after':
      return legacyInterpolation;
    default:
      throw new Error(`Unsupported interpolation ${legacyInterpolation}`);
  }
};

const createVisualizationConfig = (legacyInterpolation: LegacyInterpolation, visualization: string) => {
  const interpolation = mapInterpolation(legacyInterpolation);

  switch (visualization) {
    case 'line':
      return LineVisualizationConfig.create(interpolation);
    case 'area':
      return AreaVisualizationConfig.create(interpolation);
    default:
      return undefined;
  }
};

const _updateExistingWidgetPos = (existingPositions: WidgetPositions, rowOffset: number) => {
  const updatedWidgetPos = { ...existingPositions };

  Object.keys(updatedWidgetPos).forEach((widgetId) => {
    const widgetPos = updatedWidgetPos[widgetId];

    updatedWidgetPos[widgetId] = widgetPos.toBuilder().row(widgetPos.row + rowOffset).build();
  });

  return updatedWidgetPos;
};

const _migrateWidgets = (legacyCharts: Array<LegacyFieldChart>) => async (_dispatch: AppDispatch, getState: GetState) => {
  const { defaultHeight } = widgetDefinition(AggregationWidget.type);
  const currentView = selectActiveViewState(getState());
  const activeQuery = selectActiveQuery(getState());
  const newWidgetPositions = {};

  const newWidgets = legacyCharts.map((chart: LegacyFieldChart, index: number) => {
    const { field } = chart;
    // The old field charts only have one series per chart.
    // The series always relates to the selected field.
    const series = new Series(mapSeries(chart.valuetype, field));
    // Because all field charts show the results for the defined timerange,
    // the new row pivot always contains the timestamp field.
    const rowPivotConfig = { interval: { type: 'timeunit' as const, ...mapTime(chart.interval) } };
    const rowPivot = Pivot.create([TIMESTAMP_FIELD], 'time', rowPivotConfig);
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
    const widgetRowPos = (defaultHeight * index) + 1;

    newWidgetPositions[newWidget.id] = new WidgetPosition(1, widgetRowPos, defaultHeight, Infinity);

    return newWidget;
  });

  const newWidgetsRowOffset = legacyCharts.length * defaultHeight;
  const existingWidgetPos = _updateExistingWidgetPos(currentView.widgetPositions, newWidgetsRowOffset);
  const newViewState = currentView
    .toBuilder()
    .widgets(Immutable.List([
      // @ts-ignore
      ...currentView.widgets,
      ...newWidgets,
    ]))
    .widgetPositions({ ...existingWidgetPos, ...newWidgetPositions })
    .build();

  return { newViewState, currentQueryId: activeQuery };
};

const _onMigrate = async (dispatch: AppDispatch, legacyCharts: Array<LegacyFieldChart>, setMigrating: (migrating: boolean) => void, setMigrationFinished: (finished: boolean) => void) => {
  setMigrating(true);

  const { newViewState, currentQueryId } = await dispatch(_migrateWidgets(legacyCharts));
  await dispatch(updateViewState(currentQueryId, newViewState));
  await dispatch(execute());
  Store.set(FIELD_CHARTS_MIGRATED_KEY, 'finished');
  setMigrating(false);
  setMigrationFinished(true);
};

const _onCancel = (setMigrationFinished: (finished: boolean) => void) => {
  Store.set(FIELD_CHARTS_MIGRATED_KEY, 'discarded');
  setMigrationFinished(true);
};

const MigrateFieldCharts = () => {
  const [migrating, setMigrating] = useState(false);
  const [migrationFinished, setMigrationFinished] = useState(!!Store.get(FIELD_CHARTS_MIGRATED_KEY));
  const legacyCharts: Array<LegacyFieldChart> = values(Store.get(FIELD_CHARTS_KEY));
  const chartAmount = legacyCharts.length;
  const dispatch = useAppDispatch();
  const onMigrate = useCallback(() => _onMigrate(dispatch, legacyCharts, setMigrating, setMigrationFinished), [dispatch, legacyCharts]);

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
                    onClick={onMigrate}
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
