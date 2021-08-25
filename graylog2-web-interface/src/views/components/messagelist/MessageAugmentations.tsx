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

import usePluginEntities from 'views/logic/usePluginEntities';

const AugmentationsContainer = styled.dl`
  position: sticky;
  top: 0;
`;

const MessageAugmentations = () => {
  const augmentations = usePluginEntities('messageAugmentations');

  if (!augmentations || augmentations.length === 0) {
    return null;
  }

  return (
    <AugmentationsContainer>
      {augmentations.map(({ component: Augmentation, id }) => <Augmentation key={id} />)}
    </AugmentationsContainer>
  );
};

export default MessageAugmentations;
