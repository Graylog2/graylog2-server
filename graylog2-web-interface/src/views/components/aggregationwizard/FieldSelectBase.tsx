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
import type { SyntheticEvent } from 'react';
import { useCallback, useMemo } from 'react';
import styled, { css } from 'styled-components';

import { defaultCompare } from 'logic/DefaultCompare';
import Select from 'components/common/Select';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldTypeIcon from 'views/components/sidebar/fields/FieldTypeIcon';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import type { SelectRef } from 'components/common/Select/Select';
import { Button } from 'components/bootstrap';

const FieldName = styled.span`
  display: inline-flex;
  gap: 2px;
  align-items: center;
`;

const ButtonRow = styled.div`
  margin-top: 5px;
  display: inline-flex;
  gap: 5px;
  margin-bottom: 10px;  
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
  onSelectAllRest?: (fieldNames: Array<string>) => void,
  showSelectAllRest?: boolean,
  onDeSelectAll?: (e: SyntheticEvent) => void,
  options: Array<FieldTypeMapping>
  showDeSelectAll?: boolean,
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

const FieldSelect = ({
  ariaLabel,
  autoFocus,
  allowCreate = false,
  className,
  clearable = false,
  excludedFields = [],
  id,
  isFieldQualified = () => true,
  menuPortalTarget,
  name,
  onChange,
  onMenuClose,
  openMenuOnFocus,
  persistSelection,
  placeholder,
  selectRef,
  size = 'small',
  value,
  onSelectAllRest,
  showSelectAllRest = false,
  onDeSelectAll,
  showDeSelectAll = false,
  options,
}: Props) => {
  const fieldOptions = useMemo(() => options
    .filter((field) => !excludedFields.includes(field.name))
    .map((field) => ({
      label: field.name,
      value: field.name,
      type: field.type,
      qualified: isFieldQualified(field),
    }))
    .sort(sortByLabel), [excludedFields, isFieldQualified, options]);

  const _onSelectAllRest = useCallback(() => onSelectAllRest(fieldOptions.map(({ value: fieldValue }) => fieldValue)), [fieldOptions, onSelectAllRest]);

  const _showSelectAllRest = !!fieldOptions?.length && showSelectAllRest && typeof _onSelectAllRest === 'function';

  const _showDeSelectAll = showDeSelectAll && typeof onDeSelectAll === 'function';

  return (
    <>
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
      {(_showSelectAllRest || _showDeSelectAll) && (
        <ButtonRow>
          {_showSelectAllRest && <Button bsSize="xs" onClick={_onSelectAllRest}>Select all fields</Button>}
          {_showDeSelectAll && <Button bsSize="xs" onClick={onDeSelectAll}>Deselect all fields</Button>}
        </ButtonRow>
      )}
    </>

  );
};

export default FieldSelect;
