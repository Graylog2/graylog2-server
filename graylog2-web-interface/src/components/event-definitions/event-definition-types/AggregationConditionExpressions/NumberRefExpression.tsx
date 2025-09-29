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
import { useCallback, useMemo } from 'react';
import cloneDeep from 'lodash/cloneDeep';
import * as Immutable from 'immutable';
import styled, { css } from 'styled-components';

import { defaultCompare as naturalSort, defaultCompare } from 'logic/DefaultCompare';
import { Select } from 'components/common';
import { Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/bootstrap';
import { percentileOptions, percentageStrategyOptions } from 'views/Constants';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import { Properties, type Property } from 'views/logic/fieldtypes/FieldType';
import FieldTypeIcon from 'views/components/sidebar/fields/FieldTypeIcon';

const formatFunctions = (functions) =>
  functions.sort(naturalSort).map((fn) => ({ label: `${fn.toLowerCase()}()`, value: fn }));

const getSeriesId = (currentSeries) => `${currentSeries.type}-${currentSeries.field ?? ''}`;

type NumberRefExpressionProps = {
  aggregationFunctions: any[];
  eventDefinition: any;
  expression: any;
  formattedFields: FieldTypeMapping[];
  onChange: (...args: any[]) => void;
  renderLabel: boolean;
  validation?: any;
};

// start - copied from FieldSelectBase.tsx
const FieldName = styled.span`
  display: inline-flex;
  gap: 2px;
  align-items: center;
`;

const UnqualifiedOption = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.gray[70]};
  `,
);

type OptionRendererProps = {
  label: string;
  qualified: boolean;
  type?: FieldType;
};

const OptionRenderer = ({ label, qualified, type = undefined }: OptionRendererProps) => {
  const children = (
    <FieldName>
      {type && (
        <>
          <FieldTypeIcon type={type} />{' '}
        </>
      )}
      {label}
    </FieldName>
  );

  return qualified ? <span>{children}</span> : <UnqualifiedOption>{children}</UnqualifiedOption>;
};

const sortByLabel = ({ label: label1 }: { label: string }, { label: label2 }: { label: string }) =>
  defaultCompare(label1, label2);
// end - copied from FieldSelectBase.tsx

const NumberRefExpression = ({
  aggregationFunctions,
  formattedFields,
  eventDefinition,
  expression,
  onChange,
  renderLabel,
  validation = {},
}: NumberRefExpressionProps) => {
  const getSeries = useCallback(
    (seriesId) => eventDefinition?.config?.series?.find((series) => series.id === seriesId),
    [eventDefinition?.config?.series],
  );

  const createSeries = useCallback(() => ({ id: expression.ref }), [expression.ref]);

  const getOrCreateSeries = useCallback((seriesId) => getSeries(seriesId) || createSeries(), [createSeries, getSeries]);

  const handleFieldChange = useCallback(
    (key, value) => {
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
    },
    [eventDefinition?.config?.series, expression, getOrCreateSeries, onChange],
  );

  const handleAggregationFunctionChange = useCallback(
    (nextFunction) => {
      handleFieldChange('type', nextFunction);
    },
    [handleFieldChange],
  );

  const handleAggregationFieldChange = useCallback(
    (nextField) => {
      handleFieldChange('field', nextField);
    },
    [handleFieldChange],
  );

  const series = getSeries(expression.ref) || {};

  const elements = ['percentage', 'percentile'].includes(series.type) ? 3 : 2;

  // start - copied from MetricConfiguration.tsx
  const hasProperty = (fieldType: FieldTypeMapping, properties: Array<Property>) => {
    const fieldProperties = fieldType?.type?.properties ?? Immutable.Set();

    return (
      properties.map((property) => fieldProperties.contains(property)).find((result) => result === false) === undefined
    );
  };

  const currentFunction = series.type;
  const isPercentage = currentFunction === 'percentage';
  const requiresNumericField =
    (isPercentage && series.strategy === 'SUM') || !['card', 'count', 'latest', 'percentage'].includes(currentFunction);

  const isFieldQualified = useCallback(
    (field: FieldTypeMapping) => {
      if (!requiresNumericField) {
        return true;
      }

      return hasProperty(field, [Properties.Numeric]);
    },
    [requiresNumericField],
  );

  const fieldOptions = useMemo(
    () =>
      formattedFields
        .map((field) => ({
          label: `${field.name} – ${field.value.type.type}`,
          value: field.name,
          type: field.value.type.type,
          qualified: isFieldQualified(field),
        }))
        .sort(sortByLabel),
    [isFieldQualified, formattedFields],
  );
  // end - copied from MetricConfiguration.tsx

  return (
    <Col md={6}>
      <FormGroup controlId="aggregation-function" validationState={validation.message ? 'error' : null}>
        {renderLabel && <ControlLabel>If</ControlLabel>}
        <Row className="row-sm">
          <Col md={12 / elements}>
            <Select
              className="aggregation-function"
              placeholder="Select Function"
              onChange={handleAggregationFunctionChange}
              options={formatFunctions(aggregationFunctions)}
              clearable={false}
              value={series.type}
            />
          </Col>
          {series.type === 'percentage' && (
            <Col md={12 / elements}>
              <Select
                className="aggregation-function-strategy"
                placeholder="Select Strategy (Optional)"
                onChange={(newValue) => handleFieldChange('strategy', newValue)}
                options={percentageStrategyOptions}
                value={series.strategy}
              />
            </Col>
          )}
          <Col md={12 / elements}>
            <Select
              className="aggregation-function-field"
              ignoreAccents={false}
              placeholder="Select Field (Optional)"
              onChange={handleAggregationFieldChange}
              options={fieldOptions}
              optionRenderer={OptionRenderer}
              value={series.field}
              allowCreate
            />
          </Col>
          {series.type === 'percentile' && (
            <Col md={12 / elements}>
              <Select
                className="aggregation-function-percentile"
                placeholder="Select Percentile"
                onChange={(newValue) => handleFieldChange('percentile', newValue)}
                options={percentileOptions}
                value={series.percentile}
              />
            </Col>
          )}
        </Row>
        {validation.message && <HelpBlock>{validation.message}</HelpBlock>}
      </FormGroup>
    </Col>
  );
};

export default NumberRefExpression;
