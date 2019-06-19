import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Checkbox, Col, ControlLabel, FormGroup, HelpBlock, Row } from 'react-bootstrap';

import { MultiSelect, Select, TimeUnitInput } from 'components/common';
import { Input } from 'components/bootstrap';

import FormsUtils from 'util/FormsUtils';

import commonStyles from '../common/commonStyles.css';
import styles from './AggregationForm.css';

class AggregationForm extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    allFieldTypes: PropTypes.array.isRequired,
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

  handleConfigChange = (event) => {
    const { eventDefinition, onChange } = this.props;
    const { name } = event.target;
    const config = lodash.cloneDeep(eventDefinition.config);
    config[name] = FormsUtils.getValueFromInput(event.target);
    onChange('config', config);
  };

  handleGroupByChange = (nextValue) => {
    const { eventDefinition, onChange } = this.props;
    const config = lodash.cloneDeep(eventDefinition.config);
    config.group_by = nextValue;
    onChange('config', config);
  };

  handleCustomTimerangeChange = (nextValue, nextUnit) => {
    const { eventDefinition, onChange } = this.props;
    const config = lodash.cloneDeep(eventDefinition.config);
    config.aggregation_timerange = {
      value: nextValue,
      unit: nextUnit,
    };
    onChange('config', config);
  };

  render() {
    const { allFieldTypes, eventDefinition } = this.props;
    const useScheduleTimerange = lodash.defaultTo(eventDefinition.config.use_schedule_timerange, true);
    const aggregationTimerange = lodash.defaultTo(eventDefinition.config.aggregation_timerange, {});

    return (
      <fieldset>
        <h2 className={commonStyles.title}>Aggregation</h2>
        <Row>
          <Col lg={7}>
            <FormGroup controlId="group-by">
              <ControlLabel>Group by Field(s) <small className="text-muted">(Optional)</small></ControlLabel>
              <MultiSelect id="group-by"
                           matchProp="label"
                           onChange={selected => this.handleGroupByChange(selected === '' ? [] : selected.split(','))}
                           options={this.formatFields(allFieldTypes)}
                           value={lodash.defaultTo(eventDefinition.config.group_by, []).join(',')} />
              <HelpBlock>Aggregate on groups of identical Field values. Example: count failed login attempts per username.</HelpBlock>
            </FormGroup>
          </Col>
        </Row>

        <hr />

        <h3 className={commonStyles.title}>Trigger Alert</h3>
        <Row className="row-sm">
          <Col md={4}>
            <FormGroup controlId="aggregation-function">
              <ControlLabel>If</ControlLabel>
              <Select id="aggregation-function"
                      matchProp="label"
                      placeholder="Select Aggregation"
                      onChange={() => {}}
                      options={[]}
                      value={eventDefinition.config.aggregation} />
            </FormGroup>
          </Col>
          <Col md={4}>
            <FormGroup controlId="aggregation-condition">
              <ControlLabel>Is</ControlLabel>
              <Select id="aggregation-condition"
                      matchProp="label"
                      placeholder="Select Condition"
                      onChange={() => {}}
                      options={[
                        { label: '<', value: 'less' },
                        { label: '>', value: 'greater' },
                        { label: '=', value: 'equals' },
                        { label: '≠', value: 'not-equal' },
                      ]}
                      value={eventDefinition.config.condition} />
            </FormGroup>
          </Col>
          <Col md={4}>
            <Input id="aggregation-threshold"
                   name="threshold"
                   label="Threshold"
                   type="number"
                   value={lodash.defaultTo(eventDefinition.config.threshold, 0)}
                   onChange={this.handleConfigChange} />
          </Col>
        </Row>

        <Row className="row-sm">
          <Col md={6}>
            <TimeUnitInput label="In the last"
                           update={this.handleCustomTimerangeChange}
                           enabled={!useScheduleTimerange}
                           value={lodash.defaultTo(aggregationTimerange.value, 1)}
                           unit={aggregationTimerange.unit}
                           units={['SECONDS', 'MINUTES', 'HOURS', 'DAYS']}
                           hideCheckbox />
          </Col>
          <Col md={6}>
            <FormGroup className={styles.checkbox}>
              <Checkbox name="use_schedule_timerange"
                        checked={useScheduleTimerange}
                        onChange={this.handleConfigChange}
                        inline>
                Use schedule time range
              </Checkbox>
              <span className={styles.helpButton}>
                <i className="fa fa-question-circle" />
              </span>
            </FormGroup>
          </Col>
        </Row>

      </fieldset>
    );
  }
}

export default AggregationForm;
