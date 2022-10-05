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

import { defaultCompare } from 'logic/DefaultCompare';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import { Input } from 'components/bootstrap';
import Select from 'components/common/Select';
import { useStore } from 'stores/connect';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';

type Props = {
  ariaLabel?: string,
  clearable?: boolean,
  error?: string,
  id: string,
  label: string,
  name: string,
  onChange: (changeEvent: { target: { name: string, value: string } }) => void,
  value: string | undefined,
  selectRef: React.Ref<React.ComponentType>
}

const sortByLabel = ({ label: label1 }: { label: string }, { label: label2 }: { label: string }) => defaultCompare(label1, label2);

const FieldSelect = ({ name, id, error, clearable, value, onChange, label, ariaLabel, selectRef }: Props) => {
  const { activeQuery } = useStore(ViewMetadataStore);
  const fieldTypes = useContext(FieldTypesContext);
  const fieldTypeOptions = useMemo(() => fieldTypes.queryFields
    .get(activeQuery, Immutable.List())
    .map((fieldType) => ({ label: fieldType.name, value: fieldType.name }))
    .toArray()
    .sort(sortByLabel),
  [activeQuery, fieldTypes.queryFields]);

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
              name={name}
              value={value}
              aria-label={ariaLabel}
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
};

export default FieldSelect;
