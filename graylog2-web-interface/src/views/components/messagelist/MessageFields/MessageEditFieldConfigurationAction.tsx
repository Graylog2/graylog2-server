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

import React, { useState, useCallback } from 'react';

import { Button } from 'components/bootstrap';
import MessageFieldsEditModal from 'views/components/messagelist/MessageFields/MessageFieldsEditModal';
import useSendFavoriteFieldTelemetry from 'views/components/messagelist/MessageFields/hooks/useSendFavoriteFieldTelemetry';

const MessageEditFieldConfigurationAction = () => {
  const [showModal, setShowModal] = useState(false);
  const sendFavoriteFieldTelemetry = useSendFavoriteFieldTelemetry();

  const toggleEditMode = useCallback(() => setShowModal((prev) => !prev), []);

  const onClick = useCallback(() => {
    sendFavoriteFieldTelemetry('EDIT_OPEN');

    return toggleEditMode();
  }, [sendFavoriteFieldTelemetry, toggleEditMode]);

  return (
    <>
      <Button bsSize="small" onClick={onClick} title="Edit favorite fields">
        Edit favorite fields
      </Button>
      {showModal && <MessageFieldsEditModal toggleEditMode={toggleEditMode} />}
    </>
  );
};

export default MessageEditFieldConfigurationAction;
