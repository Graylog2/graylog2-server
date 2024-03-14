import * as React from 'react';
import { useState } from 'react';

import { Menu } from 'components/bootstrap';
import { IconButton } from 'components/common';

import type { FilterComponent } from './types';

type Props = {
  column: string,
  columnTitle: (column: string) => string,
  containerWidth: number
  filterComponent: FilterComponent
  onDelete: () => void,
  onEdit: (value: string) => void,
  selectedValues: Array<string>,
  value: string
}

const FilterEditButton = ({ filterComponent, column, value, columnTitle, onEdit, selectedValues, containerWidth, onDelete }: Props) => {
  const [open, setOpen] = useState(false);
  const [editValue, setEditValue] = React.useState<string>(value);

  const submitChanges = (newValue: string) => {
    if (newValue) {
      onEdit(newValue);
    } else {
      onDelete();
    }

    setEditValue(null);
    setOpen(false);
  };

  const onChange = (newValue: unknown, shouldSubmit = true) => {
    const normalizedValue = filterComponent?.valueForConfig?.(newValue) ?? newValue as string;

    setEditValue(normalizedValue);

    if (shouldSubmit) {
      submitChanges(normalizedValue);
    }
  };

  const onClose = () => {
    setOpen(false);

    if (filterComponent.submitChangesOnClose) {
      submitChanges(editValue);
    }
  };

  if (!filterComponent) {
    return null;
  }

  return (
    <Menu position="bottom-end"
          withinPortal
          opened={open}
          width={containerWidth}
          offset={{ alignmentAxis: -25 }}
          onClose={onClose}>
      <Menu.Target>
        <IconButton name="edit"
                    title={`Edit ${columnTitle(column)} filter`}
                    onClick={() => setOpen(true)} />
      </Menu.Target>
      <Menu.Dropdown>
        {filterComponent.configuration(selectedValues, filterComponent.valueFromConfig?.(editValue) ?? editValue, onChange)}
      </Menu.Dropdown>
    </Menu>
  );
};

export default FilterEditButton;
