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
import { useCallback, useState } from 'react';
import styled from 'styled-components';

import { Button } from 'components/bootstrap';
import { Icon } from 'components/common';
import SelectedFieldsList from 'views/components/widgets/SelectedFieldsList';
import FieldSelect from 'views/components/aggregationwizard/FieldSelect';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

const FIELD_LIST_LIMIT = 1;

const ToggleButton = styled(Button)`
  padding: 0;
  margin-bottom: 10px;
  display: block;
`;

const ToggleIcon = styled(Icon)`
  margin-left: 5px;
`;

type Props = {
  createSelectPlaceholder?: string
  displaySortableListOverlayInPortal?: boolean,
  menuPortalTarget?: HTMLElement,
  onChange: (newFields: Array<string>) => void,
  isFieldQualified?: (field: FieldTypeMapping) => boolean,
  selectSize?: 'normal' | 'small',
  selectedFields: Array<string>,
  testPrefix?: string,
  showSelectAllRest?: boolean,
  showDeSelectAll?: boolean,
  showListCollapseButton?: boolean
  showUnit?: boolean,
  fieldSelect?: React.ComponentType<React.ComponentProps<typeof FieldSelect>>
}

const FieldsConfiguration = ({
  createSelectPlaceholder = 'Add a field',
  displaySortableListOverlayInPortal = false,
  menuPortalTarget,
  onChange,
  isFieldQualified,
  selectSize,
  selectedFields,
  testPrefix = '',
  showSelectAllRest = false,
  showDeSelectAll = false,
  showListCollapseButton = false,
  showUnit = false,
  fieldSelect: FieldSelectComponent = FieldSelect,
}: Props) => {
  const [showSelectedList, setShowSelectedList] = useState(true);
  const onAddField = useCallback((newField: string) => (
    onChange([...selectedFields, newField])
  ), [onChange, selectedFields]);

  const _showListCollapseButton = showListCollapseButton && selectedFields.length > FIELD_LIST_LIMIT;

  const onSelectAllRest = (newFields: Array<string>) => {
    const _selectedFields = [...selectedFields, ...newFields];
    onChange(_selectedFields);

    if (showListCollapseButton && _selectedFields.length > FIELD_LIST_LIMIT) {
      setShowSelectedList(false);
    }
  };

  const onDeselectAll = () => {
    onChange([]);
    setShowSelectedList(true);
  };

  return (
    <>
      {_showListCollapseButton && (
      <ToggleButton bsStyle="link"
                    bsSize="xs"
                    onClick={() => {
                      setShowSelectedList((cur) => !cur);
                    }}>
        {showSelectedList ? `Hide ${selectedFields.length} selected fields` : `Show ${selectedFields.length} selected fields`}<ToggleIcon name={showSelectedList ? 'keyboard_arrow_up' : 'keyboard_arrow_down'} />
      </ToggleButton>
      )}
      {showSelectedList && (
      <SelectedFieldsList testPrefix={testPrefix}
                          selectedFields={selectedFields}
                          selectSize={selectSize}
                          displayOverlayInPortal={displaySortableListOverlayInPortal}
                          onChange={onChange}
                          showUnit={showUnit} />
      )}
      <FieldSelectComponent id="field-create-select"
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
                            ariaLabel={createSelectPlaceholder}
                            onSelectAllRest={showSelectAllRest && onSelectAllRest}
                            showSelectAllRest={showSelectAllRest}
                            onDeSelectAll={onDeselectAll}
                            showDeSelectAll={showDeSelectAll && !!selectedFields.length} />
    </>
  );
};

export default FieldsConfiguration;
