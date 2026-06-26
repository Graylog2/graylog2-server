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
import styled, { css } from 'styled-components';

import { Icon } from 'components/common';

const Wrapper = styled.span`
  white-space: nowrap;
`;

const StatusLabel = styled.span(
  ({ theme }) => css`
    margin-left: ${theme.spacings.xxs};
  `,
);

const SyncPendingIcon = styled(Icon)(
  ({ theme }) => css`
    color: ${theme.colors.variant.warning};
  `,
);

const InSyncIcon = styled(Icon)(
  ({ theme }) => css`
    color: ${theme.colors.text.primary};
  `,
);

type Props = {
  pending: boolean;
  withLabel?: boolean;
};

/**
 * The collector's sync state — whether it still has queued changes to apply. Shared between the
 * instances table column (icon-only, hover text) and the instance detail drawer (icon + label).
 */
const SyncStateIndicator = ({ pending, withLabel = false }: Props) =>
  pending ? (
    <Wrapper title="Sync pending — changes will apply at the collector's next check-in">
      <SyncPendingIcon name="update" />
      {withLabel && <StatusLabel>Sync pending</StatusLabel>}
    </Wrapper>
  ) : (
    <Wrapper title="In sync — all changes applied">
      <InSyncIcon name="check_circle" />
      {withLabel && <StatusLabel>In sync</StatusLabel>}
    </Wrapper>
  );

export default SyncStateIndicator;
