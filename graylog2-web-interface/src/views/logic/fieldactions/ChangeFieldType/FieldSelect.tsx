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

import { Input } from 'components/bootstrap';
import { Select } from 'components/common';
import useIndexSetFieldTypesAll from 'views/logic/fieldactions/ChangeFieldType/hooks/useIndexSetFieldTypesAll';

const StyledLabel = styled.h5`
  font-weight: bold;
  margin-bottom: 5px;
`;
const StyledSelect = styled(Select)`
  width: 400px;
  margin-bottom: 20px;
`;

type Props = {
  indexSetId: string,
  onFieldChange: (param: {
    fieldName: string,
    type: string
  }) => void,
  field: string,
}

const FieldSelect = ({ indexSetId, onFieldChange, field }: Props) => {
  const { data: { options, currentTypes }, isLoading } = useIndexSetFieldTypesAll(indexSetId);

  const _onFieldChange = useCallback((value: string) => {
    onFieldChange({ fieldName: value, type: currentTypes?.[value] });
  }, [currentTypes, onFieldChange]);

  return (
    <>
      <StyledLabel>Select Field</StyledLabel>
      <Input id="field">
        <StyledSelect inputId="field"
                      options={options}
                      value={field}
                      onChange={_onFieldChange}
                      placeholder="Select or type the field"
                      disabled={isLoading}
                      inputProps={{ 'aria-label': 'Select Field' }}
                      required
                      allowCreate />
      </Input>
    </>
  );
};

export default FieldSelect;
