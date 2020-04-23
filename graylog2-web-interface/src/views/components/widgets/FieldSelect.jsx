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
  onChange: ({ label: string, value: string }[]) => void,
  fields: Immutable.List<FieldTypeMapping>,
  value: ?{field: string}[],
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
};

export default FieldSelect;
