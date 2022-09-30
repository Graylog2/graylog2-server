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
import { useFormikContext } from 'formik';
import styled from 'styled-components';

import Button from 'components/bootstrap/Button';
import { Spinner } from 'components/common';
import { ButtonToolbar } from 'components/bootstrap';
import AggregationElementSelect from 'views/components/aggregationwizard/AggregationElementSelect';

import aggregationElements from './aggregationElementDefinitions';
import type { WidgetConfigFormValues } from './WidgetConfigForm';

const aggregationElementsByKey = Object.fromEntries(aggregationElements.map((element) => ([element.key, element])));

const StyledButtonToolbar = styled(ButtonToolbar)`
  margin-bottom: 10px
`;

const onCreateElement = (
  elementKey: string,
  values: WidgetConfigFormValues,
  setValues: (formValues: WidgetConfigFormValues) => void,
) => {
  const aggregationElement = aggregationElementsByKey[elementKey];

  if (aggregationElement?.onCreate) {
    setValues(aggregationElement.onCreate(values));
  } else {
    setValues({
      ...values,
      [elementKey]: [
        ...(values[elementKey] ?? []),
        {},
      ],
    });
  }
};

const SelectContainer = styled.div`
  margin-left: 5px;
`;

const ElementsConfigurationActions = () => {
  const { dirty, isSubmitting: isUpdatingPreview, isValid, values, setValues } = useFormikContext<WidgetConfigFormValues>();

  return (
    <StyledButtonToolbar>
      <SelectContainer data-testid="add-element-section" className="pull-left">
        <AggregationElementSelect onSelect={(elementKey) => onCreateElement(elementKey, values, setValues)}
                                  aggregationElements={aggregationElements}
                                  formValues={values} />
      </SelectContainer>

      <Button bsStyle="info" className="pull-right" type="submit" disabled={!isValid || isUpdatingPreview || !dirty}>
        {isUpdatingPreview ? <Spinner text="Updating preview..." delay={0} /> : 'Update preview'}
      </Button>
    </StyledButtonToolbar>
  );
};

export default ElementsConfigurationActions;
