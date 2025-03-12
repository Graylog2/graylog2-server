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
import styled, { css } from 'styled-components';

import IconButton from 'components/common/IconButton';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import useTableFetchContext from 'components/common/PaginatedEntityTable/useTableFetchContext';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import Spinner from 'components/common/Spinner';

const Container = styled.div(
  ({ theme }) => css`
    margin: 5px 0;
    padding: 5px;
    background-color: ${theme.colors.background.secondaryNav};
  `,
);
const HeadlineContainer = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
`;
type EventsMetricsProps = React.PropsWithChildren<{
  expanded: boolean;
  onExpandToggle: (expanded: boolean) => void;
}>;
const EventsMetrics = ({ children = undefined, expanded: initialExpanded, onExpandToggle }: EventsMetricsProps) => {
  const [expanded, setExpanded] = useState<boolean>(initialExpanded);
  const expandTitle = `${expanded ? 'Collapse' : 'Expand'} Metrics`;
  const expandIcon = expanded ? 'unfold_less' : 'unfold_more';
  const onClick = useCallback(
    () =>
      setExpanded((_expanded) => {
        const newValue = !_expanded;
        onExpandToggle(newValue);

        return newValue;
      }),
    [onExpandToggle],
  );

  return (
    <Container>
      <HeadlineContainer>
        <h2>Metrics</h2>
        <IconButton title={expandTitle} name={expandIcon} onClick={onClick} />
      </HeadlineContainer>
      {expanded ? children : null}
    </Container>
  );
};

type Props = React.PropsWithChildren<{}>;
const EventsMetricsBarrier = ({ children = undefined }: Props) => {
  const { entityTableId } = useTableFetchContext();
  const { mutate: updatePreferences } = useUpdateUserLayoutPreferences(entityTableId);
  const { data, isInitialLoading: isLoadingPreferences } = useUserLayoutPreferences<{ showMetrics: boolean }>(
    entityTableId,
  );
  const onExpandToggle = useCallback(
    (expanded: boolean) => updatePreferences({ customPreferences: { showMetrics: expanded } }),
    [updatePreferences],
  );

  return isLoadingPreferences ? (
    <Spinner />
  ) : (
    <EventsMetrics expanded={data.customPreferences?.showMetrics ?? true} onExpandToggle={onExpandToggle}>
      {children}
    </EventsMetrics>
  );
};

export default EventsMetricsBarrier;
