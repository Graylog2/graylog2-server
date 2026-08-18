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
import { useMemo, useState } from 'react';
import { useFloating } from '@floating-ui/react';

import OnClickPopoverWrapper from 'views/components/visualizations/OnClickPopover/OnClickPopoverWrapper';
import ValueActionsDropdown from 'views/components/visualizations/OnClickPopover/ValueActionsDropdown';
import type { Step } from 'views/components/visualizations/OnClickPopover/Types';
import { AdditionalContext } from 'views/logic/ActionContext';
import useQueryFieldTypes from 'views/hooks/useQueryFieldTypes';

import type { ClickedItem } from './types';

type Result = {
  handleRectClick: (e: React.MouseEvent<SVGElement>, field: string, value: string) => void;
  popover: React.ReactNode;
};

/**
 * Manages the field/value action popover triggered by clicking a funnel rect.
 *
 * Uses floating-ui (matching usePlotOnClickPopover) so the popover anchors to
 * the clicked SVG element and renders outside the Container's overflow:hidden.
 */
const useFunnelOnClickPopover = (): Result => {
  const types = useQueryFieldTypes();
  const [clickedItem, setClickedItem] = useState<ClickedItem | null>(null);
  // Real useState so setDummyStep satisfies ValueActionsDropdown's prop type.
  const [, setDummyStep] = useState<Step>('values');

  const { refs, floatingStyles } = useFloating({
    placement: 'top-start',
    elements: { reference: clickedItem?.el },
    transform: false,
  });

  const additionalContextValue = useMemo(
    () => ({
      valuePath: clickedItem ? [{ [clickedItem.field]: clickedItem.value }] : [],
      fieldTypes: types,
    }),
    [clickedItem, types],
  );

  const handleRectClick = (e: React.MouseEvent<SVGElement>, field: string, value: string) => {
    const el = e.currentTarget;
    // Preserve the same reference when clicking the same element — a new object
    // would cause downstream hooks to reset their internal state.
    setClickedItem((prev) => (prev?.el === el ? prev : { el, field, value }));
  };

  const popover = (
    <OnClickPopoverWrapper
      // eslint-disable-next-line react-hooks/refs
      ref={refs.setFloating}
      isPopoverOpen={!!clickedItem}
      onPopoverChange={(open) => { if (!open) setClickedItem(null); }}
      style={floatingStyles}>
      {clickedItem && (
        <AdditionalContext.Provider value={additionalContextValue}>
          <ValueActionsDropdown
            field={clickedItem.field}
            value={clickedItem.value}
            onActionRun={() => setClickedItem(null)}
            setStep={setDummyStep}
          />
        </AdditionalContext.Provider>
      )}
    </OnClickPopoverWrapper>
  );

  return { handleRectClick, popover };
};

export default useFunnelOnClickPopover;
