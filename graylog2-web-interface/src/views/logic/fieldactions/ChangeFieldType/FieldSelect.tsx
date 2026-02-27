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
import React, { useCallback } from 'react';
import styled from 'styled-components';
import { useFormikContext } from 'formik';

import { Input } from 'components/bootstrap';
import { Select } from 'components/common';
import useIndexSetFieldTypesAll from 'views/logic/fieldactions/ChangeFieldType/hooks/useIndexSetFieldTypesAll';
import type { FormValues } from 'views/logic/fieldactions/ChangeFieldType/ChangeFieldTypeModal';

const StyledLabel = styled.h5`
  font-weight: bold;
  margin-bottom: 5px;
`;
const StyledSelect = styled(Select)`
  width: 400px;
  margin-bottom: 20px;
`;

type Props = {
  indexSetId: string;
  field: string;
  name: string;
};

const FieldSelect = ({ indexSetId, field, name }: Props) => {
  const { setFieldValue } = useFormikContext<FormValues>();

  const {
    data: { options, currentTypes },
    isLoading,
  } = useIndexSetFieldTypesAll(indexSetId);

  const _onFieldChange = useCallback(
    (value: string) => {
      setFieldValue(name, value);
      setFieldValue('field_type', currentTypes?.[value]);
    },
    [currentTypes, name, setFieldValue],
  );

  return (
    <>
      <StyledLabel>Select Field</StyledLabel>
      <Input id={name}>
        <StyledSelect
          inputId={name}
          name={name}
          options={options}
          value={field}
          onChange={_onFieldChange}
          placeholder="Select or type the field"
          disabled={isLoading}
          aria-label="Select Field"
          required
          allowCreate
        />
      </Input>
    </>
  );
};

export default FieldSelect;
