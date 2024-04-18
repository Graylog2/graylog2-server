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
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldTypeIcon from 'views/components/sidebar/fields/FieldTypeIcon';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import type { SelectRef } from 'components/common/Select/Select';

const FieldName = styled.span`
  display: inline-flex;
  gap: 2px;
  align-items: center;
`;

type Props = {
  ariaLabel?: string,
  autoFocus?: boolean,
  allowCreate?: boolean,
  className?: string,
  clearable?: boolean,
  excludedFields?: Array<string>,
  id: string,
  isFieldQualified?: (field: FieldTypeMapping) => boolean,
  menuPortalTarget?: HTMLElement,
  name: string,
  onChange: (fieldName: string) => void,
  onMenuClose?: () => void,
  openMenuOnFocus?: boolean,
  persistSelection?: boolean,
  placeholder?: string,
  selectRef?: SelectRef,
  size?: 'normal' | 'small',
  value: string | undefined,
}

const sortByLabel = ({ label: label1 }: { label: string }, { label: label2 }: { label: string }) => defaultCompare(label1, label2);

const UnqualifiedOption = styled.span(({ theme }) => css`
  color: ${theme.colors.gray[70]};
`);

type OptionRendererProps = {
  label: string,
  qualified: boolean,
  type?: FieldType,
};

const OptionRenderer = ({ label, qualified, type }: OptionRendererProps) => {
  const children = (
    <FieldName>
      {type && <><FieldTypeIcon type={type} /> </>}{label}
    </FieldName>
  );

  return qualified ? <span>{children}</span> : <UnqualifiedOption>{children}</UnqualifiedOption>;
};

OptionRenderer.defaultProps = {
  type: undefined,
};

const FieldSelect = ({
  ariaLabel,
  autoFocus,
  allowCreate,
  className,
  clearable,
  excludedFields,
  id,
  isFieldQualified,
  menuPortalTarget,
  name,
  onChange,
  onMenuClose,
  openMenuOnFocus,
  persistSelection,
  placeholder,
  selectRef,
  size,
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
      qualified: isFieldQualified(field),
    }))
    .toArray()
    .sort(sortByLabel), [activeQuery, excludedFields, fieldTypes.queryFields, isFieldQualified]);

  return (
    <Select options={fieldOptions}
            inputId={`select-${id}`}
            forwardedRef={selectRef}
            allowCreate={allowCreate}
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
            size={size}
            autoFocus={autoFocus}
            menuPortalTarget={menuPortalTarget}
            onChange={onChange} />

  );
};

FieldSelect.defaultProps = {
  allowCreate: false,
  ariaLabel: undefined,
  autoFocus: undefined,
  className: undefined,
  clearable: false,
  isFieldQualified: () => true,
  excludedFields: [],
  onMenuClose: undefined,
  openMenuOnFocus: undefined,
  persistSelection: undefined,
  placeholder: undefined,
  selectRef: undefined,
  size: 'small',
  menuPortalTarget: undefined,
};

export default FieldSelect;
