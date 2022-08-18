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
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { MultiSelect } from 'components/common';
import { Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/bootstrap';
// TODO: This should be moved to a general place outside of `views`
import { defaultCompare } from 'logic/DefaultCompare';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import { ALL_MESSAGES_TIMERANGE } from 'views/Constants';

import AggregationConditionsForm from './AggregationConditionsForm';

import commonStyles from '../common/commonStyles.css';

type EventDefinitionConfig = {
  group_by: Array<string>,
  streams: Array<string>,
};

type EventDefinition = {
  config: EventDefinitionConfig,
};

type Props = {
  eventDefinition: EventDefinition,
  validation: {},
  aggregationFunctions: Array<{}>,
  onChange: (key: string, newValue: any) => void,
};

const AggregationForm = ({ aggregationFunctions, eventDefinition, validation, onChange }: Props) => {
  const { data: allFieldTypes } = useFieldTypes(eventDefinition?.config?.streams ?? [], ALL_MESSAGES_TIMERANGE);
  // Memoize function to only format fields when they change. Use joined fieldNames as cache key.
  const formattedFields = useMemo(() => (allFieldTypes ?? [])
    .sort((ftA, ftB) => defaultCompare(ftA.name, ftB.name))
    .map((fieldType) => ({
      label: `${fieldType.name} – ${fieldType.value.type.type}`,
      value: fieldType.name,
    })), [allFieldTypes]);

  const propagateConfigChange = useCallback((update: Partial<EventDefinitionConfig>) => {
    const nextConfig = { ...eventDefinition.config, ...update };

    onChange('config', nextConfig);
  }, [eventDefinition.config, onChange]);

  const handleGroupByChange = useCallback((selected: string) => {
    const nextValue = selected === '' ? [] : selected.split(',');
    propagateConfigChange({ group_by: nextValue });
  }, [propagateConfigChange]);

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
                         onChange={handleGroupByChange}
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
                                 onChange={propagateConfigChange} />
    </fieldset>
  );
};

AggregationForm.propTypes = {
  eventDefinition: PropTypes.object.isRequired,
  validation: PropTypes.object.isRequired,
  aggregationFunctions: PropTypes.array.isRequired,
  onChange: PropTypes.func.isRequired,
};

export default AggregationForm;
