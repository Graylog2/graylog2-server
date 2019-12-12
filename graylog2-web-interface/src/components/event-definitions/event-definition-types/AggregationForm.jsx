import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import naturalSort from 'javascript-natural-sort';
import uuid from 'uuid/v4';
import { Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/graylog';

import { MultiSelect, Select } from 'components/common';
import { Input } from 'components/bootstrap';

import FormsUtils from 'util/FormsUtils';
import AggregationExpressionParser from 'logic/alerts/AggregationExpressionParser';
import { naturalSortIgnoreCase } from 'util/SortUtils';

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
      return fieldTypes
        .sort((ftA, ftB) => naturalSortIgnoreCase(ftA.name, ftB.name))
        .map((fieldType) => {
          return {
            label: `${fieldType.name} – ${fieldType.value.type.type}`,
            value: fieldType.name,
          };
        });
    },
    fieldTypes => fieldTypes.map(ft => ft.name).join('-'),
  );

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
    this.setState({ thresholdOperator: nextOperator });
  };

  handleExpressionThresholdChange = (event) => {
    const { eventDefinition } = this.props;
    const series = this.getOrCreateSeries(eventDefinition.config);
    const { operator } = AggregationExpressionParser.parseExpression(eventDefinition.config.conditions);
    const nextThreshold = event.target.value === '' ? '' : FormsUtils.getValueFromInput(event.target);

    const nextExpression = AggregationExpressionParser.generateExpression(series.id, operator, Number(nextThreshold));
    this.propagateConfigChange('conditions', nextExpression);
    this.setState({ thresholdValue: nextThreshold });
  };

  render() {
    const { allFieldTypes, aggregationFunctions, eventDefinition, validation } = this.props;
    const { thresholdOperator, thresholdValue } = this.state;
    const formattedFields = this.formatFields(allFieldTypes);
    const series = this.getSeries(eventDefinition.config) || {};

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
                Select Fields that Graylog should use to group Filter results when they have identical values.
                {' '}<b>Example:</b><br />
                Assuming you created a Filter with all failed log-in attempts in your network, Graylog could alert you
                when there are more than 5 failed log-in attempts overall. Now, add <code>username</code> as Group by
                Field and Graylog will alert you <em>for each <code>username</code></em> with more than 5 failed
                log-in attempts.
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
                          allowCreate
                          isClearable />
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
      </fieldset>
    );
  }
}

export default AggregationForm;
