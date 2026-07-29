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
import styled from 'styled-components';

import IconButton from 'components/common/IconButton';
import type { WidgetActionType, WidgetMenuActionComponentProps } from 'views/components/widgets/Types';

const NeutralLink = styled.a`
  display: inline-flex;
  align-items: center;
  color: inherit;
  text-decoration: none;

  &:hover {
    text-decoration: none;
  }

  &:visited {
    color: inherit;
  }
`;

const TITLE = 'Replay search';

const ReplayLinkWidgetActionComponent = ({ widget }: WidgetMenuActionComponentProps) => {
  const href = widget.context;

  if (!href) {
    return null;
  }

  return (
    <NeutralLink href={href} target="_blank" rel="noopener noreferrer" title={TITLE}>
      <IconButton name="play_arrow" focusable={false} title={TITLE} />
    </NeutralLink>
  );
};

const replayLinkWidgetAction: WidgetActionType = {
  type: 'welcome-replay-link',
  position: 'menu',
  showInNonInteractiveMode: true,
  component: ReplayLinkWidgetActionComponent,
};

export default replayLinkWidgetAction;
