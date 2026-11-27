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
import { useState, useMemo } from 'react';
import { useFloating } from '@floating-ui/react';

import { AdditionalContext } from 'views/logic/ActionContext';
import ValueActionsDropdown from 'views/components/visualizations/OnClickPopover/ValueActionsDropdown';
import OnClickPopoverWrapper from 'views/components/visualizations/OnClickPopover/OnClickPopoverWrapper';
import useQueryFieldTypes from 'views/hooks/useQueryFieldTypes';

type ClickedItem = {
  el: Element;
  field: string;
  value: string;
};

const useSwimlaneClickPopover = () => {
  const [clickedItem, setClickedItem] = useState<ClickedItem | null>(null);
  const types = useQueryFieldTypes();

  const { refs, floatingStyles } = useFloating({
    placement: 'top-start',
    elements: { reference: clickedItem?.el },
    transform: false,
  });

  const handleClick = (e: React.MouseEvent, field: string, value: string) => {
    const el = e.currentTarget as Element;
    setClickedItem((prev) => (prev?.el === el && prev?.field === field && prev?.value === value ? null : { el, field, value }));
  };

  const onPopoverChange = (isOpen: boolean) => {
    if (!isOpen) setClickedItem(null);
  };

  const additionalContextValue = useMemo(
    () => ({
      valuePath: [{ [clickedItem?.field ?? '']: clickedItem?.value ?? '' }],
      fieldTypes: types,
    }),
    [clickedItem, types],
  );

  const popover = (
    <OnClickPopoverWrapper
      isPopoverOpen={!!clickedItem}
      onPopoverChange={onPopoverChange}
      // eslint-disable-next-line react-hooks/refs
      ref={refs.setFloating}
      style={floatingStyles}>
      {clickedItem && (
        <AdditionalContext.Provider value={additionalContextValue}>
          <ValueActionsDropdown
            field={clickedItem.field}
            value={clickedItem.value}
            setStep={() => setClickedItem(null)}
            onActionRun={() => setClickedItem(null)}
          />
        </AdditionalContext.Provider>
      )}
    </OnClickPopoverWrapper>
  );

  return { handleClick, popover };
};

export default useSwimlaneClickPopover;
