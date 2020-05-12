import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/graylog';

// TODO: This should be moved to a general place outside of `views`
import { defaultCompare } from 'views/logic/DefaultCompare';
import { MultiSelect } from 'components/common';

import AggregationConditionsForm from './AggregationConditionsForm';

import commonStyles from '../common/commonStyles.css';

class AggregationForm extends React.Component {
  // Memoize function to only format fields when they change. Use joined fieldNames as cache key.
  formatFields = lodash.memoize(
    (fieldTypes) => {
      return fieldTypes
        .sort((ftA, ftB) => defaultCompare(ftA.name, ftB.name))
        .map((fieldType) => {
          return {
            label: `${fieldType.name} – ${fieldType.value.type.type}`,
            value: fieldType.name,
          };
        });
    },
    (fieldTypes) => fieldTypes.map((ft) => ft.name).join('-'),
  );

  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    allFieldTypes: PropTypes.array.isRequired,
    aggregationFunctions: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  propagateConfigChange = (update) => {
    const { eventDefinition, onChange } = this.props;
    const nextConfig = { ...eventDefinition.config, ...update };
    onChange('config', nextConfig);
  };

  handleGroupByChange = (nextValue) => {
    this.propagateConfigChange({ group_by: nextValue });
  };

  render() {
    const { allFieldTypes, aggregationFunctions, eventDefinition, validation } = this.props;
    const formattedFields = this.formatFields(allFieldTypes);

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
                           onChange={(selected) => this.handleGroupByChange(selected === '' ? [] : selected.split(','))}
                           options={formattedFields}
                           ignoreAccents={false}
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

        <AggregationConditionsForm eventDefinition={eventDefinition}
                                   validation={validation}
                                   formattedFields={formattedFields}
                                   aggregationFunctions={aggregationFunctions}
                                   onChange={this.propagateConfigChange} />
      </fieldset>
    );
  }
}

export default AggregationForm;
