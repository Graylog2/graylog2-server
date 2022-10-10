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
import { useContext, useMemo } from 'react';
import * as Immutable from 'immutable';
import styled, { css } from 'styled-components';

import { defaultCompare } from 'logic/DefaultCompare';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import { Input } from 'components/bootstrap';
import Select from 'components/common/Select';
import { useStore } from 'stores/connect';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import type { Property } from 'views/logic/fieldtypes/FieldType';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldTypeIcon from 'views/components/sidebar/fields/FieldTypeIcon';

type Props = {
  ariaLabel?: string,
  clearable?: boolean,
  error?: string,
  id: string,
  label: string,
  name: string,
  onChange: (changeEvent: { target: { name: string, value: string } }) => void,
  value: string | undefined,
  selectRef?: React.Ref<React.ComponentType>
  properties?: Array<Property>,
}

const sortByLabel = ({ label: label1 }: { label: string }, { label: label2 }: { label: string }) => defaultCompare(label1, label2);

const hasProperty = (fieldType: FieldTypeMapping, properties: Array<Property>) => {
  const fieldProperties = fieldType?.type?.properties ?? Immutable.Set();

  return properties
    .map((property) => fieldProperties.contains(property))
    .find((result) => result === false) === undefined;
};

const UnqualifiedOption = styled.span(({ theme }) => css`
  color: ${theme.colors.variant.light.default};
`);

type OptionRendererProps = {
  label: string,
  qualified: boolean,
  type: string,
};

const OptionRenderer = ({ label, qualified, type }: OptionRendererProps) => {
  const children = <><FieldTypeIcon type={type} /> {label}</>;

  return qualified ? <span>{children}</span> : <UnqualifiedOption>{children}</UnqualifiedOption>;
};

const FieldSelect = ({ name, id, error, clearable, value, onChange, label, ariaLabel, selectRef, properties }: Props) => {
  const { activeQuery } = useStore(ViewMetadataStore);
  const fieldTypes = useContext(FieldTypesContext);
  const fieldTypeOptions = useMemo(() => fieldTypes.queryFields
    .get(activeQuery, Immutable.List())
    .map((fieldType) => ({ label: fieldType.name, value: fieldType.name, type: fieldType.type, qualified: properties ? hasProperty(fieldType, properties) : true }))
    .toArray()
    .sort(sortByLabel), [activeQuery, fieldTypes.queryFields, properties]);

  return (
    <Input id={id}
           label={label}
           error={error}
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9">
      <Select options={fieldTypeOptions}
              inputId={`select-${id}`}
              forwardedRef={selectRef}
              clearable={clearable}
              placeholder="Select field"
              name={name}
              value={value}
              aria-label={ariaLabel}
              optionRenderer={OptionRenderer}
              size="small"
              menuPortalTarget={document.body}
              onChange={(newValue: string) => onChange({ target: { name, value: newValue } })} />
    </Input>

  );
};

FieldSelect.defaultProps = {
  clearable: false,
  error: undefined,
  ariaLabel: undefined,
  selectRef: undefined,
  properties: undefined,
};

export default FieldSelect;
