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
import { Avatar } from '@mantine/core';
import styled, { css } from 'styled-components';

import Tooltip from 'components/common/Tooltip';
import useCurrentUser from 'hooks/useCurrentUser';

import type { Viewer } from './types';
import { currentUserFirst, initials, viewerLabel } from './viewers';

const COLORS = ['primary', 'info', 'success', 'warning', 'danger'];

const StyledAvatar = styled(Avatar)(
  ({ theme }) => css`
    cursor: default;

    && {
      border-color: ${theme.colors.global.contentBackground};
    }
  `,
);

const CurrentUserAvatar = styled(StyledAvatar)(
  ({ theme }) => css`
    && {
      border-color: ${theme.colors.variant.primary};
    }
  `,
);

const HiddenViewers = styled.ul`
  margin: 0;
  padding: 0;
  list-style: none;
`;

type Props = {
  viewers: Array<Viewer>;
  max?: number;
};

const ViewerBadges = ({ viewers, max = 5 }: Props) => {
  const currentUser = useCurrentUser(false);

  if (viewers.length === 0) {
    return null;
  }

  const ordered = currentUserFirst(viewers, currentUser?.id);
  const visible = ordered.length > max ? ordered.slice(0, Math.max(max - 1, 1)) : ordered;
  const hidden = ordered.slice(visible.length);
  const label = (viewer: Viewer) => viewerLabel(viewer, viewer.id === currentUser?.id);

  return (
    <Avatar.Group spacing="xs" role="group" aria-label="Current viewers">
      {visible.map((viewer) => {
        const ViewerAvatar = viewer.id === currentUser?.id ? CurrentUserAvatar : StyledAvatar;

        return (
          <Tooltip key={viewer.id} label={label(viewer)} withArrow>
            <ViewerAvatar
              size="sm"
              radius="xl"
              variant="light"
              color="initials"
              name={viewer.username}
              allowedInitialsColors={COLORS}
              role="img"
              aria-label={label(viewer)}>
              {initials(viewer)}
            </ViewerAvatar>
          </Tooltip>
        );
      })}
      {hidden.length > 0 && (
        <Tooltip
          label={
            <HiddenViewers>
              {hidden.map((viewer) => (
                <li key={viewer.id}>{label(viewer)}</li>
              ))}
            </HiddenViewers>
          }
          withArrow>
          <StyledAvatar
            size="sm"
            radius="xl"
            variant="light"
            color="gray"
            role="img"
            aria-label={`${hidden.length} more viewers`}>
            +{hidden.length}
          </StyledAvatar>
        </Tooltip>
      )}
    </Avatar.Group>
  );
};

export default ViewerBadges;
