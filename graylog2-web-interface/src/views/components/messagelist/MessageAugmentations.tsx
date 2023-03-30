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
import Sticky from 'react-sticky-el';
import type { DefaultTheme } from 'styled-components';
import styled, { withTheme } from 'styled-components';
import { useContext } from 'react';

import usePluginEntities from 'hooks/usePluginEntities';
import type { Message } from 'views/components/messagelist/Types';
import WindowDimensionsContext from 'contexts/WindowDimensionsContext';

const StyledSticky = styled(Sticky)`
  position: static !important;
`;

type Props = {
  message: Message,
  theme: DefaultTheme
}

const MessageAugmentations = ({ message, theme }: Props) => {
  const augmentations = usePluginEntities('messageAugmentations');
  const windowDimensions = useContext(WindowDimensionsContext);
  const isSticky = windowDimensions.width >= theme.breakpoints.px.max.md;

  if (!augmentations || augmentations.length === 0) {
    return null;
  }

  return (
    <StyledSticky boundaryElement={`#sticky-augmentations-boundary-${message.id}`}
                  disabled={!isSticky}
                  positionRecheckInterval={400}
                  scrollElement="#sticky-augmentations-container">
      <dl>
        {augmentations.map(({ component: Augmentation, id }) => <Augmentation key={id} message={message} />)}
      </dl>
    </StyledSticky>
  );
};

export default withTheme(MessageAugmentations);
