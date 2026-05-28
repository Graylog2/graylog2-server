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
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { PlotMouseEvent } from 'plotly.js';

import OverflowingComponentsContextProvider from 'views/components/contexts/OverflowingComponentsContextProvider';
import Popover from 'components/common/Popover';
import { AdditionalContext } from 'views/logic/ActionContext';
import useQueryFieldTypes from 'views/hooks/useQueryFieldTypes';
import type { OnClickMarkerEvent } from 'views/components/visualizations/GenericPlot';

import SankeyNodeActionsDropdown from './SankeyNodeActionsDropdown';

type NodeCustomData = { field: string; value: unknown };

type Selected = NodeCustomData & { x: number; y: number };

type SankeyPoint = {
  index?: number;
  customdata?: NodeCustomData;
  source?: unknown;
  target?: unknown;
};

const targetStyle = (selected: Selected | null): React.CSSProperties => ({
  position: 'fixed',
  left: selected?.x ?? 0,
  top: selected?.y ?? 0,
  width: 0,
  height: 0,
  pointerEvents: 'none',
});

const useSankeyOnClickPopover = () => {
  const [selected, setSelected] = useState<Selected | null>(null);
  const types = useQueryFieldTypes();
  const lastMouseRef = useRef<{ x: number; y: number }>({ x: 0, y: 0 });

  useEffect(() => {
    const handler = (ev: MouseEvent) => {
      lastMouseRef.current = { x: ev.clientX, y: ev.clientY };
    };

    document.addEventListener('mousedown', handler, true);

    return () => document.removeEventListener('mousedown', handler, true);
  }, []);

  const onChartClick = useCallback((_: OnClickMarkerEvent, e: PlotMouseEvent) => {
    const point = e.points?.[0] as unknown as SankeyPoint | undefined;

    if (!point) return;
    if (point.source !== undefined || point.target !== undefined) return;
    if (!point.customdata || typeof point.customdata !== 'object') return;
    if (typeof (point.customdata as NodeCustomData).field !== 'string') return;

    const { x, y } = lastMouseRef.current;

    setSelected({ field: point.customdata.field, value: point.customdata.value, x, y });
  }, []);

  const closePopover = useCallback(() => setSelected(null), []);

  const onPopoverChange = useCallback(
    (isOpen: boolean) => {
      if (!isOpen) closePopover();
    },
    [closePopover],
  );

  const additionalContextValue = useMemo(() => ({ fieldTypes: types }), [types]);

  const popover = (
    <OverflowingComponentsContextProvider>
      <Popover opened={!!selected} onChange={onPopoverChange} withArrow position="bottom" offset={4}>
        <Popover.Target>
          <div style={targetStyle(selected)} />
        </Popover.Target>
        {selected && (
          <AdditionalContext.Provider value={additionalContextValue}>
            <SankeyNodeActionsDropdown field={selected.field} value={selected.value} onActionRun={closePopover} />
          </AdditionalContext.Provider>
        )}
      </Popover>
    </OverflowingComponentsContextProvider>
  );

  return { onChartClick, popover };
};

export default useSankeyOnClickPopover;
