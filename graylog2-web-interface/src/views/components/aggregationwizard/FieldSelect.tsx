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
import Select from 'components/common/Select';
import type { Property } from 'views/logic/fieldtypes/FieldType';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldTypeIcon from 'views/components/sidebar/fields/FieldTypeIcon';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import useActiveQueryId from 'views/hooks/useActiveQueryId';

const FieldName = styled.span`
  display: inline-flex;
  gap: 2px;
  align-items: center;
`;

type Props = {
  autoFocus?: boolean,
  ariaLabel?: string,
  clearable?: boolean,
  qualifiedTypeCategory?: 'time' | 'values',
  id: string,
  name: string,
  onChange: (fieldName: string) => void,
  placeholder?: string,
  className?: string,
  properties?: Array<Property>,
  selectRef?: React.Ref<React.ComponentType>
  value: string | undefined,
  persistSelection?: boolean,
  openMenuOnFocus?: boolean,
  onMenuClose?: () => void,
  excludedFields?: Array<string>,
}

const sortByLabel = ({ label: label1 }: { label: string }, { label: label2 }: { label: string }) => defaultCompare(label1, label2);

const hasProperty = (fieldType: FieldTypeMapping, properties: Array<Property>) => {
  const fieldProperties = fieldType?.type?.properties ?? Immutable.Set();

  return properties
    .map((property) => fieldProperties.contains(property))
    .find((result) => result === false) === undefined;
};

const isFieldQualified = (field: FieldTypeMapping, properties: Array<Property>, qualifiedTypeCategory: 'time' | 'values' | undefined) => {
  if (properties) {
    return hasProperty(field, properties);
  }

  if (qualifiedTypeCategory) {
    const fieldTypeCategory = field.type.type === 'date' ? 'time' : 'values';

    return qualifiedTypeCategory === fieldTypeCategory;
  }

  return true;
};

const UnqualifiedOption = styled.span(({ theme }) => css`
  color: ${theme.colors.variant.light.default};
`);

type OptionRendererProps = {
  label: string,
  qualified: boolean,
  type: FieldType,
};

const OptionRenderer = ({ label, qualified, type }: OptionRendererProps) => {
  const children = <FieldName><FieldTypeIcon type={type} /> {label}</FieldName>;

  return qualified ? <span>{children}</span> : <UnqualifiedOption>{children}</UnqualifiedOption>;
};

const FieldSelect = ({
  ariaLabel,
  autoFocus,
  className,
  clearable,
  excludedFields,
  qualifiedTypeCategory,
  id,
  name,
  onChange,
  onMenuClose,
  openMenuOnFocus,
  persistSelection,
  placeholder,
  properties,
  selectRef,
  value,
}: Props) => {
  const activeQuery = useActiveQueryId();
  const fieldTypes = useContext(FieldTypesContext);

  const fieldOptions = useMemo(() => fieldTypes.queryFields
    .get(activeQuery, Immutable.List())
    .filter((field) => !excludedFields.includes(field.name))
    .map((field) => ({
      label: field.name,
      value: field.name,
      type: field.type,
      qualified: isFieldQualified(field, properties, qualifiedTypeCategory),
    }))
    .toArray()
    .sort(sortByLabel), [activeQuery, excludedFields, fieldTypes.queryFields, properties, qualifiedTypeCategory]);

  return (
    <Select options={fieldOptions}
            inputId={`select-${id}`}
            forwardedRef={selectRef}
            className={className}
            onMenuClose={onMenuClose}
            openMenuOnFocus={openMenuOnFocus}
            persistSelection={persistSelection}
            clearable={clearable}
            placeholder={placeholder}
            name={name}
            value={value}
            aria-label={ariaLabel}
            optionRenderer={OptionRenderer}
            size="small"
            autoFocus={autoFocus}
            menuPortalTarget={document.body}
            onChange={onChange} />

  );
};

FieldSelect.defaultProps = {
  ariaLabel: undefined,
  autoFocus: undefined,
  className: undefined,
  clearable: false,
  qualifiedTypeCategory: undefined,
  excludedFields: [],
  onMenuClose: undefined,
  openMenuOnFocus: undefined,
  persistSelection: undefined,
  placeholder: undefined,
  properties: undefined,
  selectRef: undefined,
};

export default FieldSelect;
