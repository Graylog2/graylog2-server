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
import { useCallback } from 'react';
import cloneDeep from 'lodash/cloneDeep';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { Select } from 'components/common';
import { Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/bootstrap';
import { percentileOptions, percentageStrategyOptions } from 'views/Constants';

const formatFunctions = (functions) => functions
  .sort(naturalSort)
  .map((fn) => ({ label: `${fn.toLowerCase()}()`, value: fn }));

const getSeriesId = (currentSeries) => `${currentSeries.type}-${currentSeries.field ?? ''}`;

type NumberRefExpressionProps = {
  aggregationFunctions: any[];
  eventDefinition: any;
  expression: any;
  formattedFields: any[];
  onChange: (...args: any[]) => void;
  renderLabel: boolean;
  validation?: any;
  parent?: any
};

const NumberRefExpression = ({
  aggregationFunctions,
  formattedFields,
  eventDefinition,
  expression,
  onChange,
  renderLabel,
  validation = {},
}: NumberRefExpressionProps) => {
  const getSeries = useCallback((seriesId) => eventDefinition?.config?.series?.find((series) => series.id === seriesId), [eventDefinition?.config?.series]);

  const createSeries = useCallback(() => ({ id: expression.ref }), [expression.ref]);

  const getOrCreateSeries = useCallback((seriesId) => getSeries(seriesId) || createSeries(), [createSeries, getSeries]);

  const handleFieldChange = useCallback((key, value) => {
    const series = cloneDeep(eventDefinition?.config?.series);
    const nextSeries = cloneDeep(getOrCreateSeries(expression.ref));

    if (value !== undefined) {
      nextSeries[key] = value;
    }

    if (key === 'type' && value !== 'percentage') {
      delete nextSeries.strategy;
    }

    if (key === 'type' && value !== 'percentage') {
      delete nextSeries.percentile;
    }

    const nextSeriesId = getSeriesId(nextSeries);
    nextSeries.id = nextSeriesId;

    const seriesIndex = series.findIndex((s) => s.id === nextSeries.id);

    if (seriesIndex >= 0) {
      series[seriesIndex] = nextSeries;
    } else {
      series.push(nextSeries);
    }

    const nextExpression = cloneDeep(expression);

    nextExpression.ref = nextSeriesId;

    onChange({
      series: series,
      conditions: nextExpression,
    });
  }, [eventDefinition?.config?.series, expression, getOrCreateSeries, onChange]);

  const handleAggregationFunctionChange = useCallback((nextFunction) => {
    handleFieldChange('type', nextFunction);
  }, [handleFieldChange]);

  const handleAggregationFieldChange = useCallback((nextField) => {
    handleFieldChange('field', nextField);
  }, [handleFieldChange]);

  const series = getSeries(expression.ref) || {};

  const elements = ['percentage', 'percentile'].includes(series.type) ? 3 : 2;

  return (
    <Col md={6}>
      <FormGroup controlId="aggregation-function" validationState={validation.message ? 'error' : null}>
        {renderLabel && <ControlLabel>If</ControlLabel>}
        <Row className="row-sm">
          <Col md={12 / elements}>
            <Select className="aggregation-function"
                    matchProp="label"
                    placeholder="Select Function"
                    onChange={handleAggregationFunctionChange}
                    options={formatFunctions(aggregationFunctions)}
                    clearable={false}
                    value={series.type} />
          </Col>
          {series.type === 'percentage' && (
            <Col md={12 / elements}>
              <Select className="aggregation-function-strategy"
                      matchProp="label"
                      placeholder="Select Strategy (Optional)"
                      onChange={(newValue) => handleFieldChange('strategy', newValue)}
                      options={percentageStrategyOptions}
                      value={series.strategy} />
            </Col>
          )}
          <Col md={12 / elements}>
            <Select className="aggregation-function-field"
                    ignoreAccents={false}
                    matchProp="label"
                    placeholder="Select Field (Optional)"
                    onChange={handleAggregationFieldChange}
                    options={formattedFields}
                    value={series.field}
                    allowCreate />
          </Col>
          {series.type === 'percentile' && (
            <Col md={12 / elements}>
              <Select className="aggregation-function-percentile"
                      placeholder="Select Percentile"
                      onChange={(newValue) => handleFieldChange('percentile', newValue)}
                      options={percentileOptions}
                      value={series.percentile} />
            </Col>
          )}
        </Row>
        {validation.message && <HelpBlock>{validation.message}</HelpBlock>}
      </FormGroup>
    </Col>
  );
};

export default NumberRefExpression;
