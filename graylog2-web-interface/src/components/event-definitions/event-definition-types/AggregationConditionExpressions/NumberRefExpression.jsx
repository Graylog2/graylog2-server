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
import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import naturalSort from 'javascript-natural-sort';

import { Select } from 'components/common';
import { Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/graylog';
import { numberRefNodePropType } from 'logic/alerts/AggregationExpressionTypes';

const formatFunctions = (functions) => {
  return functions
    .sort(naturalSort)
    .map((fn) => ({ label: `${fn.toLowerCase()}()`, value: fn }));
};

const NumberRefExpression = ({
  aggregationFunctions,
  formattedFields,
  eventDefinition,
  expression,
  onChange,
  renderLabel,
  validation,
}) => {
  const getSeries = (seriesId) => {
    return eventDefinition.config.series.find((series) => series.id === seriesId);
  };

  const createSeries = () => {
    return { id: expression.ref };
  };

  const getOrCreateSeries = (seriesId) => {
    return getSeries(seriesId) || createSeries();
  };

  const getSeriesId = (currentSeries, func, field) => {
    return `${lodash.defaultTo(func, currentSeries.function)}-${lodash.defaultTo(field, currentSeries.field || '')}`;
  };

  const handleFieldChange = ({ nextFunction, nextField }) => {
    const series = lodash.cloneDeep(eventDefinition.config.series);
    const nextSeries = lodash.cloneDeep(getOrCreateSeries(expression.ref));
    const nextSeriesId = getSeriesId(nextSeries, nextFunction, nextField);

    if (nextFunction !== undefined) {
      nextSeries.function = nextFunction;
    }

    if (nextField !== undefined) {
      nextSeries.field = nextField;
    }

    nextSeries.id = nextSeriesId;

    const seriesIndex = series.findIndex((s) => s.id === nextSeries.id);

    if (seriesIndex >= 0) {
      series[seriesIndex] = nextSeries;
    } else {
      series.push(nextSeries);
    }

    const nextExpression = lodash.cloneDeep(expression);

    nextExpression.ref = nextSeriesId;

    onChange({
      series: series,
      conditions: nextExpression,
    });
  };

  const handleAggregationFunctionChange = (nextFunction) => {
    handleFieldChange({ nextFunction });
  };

  const handleAggregationFieldChange = (nextField) => {
    handleFieldChange({ nextField });
  };

  const series = getSeries(expression.ref) || {};

  return (
    <Col md={6}>
      <FormGroup controlId="aggregation-function" validationState={validation.message ? 'error' : null}>
        {renderLabel && <ControlLabel>If</ControlLabel>}
        <Row className="row-sm">
          <Col md={6}>
            <Select className="aggregation-function"
                    matchProp="label"
                    placeholder="Select Function"
                    onChange={handleAggregationFunctionChange}
                    options={formatFunctions(aggregationFunctions)}
                    clearable={false}
                    value={series.function} />
          </Col>
          <Col md={6}>
            <Select className="aggregation-function-field"
                    ignoreAccents={false}
                    matchProp="label"
                    placeholder="Select Field (Optional)"
                    onChange={handleAggregationFieldChange}
                    options={formattedFields}
                    value={series.field}
                    allowCreate />
          </Col>
        </Row>
        {validation.message && <HelpBlock>{validation.message}</HelpBlock>}
      </FormGroup>
    </Col>
  );
};

NumberRefExpression.propTypes = {
  aggregationFunctions: PropTypes.array.isRequired,
  eventDefinition: PropTypes.object.isRequired,
  expression: numberRefNodePropType.isRequired,
  formattedFields: PropTypes.array.isRequired,
  onChange: PropTypes.func.isRequired,
  renderLabel: PropTypes.bool.isRequired,
  validation: PropTypes.object,
};

NumberRefExpression.defaultProps = {
  validation: {},
};

export default NumberRefExpression;
