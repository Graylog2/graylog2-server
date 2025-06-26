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
import isEqual from 'lodash/isEqual';
import { useFormikContext } from 'formik';
import styled from 'styled-components';
import { useCallback } from 'react';

import type { TimeRange, AbsoluteTimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import useUserDateTime from 'hooks/useUserDateTime';
import { onInitializingTimerange } from 'views/components/TimerangeForForm';
import { normalizeFromSearchBarForBackend } from 'views/logic/queries/NormalizeTimeRange';
import type { IconName } from 'components/common/Icon';
import Icon from 'components/common/Icon';
import { Button } from 'components/bootstrap';
import { isNoTimeRangeOverride } from 'views/typeGuards/timeRange';
import { readableDifference } from 'util/DateTime';

const DIRECTIONS = {
  backward: 'backward',
  forward: 'forward',
};

const DIRECTION_ICONS: Record<Direction, IconName> = {
  [DIRECTIONS.backward]: 'keyboard_arrow_left',
  [DIRECTIONS.forward]: 'keyboard_arrow_right',
};

type Direction = (typeof DIRECTIONS)[keyof typeof DIRECTIONS];

const ArrowButton = styled(Button)`
  padding: 0;
  border: 0;
`;

const MoveRangeButton = ({
  direction,
  disabled,
  onMoveRange,
  title,
}: {
  direction: Direction;
  disabled: boolean;
  onMoveRange: (direction: Direction) => void;
  title: string;
}) => {
  const onClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    onMoveRange(direction);
  };

  return (
    <ArrowButton onClick={onClick} disabled={disabled} title={title}>
      <Icon name={DIRECTION_ICONS[direction]} />
    </ArrowButton>
  );
};

type Props = React.PropsWithChildren<{
  setCurrentTimeRange: (newRange: TimeRange) => void;
  effectiveTimerange: AbsoluteTimeRange | undefined;
  queryTimerange: TimeRange | NoTimeRangeOverride;
  searchBarTimerange: TimeRange | NoTimeRangeOverride;
}>;

const MoveRangeInner = ({
  setCurrentTimeRange,
  effectiveTimerange,
  queryTimerange,
  searchBarTimerange,
  children = undefined,
}: Props) => {
  const { formatTime, userTimezone } = useUserDateTime();
  const { submitForm, isValid } = useFormikContext<{ timerange: TimeRange }>();

  const readableDuration = effectiveTimerange
    ? readableDifference(effectiveTimerange.from, effectiveTimerange.to)
    : undefined;

  if (effectiveTimerange) {
    readableDifference(effectiveTimerange.from, effectiveTimerange.to);
  }

  const onMoveRange = useCallback(
    (direction: Direction) => {
      // Todo: Add telemetry event
      const currentFrom = new Date(effectiveTimerange.from);
      const currentTo = new Date(effectiveTimerange.to);

      const currentDurationMs = currentTo.getTime() - currentFrom.getTime();

      const isBackwardDirection = direction === DIRECTIONS.backward;

      const newFrom = (
        isBackwardDirection ? new Date(currentFrom.getTime() - currentDurationMs) : currentTo
      ).toISOString();
      const newTo = (
        isBackwardDirection ? currentFrom : new Date(currentTo.getTime() + currentDurationMs)
      ).toISOString();

      setCurrentTimeRange(onInitializingTimerange({ type: 'absolute', from: newFrom, to: newTo }, formatTime));
      submitForm();
    },
    [effectiveTimerange?.from, effectiveTimerange?.to, formatTime, setCurrentTimeRange, submitForm],
  );

  const disableButton =
    !effectiveTimerange ||
    !isValid ||
    isNoTimeRangeOverride(searchBarTimerange) ||
    !isEqual(queryTimerange, normalizeFromSearchBarForBackend(searchBarTimerange, userTimezone, 'internalIndexer'));

  return (
    <>
      <MoveRangeButton
        onMoveRange={onMoveRange}
        disabled={disableButton}
        direction={DIRECTIONS.backward}
        title={disableButton ? 'Show previous' : `Show previous ${readableDuration}`}
      />
      {children}
      <MoveRangeButton
        onMoveRange={onMoveRange}
        disabled={disableButton}
        direction={DIRECTIONS.forward}
        title={disableButton ? 'Show next' : `Show next ${readableDuration}`}
      />
    </>
  );
};

const MoveRange = ({
  displayMoveRangeButtons,
  setCurrentTimeRange,
  effectiveTimerange,
  queryTimerange,
  searchBarTimerange,
  children = undefined,
}: Props & { displayMoveRangeButtons: boolean }) => {
  if (!displayMoveRangeButtons) {
    return children;
  }

  return (
    <MoveRangeInner
      setCurrentTimeRange={setCurrentTimeRange}
      effectiveTimerange={effectiveTimerange}
      queryTimerange={queryTimerange}
      searchBarTimerange={searchBarTimerange}>
      {children}
    </MoveRangeInner>
  );
};

export default MoveRange;
