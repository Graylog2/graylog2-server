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
import { Field } from 'formik';
import styled from 'styled-components';

import { FieldComponentProps } from 'views/components/aggregationwizard/elementConfiguration/VisualizationConfigurationOptions';
import { Input } from 'components/bootstrap';
import { HelpBlock } from 'components/graylog';

const StyledField = styled(Field)`
  &&[type="checkbox"] {
    margin-top: 8px;
  }
`;

const BooleanField = ({ field, name, error, title }: FieldComponentProps) => (
  <>
    <Input id={`${name}-input`}
           label={title}
           error={error}
           labelClassName="col-sm-11"
           wrapperClassName="col-sm-1">
      <StyledField type="checkbox"
                   className="pull-right"
                   aria-label={field.title}
                   name={name} />
    </Input>
    <HelpBlock>{field.description}</HelpBlock>
  </>
);

export default BooleanField;
