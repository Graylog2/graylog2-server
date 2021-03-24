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

import { Input } from 'components/bootstrap';
import { Checkbox, HelpBlock } from 'components/graylog';
import { FieldComponentProps } from 'views/components/aggregationwizard/elementConfiguration/VisualizationConfigurationOptions';

const BooleanField = ({ field, name, onChange, error, title, value }: FieldComponentProps) => (
  <>
    <Input id={`${name}-input`}
           label={title}
           error={error}
           labelClassName="col-sm-11"
           wrapperClassName="col-sm-1">
      <Checkbox id={`${name}-input`} name={name} onChange={onChange} defaultValue={value} />
    </Input>
    <HelpBlock>{field.description}</HelpBlock>
  </>
);

export default BooleanField;
