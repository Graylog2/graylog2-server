import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import naturalSort from 'javascript-natural-sort';
import uuid from 'uuid/v4';
import { Col, ControlLabel, FormGroup, HelpBlock, Row } from 'react-bootstrap';

import { MultiSelect, Select } from 'components/common';
import { Input } from 'components/bootstrap';

import FormsUtils from 'util/FormsUtils';
import AggregationExpressionParser from 'logic/alerts/AggregationExpressionParser';

import commonStyles from '../common/commonStyles.css';

class AggregationForm extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    allFieldTypes: PropTypes.array.isRequired,
    aggregationFunctions: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  // Memoize function to only format fields when they change. Use joined fieldNames as cache key.
  formatFields = lodash.memoize(
    (fieldTypes) => {
      return fieldTypes.map((fieldType) => {
        return {
          label: `${fieldType.name} – ${fieldType.value.type.type}`,
          value: fieldType.name,
        };
      });
    },
    fieldTypes => fieldTypes.map(ft => ft.name).join('-'),
  );

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
    const { eventDefinition } = this.props;
    const series = lodash.cloneDeep(eventDefinition.config.series);
    const nextSeries = { id: uuid() };
    series.push(nextSeries);
    this.propagateConfigChange('series', series);
    return { id: uuid() };
  };

  getOrCreateSeries = (config) => {
    return this.getSeries(config) || this.createSeries();
  };

  propagateConfigChange = (key, value) => {
    const { eventDefinition, onChange } = this.props;
    const nextConfig = lodash.cloneDeep(eventDefinition.config);
    nextConfig[key] = value;
    onChange('config', nextConfig);
  };

  handleGroupByChange = (nextValue) => {
    this.propagateConfigChange('group_by', nextValue);
  };

  handleAggregationFunctionChange = (nextFunction) => {
    const { eventDefinition } = this.props;
    const series = lodash.cloneDeep(eventDefinition.config.series);
    const nextSeries = lodash.cloneDeep(this.getOrCreateSeries(eventDefinition.config));
    nextSeries.function = nextFunction;
    series[0] = nextSeries;
    this.propagateConfigChange('series', series);
  };

  handleAggregationFieldChange = (nextField) => {
    const { eventDefinition } = this.props;
    const series = lodash.cloneDeep(eventDefinition.config.series);
    const nextSeries = lodash.cloneDeep(this.getOrCreateSeries(eventDefinition.config));
    nextSeries.field = nextField;
    series[0] = nextSeries;
    this.propagateConfigChange('series', series);
  };

  handleExpressionOperatorChange = (nextOperator) => {
    const { eventDefinition } = this.props;
    const series = this.getOrCreateSeries(eventDefinition.config);
    const { value } = AggregationExpressionParser.parseExpression(eventDefinition.config.conditions);

    const nextExpression = AggregationExpressionParser.generateExpression(series.id, nextOperator, value);
    this.propagateConfigChange('conditions', nextExpression);
  };

  handleExpressionThresholdChange = (event) => {
    const { eventDefinition } = this.props;
    const series = this.getOrCreateSeries(eventDefinition.config);
    const { operator } = AggregationExpressionParser.parseExpression(eventDefinition.config.conditions);
    const nextThreshold = FormsUtils.getValueFromInput(event.target);

    const nextExpression = AggregationExpressionParser.generateExpression(series.id, operator, nextThreshold);
    this.propagateConfigChange('conditions', nextExpression);
  };

  render() {
    const { allFieldTypes, aggregationFunctions, eventDefinition, validation } = this.props;
    const formattedFields = this.formatFields(allFieldTypes);
    const series = this.getSeries(eventDefinition.config) || {};
    const expressionResults = AggregationExpressionParser.parseExpression(eventDefinition.config.conditions);

    return (
      <fieldset>
        <h2 className={commonStyles.title}>Aggregation</h2>
        <p>
          Summarize log messages matching the Filter defined above by using a function. You can optionally group the
          Filter results by identical field values.
        </p>
        <Row>
          <Col lg={7}>
            <FormGroup controlId="group-by">
              <ControlLabel>Group by Field(s) <small className="text-muted">(Optional)</small></ControlLabel>
              <MultiSelect id="group-by"
                           matchProp="label"
                           onChange={selected => this.handleGroupByChange(selected === '' ? [] : selected.split(','))}
                           options={formattedFields}
                           value={lodash.defaultTo(eventDefinition.config.group_by, []).join(',')}
                           allowCreate />
              <HelpBlock>
                Select Fields Graylog should use to group messages.{' '}
                <b>Example:</b> Set <code>username</code> as Group by Field to create groups of messages with
                identical <code>username</code> values. That way you can set a condition that will be computed for
                each group with the same <code>username</code>.
              </HelpBlock>
            </FormGroup>
          </Col>
        </Row>

        <hr />

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
                      value={expressionResults.operator} />
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
                   value={lodash.defaultTo(expressionResults.value, 0)}
                   bsStyle={validation.errors.conditions ? 'error' : null}
                   help={lodash.get(validation, 'errors.conditions[0]', null)}
                   onChange={this.handleExpressionThresholdChange} />
          </Col>

        </Row>
      </fieldset>
    );
  }
}

export default AggregationForm;
