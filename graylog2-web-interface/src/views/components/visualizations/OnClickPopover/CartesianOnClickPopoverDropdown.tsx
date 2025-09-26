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
import React, { useMemo, useCallback } from 'react';
import styled, { css } from 'styled-components';

import Popover from 'components/common/Popover';
import { keySeparator } from 'views/Constants';
import OnClickPopoverValueGroups from 'views/components/visualizations/OnClickPopover/OnClickPopoverValueGroups';
import type { ValueGroups, OnClickPopoverDropdownProps } from 'views/components/visualizations/OnClickPopover/Types';
import getHoverSwatchColor from 'views/components/visualizations/utils/getHoverSwatchColor';
import useUserDateTime from 'hooks/useUserDateTime';
import { adjustFormat, toUTCFromTz, isValidDate } from 'util/DateTime';

const DivContainer = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xxs};
  `,
);

const CartesianOnClickPopoverDropdown = ({ clickPoint, config, setFieldData }: OnClickPopoverDropdownProps) => {
  const traceColor = getHoverSwatchColor(clickPoint);
  const { userTimezone } = useUserDateTime();
  const convertToOriginalValue = useCallback(
    (value: number | string) => {
      if (config.isTimeline && typeof value === 'string' && isValidDate(value))
        return adjustFormat(toUTCFromTz(value, userTimezone), 'internal');

      return value;
    },
    [config.isTimeline, userTimezone],
  );

  const { rowPivotValues, columnPivotValues, metricValue } = useMemo<ValueGroups>(() => {
    if (!clickPoint || !config) return {};
    const splitNames: Array<string | number> = (clickPoint.data.originalName ?? clickPoint.data.name).split(
      keySeparator,
    );
    const metric: string = config.series.length === 1 ? config.series[0].function : (splitNames.pop() as string);
    const columnValues = splitNames.filter((value) => value !== metric);

    const columnPivotsToFields = config?.columnPivots?.flatMap((pivot) => pivot.fields) ?? [];

    const rowPivotsToFields = config?.rowPivots?.flatMap((pivot) => pivot.fields) ?? [];
    const splitXValues: Array<string | number> = `${String(clickPoint.x)}`.split(keySeparator);

    return {
      rowPivotValues: splitXValues.map((labelValue, i) => {
        const value = convertToOriginalValue(labelValue);

        return {
          value,
          field: rowPivotsToFields[i],
          text: String(labelValue),
          traceColor,
        };
      }),
      columnPivotValues: columnValues.map((value, i) => ({
        value,
        field: columnPivotsToFields[i],
        text: String(value),
        traceColor,
      })),
      metricValue: {
        value: clickPoint.y,
        field: metric,
        text: `${String(clickPoint.text ?? clickPoint.y)}`,
        traceColor,
      },
    };
  }, [clickPoint, config, convertToOriginalValue, traceColor]);

  return (
    <Popover.Dropdown>
      <DivContainer>
        <OnClickPopoverValueGroups
          columnPivotValues={columnPivotValues}
          metricValue={metricValue}
          rowPivotValues={rowPivotValues}
          setFieldData={setFieldData}
        />
      </DivContainer>
    </Popover.Dropdown>
  );
};

export default CartesianOnClickPopoverDropdown;
