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
// @flow strict
import * as React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';

import { defaultCompare } from 'views/logic/DefaultCompare';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import CustomPropTypes from 'views/components/CustomPropTypes';
import SortableSelect from 'views/components/aggregationbuilder/SortableSelect';

const ValueComponent = styled.span`
  padding: 2px 5px;
`;

type Props = {
  onChange: (newFields: { label: string, value: string }[]) => void,
  fields: Immutable.List<FieldTypeMapping>,
  value: { field: string }[] | undefined | null,
  allowOptionCreation?: boolean,
  inputId?: string,
};

const FieldSelect = ({ fields, onChange, value, ...rest }: Props) => {
  const fieldsForSelect = fields
    .map((fieldType) => fieldType.name)
    .map((fieldName) => ({ label: fieldName, value: fieldName }))
    .valueSeq()
    .toJS()
    .sort((v1, v2) => defaultCompare(v1.label, v2.label));

  return (
    <SortableSelect {...rest}
                    options={fieldsForSelect}
                    onChange={onChange}
                    valueComponent={({ children: _children }) => <ValueComponent>{_children}</ValueComponent>}
                    value={value} />
  );
};

FieldSelect.propTypes = {
  onChange: PropTypes.func,
  fields: CustomPropTypes.FieldListType.isRequired,
  value: PropTypes.array,
};

FieldSelect.defaultProps = {
  onChange: () => {},
  value: null,
  allowOptionCreation: false,
  inputId: undefined,
};

export default FieldSelect;
