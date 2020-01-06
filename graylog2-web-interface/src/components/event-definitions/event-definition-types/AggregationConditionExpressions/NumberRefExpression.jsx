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
    .map(fn => ({ label: `${fn.toLowerCase()}()`, value: fn }));
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
    return eventDefinition.config.series.find(series => series.id === seriesId);
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

    const seriesIndex = series.findIndex(s => s.id === nextSeries.id);
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
      <FormGroup controlId="aggregation-function" validationState={validation.errors.series ? 'error' : null}>
        {renderLabel && <ControlLabel>If</ControlLabel>}
        <Row className="row-sm">
          <Col md={6}>
            <Select className="aggregation-function"
                    matchProp="label"
                    placeholder="Select Function"
                    onChange={handleAggregationFunctionChange}
                    options={formatFunctions(aggregationFunctions)}
                    value={series.function} />
          </Col>
          <Col md={6}>
            <Select className="aggregation-function-field"
                    matchProp="label"
                    placeholder="Select Field (Optional)"
                    onChange={handleAggregationFieldChange}
                    options={formattedFields}
                    value={series.field}
                    allowCreate
                    isClearable />
          </Col>
        </Row>
        {validation.errors.series && (
          <HelpBlock>{lodash.get(validation, 'errors.series[0]')}</HelpBlock>
        )}
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
  validation: PropTypes.object.isRequired,
};

export default NumberRefExpression;
