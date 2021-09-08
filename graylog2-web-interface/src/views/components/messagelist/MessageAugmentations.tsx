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

import usePluginEntities from 'views/logic/usePluginEntities';
import { Message } from 'views/components/messagelist/Types';

type Props = {
  message: Message,
}

const MessageAugmentations = ({ message }: Props) => {
  const augmentations = usePluginEntities('messageAugmentations');

  if (!augmentations || augmentations.length === 0) {
    return null;
  }

  return (
    <Sticky scrollElement="#sticky-augmentations-container" boundaryElement={`#sticky-augmentations-boundary-${message.id}`} positionRecheckInterval={400}>
      <dl>
        {augmentations.map(({ component: Augmentation, id }) => <Augmentation key={id} message={message} />)}
      </dl>
    </Sticky>
  );
};

export default MessageAugmentations;
