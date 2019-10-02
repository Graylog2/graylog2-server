import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import naturalSort from 'javascript-natural-sort';
import uuid from 'uuid/v4';

import { Input } from 'components/bootstrap';
import { Select } from 'components/common';
import { Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/graylog';

import FormsUtils from 'util/FormsUtils';
import AggregationExpressionParser from 'logic/alerts/AggregationExpressionParser';

import commonStyles from '../common/commonStyles.css';

class AggregationConditionsForm extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    formattedFields: PropTypes.array.isRequired,
    aggregationFunctions: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);

    const expressionResults = AggregationExpressionParser.parseExpression(props.eventDefinition.config.conditions);

    this.state = {
      thresholdOperator: expressionResults.operator,
      thresholdValue: lodash.defaultTo(expressionResults.value, 0),
    };
  }

  formatFunctions = (functions) => {
    return functions
      .sort(naturalSort)
      .map(fn => ({ label: `${fn.toLowerCase()}()`, value: fn }));
  };

  getSeries = (config) => {
    // For now we only support one series in the UI
    return config.series[0];
  };

  createSeries = () => {
    const { eventDefinition, onChange } = this.props;
    const series = lodash.cloneDeep(eventDefinition.config.series);
    const nextSeries = { id: uuid() };
    series.push(nextSeries);
    onChange('series', series);
    return { id: uuid() };
  };

  getOrCreateSeries = (config) => {
    return this.getSeries(config) || this.createSeries();
  };

  handleExpressionOperatorChange = (nextOperator) => {
    const { eventDefinition, onChange } = this.props;
    const series = this.getOrCreateSeries(eventDefinition.config);
    const { value } = AggregationExpressionParser.parseExpression(eventDefinition.config.conditions);

    const nextExpression = AggregationExpressionParser.generateExpression(series.id, nextOperator, value);
    onChange('conditions', nextExpression);
    this.setState({ thresholdOperator: nextOperator });
  };

  handleExpressionThresholdChange = (event) => {
    const { eventDefinition, onChange } = this.props;
    const series = this.getOrCreateSeries(eventDefinition.config);
    const { operator } = AggregationExpressionParser.parseExpression(eventDefinition.config.conditions);
    const nextThreshold = event.target.value === '' ? '' : FormsUtils.getValueFromInput(event.target);

    const nextExpression = AggregationExpressionParser.generateExpression(series.id, operator, Number(nextThreshold));
    onChange('conditions', nextExpression);
    this.setState({ thresholdValue: nextThreshold });
  };

  handleAggregationFunctionChange = (nextFunction) => {
    const { eventDefinition, onChange } = this.props;
    const series = lodash.cloneDeep(eventDefinition.config.series);
    const nextSeries = lodash.cloneDeep(this.getOrCreateSeries(eventDefinition.config));
    nextSeries.function = nextFunction;
    series[0] = nextSeries;
    onChange('series', series);
  };

  handleAggregationFieldChange = (nextField) => {
    const { eventDefinition, onChange } = this.props;
    const series = lodash.cloneDeep(eventDefinition.config.series);
    const nextSeries = lodash.cloneDeep(this.getOrCreateSeries(eventDefinition.config));
    nextSeries.field = nextField;
    series[0] = nextSeries;
    onChange('series', series);
  };

  render() {
    const { formattedFields, aggregationFunctions, eventDefinition, validation } = this.props;
    const { thresholdOperator, thresholdValue } = this.state;
    const series = this.getSeries(eventDefinition.config) || {};

    return (
      <React.Fragment>
        <h3 className={commonStyles.title}>Create Events for Definition</h3>

        <Row className="row-sm">
          <Col md={6}>
            <FormGroup controlId="aggregation-function" validationState={validation.errors.series ? 'error' : null}>
              <ControlLabel>If</ControlLabel>
              <Row className="row-sm">
                <Col md={6}>
                  <Select id="aggregation-function"
                          matchProp="label"
                          placeholder="Select Function"
                          onChange={this.handleAggregationFunctionChange}
                          options={this.formatFunctions(aggregationFunctions)}
                          value={series.function} />
                </Col>
                <Col md={6}>
                  <Select id="aggregation-function-field"
                          matchProp="label"
                          placeholder="Select Field (Optional)"
                          onChange={this.handleAggregationFieldChange}
                          options={formattedFields}
                          value={series.field}
                          allowCreate />
                </Col>
              </Row>
              {validation.errors.series && (
                <HelpBlock>{lodash.get(validation, 'errors.series[0]')}</HelpBlock>
              )}
            </FormGroup>
          </Col>
          <Col md={3}>
            <FormGroup controlId="aggregation-condition" validationState={validation.errors.conditions ? 'error' : null}>
              <ControlLabel>Is</ControlLabel>
              <Select id="aggregation-condition"
                      matchProp="label"
                      placeholder="Select Condition"
                      onChange={this.handleExpressionOperatorChange}
                      options={[
                        { label: '<', value: '<' },
                        { label: '<=', value: '<=' },
                        { label: '>', value: '>' },
                        { label: '>=', value: '>=' },
                        { label: '=', value: '==' },
                      ]}
                      value={thresholdOperator} />
              {validation.errors.conditions && (
                <HelpBlock>{lodash.get(validation, 'errors.conditions[0]')}</HelpBlock>
              )}
            </FormGroup>
          </Col>
          <Col md={3}>
            <Input id="aggregation-threshold"
                   name="threshold"
                   label="Threshold"
                   type="number"
                   value={thresholdValue}
                   bsStyle={validation.errors.conditions ? 'error' : null}
                   help={lodash.get(validation, 'errors.conditions[0]', null)}
                   onChange={this.handleExpressionThresholdChange} />
          </Col>
        </Row>
      </React.Fragment>
    );
  }
}

export default AggregationConditionsForm;
