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
import React, { useCallback, useContext, useMemo, useState } from 'react';
import styled, { css } from 'styled-components';
import type { Datum } from 'plotly.js';
import uniq from 'lodash/uniq';

import Popover from 'components/common/Popover';
import { Menu } from 'components/bootstrap';
import OnClickPopoverValueGroups from 'views/components/visualizations/OnClickPopover/OnClickPopoverValueGroups';
import PopoverTitle from 'views/components/visualizations/OnClickPopover/PopoverTitle';
import type { ClickPoint, FieldData, ValueGroups } from 'views/components/visualizations/OnClickPopover/Types';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import { ActionContext, AdditionalContext } from 'views/logic/ActionContext';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';
import ActionDropdown from 'views/components/actions/ActionDropdown';
import TypeSpecificValue from 'views/components/TypeSpecificValue';
import useFieldActions from 'views/components/actions/useFieldActions';
import useQueryFieldTypes from 'views/hooks/useQueryFieldTypes';
import useOverflowingComponents from 'views/hooks/useOverflowingComponents';
import hasMultipleValueForActions from 'views/components/visualizations/utils/hasMultipleValueForActions';
import { humanSeparator } from 'views/Constants';

type NodeCustomData = { field: string; value: unknown };

type LinkEndpoint = {
  customdata?: NodeCustomData;
  label?: string;
};

type EdgeCustomData = {
  source?: LinkEndpoint;
  target?: LinkEndpoint;
  value?: number;
};

type SankeyClickPoint = {
  customdata?: NodeCustomData | EdgeCustomData;
  index?: number;
  label?: string;
  value?: number;
  source?: LinkEndpoint;
  target?: LinkEndpoint;
};

const DivContainer = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xxs};
  `,
);

// Sankey natively emits link clicks with `source`/`target` at the top level of the click point.
// Other viz (e.g. network graph rendered as a scatter trace) instead encode link metadata into
// the trace's `customdata` so we treat that shape as a link too.
const linkContext = (pt: SankeyClickPoint): { source: LinkEndpoint; target: LinkEndpoint; value?: number } | null => {
  if (pt.source && pt.target && typeof pt.source === 'object' && typeof pt.target === 'object') {
    return { source: pt.source, target: pt.target, value: pt.value };
  }

  const cd = pt.customdata as EdgeCustomData | undefined;

  if (cd?.source && cd?.target && typeof cd.source === 'object' && typeof cd.target === 'object') {
    return { source: cd.source, target: cd.target, value: cd.value };
  }

  return null;
};

const nodeContext = (pt: SankeyClickPoint): NodeCustomData | null => {
  const cd = pt.customdata as NodeCustomData | undefined;

  return cd && typeof cd.field === 'string' ? cd : null;
};

type Props = {
  clickPoint: ClickPoint;
  config: AggregationWidgetConfig;
  onPopoverClose: () => void;
};

const SankeyActions = ({
  selected,
  onBack = undefined,
  onActionRun,
}: {
  selected: FieldData;
  onBack?: () => void;
  onActionRun: () => void;
}) => {
  const actionContext = useContext(ActionContext);
  const { additionalHandlerArgs } = useFieldActions();
  const { overflowingComponents, setOverflowingComponents } = useOverflowingComponents();
  const type = fieldTypeFor(selected.field, actionContext.fieldTypes);
  const handlerArgs = {
    field: selected.field,
    type,
    value: selected.value,
    contexts: actionContext,
    ...additionalHandlerArgs,
  };

  // An OR value path targets a single value across several groupings, so show that value once. The
  // combined (AND) value path of an edge instead lists each grouping value. Otherwise it's a single
  // field/value.
  const isOrCombination = actionContext.valuePathOperator === 'OR';
  const showCombinedHeader = hasMultipleValueForActions(actionContext) && !isOrCombination;

  let headerContent: React.ReactNode;

  if (showCombinedHeader) {
    headerContent = actionContext.valuePath.map((entry, index) => {
      const [entryField, entryValue] = Object.entries(entry)[0];

      return (
        <React.Fragment key={`${entryField}-${String(entryValue)}`}>
          {index > 0 && humanSeparator}
          <TypeSpecificValue
            field={entryField}
            value={entryValue}
            type={fieldTypeFor(entryField, actionContext.fieldTypes)}
            truncate
          />
        </React.Fragment>
      );
    });
  } else if (isOrCombination) {
    headerContent = <TypeSpecificValue field={selected.field} value={selected.value} type={type} truncate />;
  } else {
    headerContent = (
      <>
        {selected.field} = <TypeSpecificValue field={selected.field} value={selected.value} type={type} truncate />
      </>
    );
  }

  return (
    <>
      <PopoverTitle onBackClick={onBack ?? false}>{headerContent}</PopoverTitle>
      <Menu opened>
        <ActionDropdown
          handlerArgs={handlerArgs}
          type="value"
          onMenuToggle={onActionRun}
          overflowingComponents={overflowingComponents}
          setOverflowingComponents={setOverflowingComponents}
        />
      </Menu>
    </>
  );
};

const collectGroupItems = (valueGroups: ValueGroups) => [
  ...(valueGroups.rowPivotValues ?? []),
  ...(valueGroups.columnPivotValues ?? []),
  ...(valueGroups.metricValue ? [valueGroups.metricValue] : []),
];

const SankeyOnClickPopover = ({ clickPoint, config, onPopoverClose }: Props) => {
  const pt = clickPoint as unknown as SankeyClickPoint | undefined;

  const valueGroups = useMemo<ValueGroups & { title: string; nodeSelection?: FieldData | null }>(() => {
    if (!pt) return { title: '' };

    const link = linkContext(pt);

    if (link) {
      const srcLabel = link.source.label ?? '';
      const tgtLabel = link.target.label ?? '';
      const linkTitle = srcLabel && tgtLabel ? `${srcLabel} → ${tgtLabel}` : 'Connection';
      const srcCustom = link.source.customdata;
      const tgtCustom = link.target.customdata;
      const linkValue = link.value;
      const metric = config?.series?.[0];

      return {
        title: linkTitle,
        rowPivotValues: srcCustom
          ? [
              {
                field: srcCustom.field,
                value: srcCustom.value as Datum,
                text: srcLabel || String(srcCustom.value),
                traceColor: null,
              },
            ]
          : [],
        columnPivotValues: tgtCustom
          ? [
              {
                field: tgtCustom.field,
                value: tgtCustom.value as Datum,
                text: tgtLabel || String(tgtCustom.value),
                traceColor: null,
              },
            ]
          : [],
        metricValue:
          metric && linkValue !== undefined
            ? { field: metric.effectiveName, value: linkValue, text: String(linkValue), traceColor: null }
            : undefined,
      };
    }

    const node = nodeContext(pt);

    if (!node) return { title: pt.label ?? '' };

    // For the network graph a node value can occur in any of the configured groupings, so clicking
    // it should query that value across all of them, OR'd together (e.g. `source:V OR target:V`).
    const groupingFields = uniq(
      [...(config?.rowPivots ?? []), ...(config?.columnPivots ?? [])].flatMap((pivot) => pivot.fields),
    );
    const queryAcrossGroupings = config?.visualization === 'network' && groupingFields.length > 1;

    return {
      title: pt.label ?? String(node.value),
      rowPivotValues: [
        {
          field: node.field,
          value: node.value as Datum,
          text: pt.label ?? String(node.value),
          traceColor: null,
        },
      ],
      columnPivotValues: [],
      metricValue: undefined,
      nodeSelection: queryAcrossGroupings
        ? {
            field: node.field,
            value: node.value as Datum,
            contexts: {
              valuePath: groupingFields.map((groupingField) => ({ [groupingField]: node.value })),
              valuePathOperator: 'OR',
            },
          }
        : null,
    };
  }, [pt, config]);

  const items = useMemo(() => collectGroupItems(valueGroups), [valueGroups]);
  const skipValueSelection = items.length === 1;

  // Component is remounted (via `key` on the caller) whenever the chart click changes,
  // so internal state always starts fresh. When there's only one selectable item, jump
  // straight to the action menu instead of asking the user to pick from a single option.
  const [selected, setSelected] = useState<FieldData | null>(() => {
    if (!skipValueSelection) return null;

    return valueGroups.nodeSelection ?? { field: items[0].field, value: items[0].value, contexts: null };
  });

  const types = useQueryFieldTypes();
  const additionalContextValue = useMemo(
    () => ({
      valuePath: selected?.contexts?.valuePath,
      valuePathOperator: selected?.contexts?.valuePathOperator,
      fieldTypes: types,
    }),
    [selected?.contexts?.valuePath, selected?.contexts?.valuePathOperator, types],
  );

  const onActionRun = useCallback(() => {
    onPopoverClose();
    setSelected(null);
  }, [onPopoverClose]);

  const onBack = useCallback(() => setSelected(null), []);

  if (!pt) return null;

  return (
    <Popover.Dropdown title={selected ? null : valueGroups.title}>
      <DivContainer>
        {selected ? (
          <AdditionalContext.Provider value={additionalContextValue}>
            <SankeyActions
              selected={selected}
              onBack={skipValueSelection ? undefined : onBack}
              onActionRun={onActionRun}
            />
          </AdditionalContext.Provider>
        ) : (
          <OnClickPopoverValueGroups
            columnPivotValues={valueGroups.columnPivotValues}
            metricValue={valueGroups.metricValue}
            rowPivotValues={valueGroups.rowPivotValues}
            setFieldData={setSelected}
            config={config}
          />
        )}
      </DivContainer>
    </Popover.Dropdown>
  );
};

export default SankeyOnClickPopover;
