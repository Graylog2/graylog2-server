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
import { useCallback } from 'react';

import SelectedFieldsList from 'views/components/widgets/SelectedFieldsList';
import FieldSelect from 'views/components/aggregationwizard/FieldSelect';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

type Props = {
  createSelectPlaceholder?: string
  displaySortableListOverlayInPortal?: boolean,
  menuPortalTarget?: HTMLElement,
  onChange: (newFields: Array<string>) => void,
  isFieldQualified?: (field: FieldTypeMapping) => boolean,
  selectSize?: 'normal' | 'small',
  selectedFields: Array<string>,
  testPrefix?: string,
}

const FieldsConfiguration = ({
  createSelectPlaceholder,
  displaySortableListOverlayInPortal,
  menuPortalTarget,
  onChange,
  isFieldQualified,
  selectSize,
  selectedFields,
  testPrefix,
}: Props) => {
  const onAddField = useCallback((newField: string) => (
    onChange([...selectedFields, newField])
  ), [onChange, selectedFields]);

  return (
    <>
      <SelectedFieldsList testPrefix={testPrefix}
                          selectedFields={selectedFields}
                          selectSize={selectSize}
                          displayOverlayInPortal={displaySortableListOverlayInPortal}
                          onChange={onChange} />
      <FieldSelect id="field-create-select"
                   onChange={onAddField}
                   clearable={false}
                   isFieldQualified={isFieldQualified}
                   persistSelection={false}
                   name="field-create-select"
                   value={undefined}
                   size={selectSize}
                   menuPortalTarget={menuPortalTarget}
                   excludedFields={selectedFields ?? []}
                   placeholder={createSelectPlaceholder}
                   ariaLabel={createSelectPlaceholder} />
    </>
  );
};

FieldsConfiguration.defaultProps = {
  createSelectPlaceholder: 'Add a field',
  displaySortableListOverlayInPortal: false,
  isFieldQualified: undefined,
  selectSize: undefined,
  menuPortalTarget: undefined,
  testPrefix: '',
};

export default FieldsConfiguration;
