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
import { useState } from 'react';

import { IconButton, ModalSubmit } from 'components/common';
import { ButtonToolbar, Modal } from 'components/bootstrap';
import usePluginEntities from 'hooks/usePluginEntities';

const EventDetails = ({ eventId }: { eventId: string }) => {
  const puggableEventDetails = usePluginEntities('views.components.widgets.events.detailsComponent');

  console.log(puggableEventDetails);

  if (puggableEventDetails?.length) {
    const { component: Component } = puggableEventDetails[0];

    return <Component eventId={eventId} />;
  }

  return <>default</>;
};

const RowActions = ({ eventId }: { eventId: string }) => {
  const [showDetailsModal, setShowDetailsModal] = useState(false);

  const toggleDetailsModal = () => setShowDetailsModal((cur) => !cur);

  return (
    <>
      <ButtonToolbar>
        <IconButton name="open_in_full" title="View event details" onClick={toggleDetailsModal} />
        <IconButton name="more_vert" title="Toggle event actions" />
      </ButtonToolbar>
      {showDetailsModal && (
      <Modal show={showDetailsModal}
             bsSize="large"
             onHide={toggleDetailsModal}>
        <Modal.Header closeButton>
          <Modal.Title>Event details</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <EventDetails eventId={eventId} />
        </Modal.Body>
        <Modal.Footer>
          <ModalSubmit displayCancel={false}
                       onSubmit={toggleDetailsModal}
                       submitButtonType="button"
                       submitButtonText="Close" />
        </Modal.Footer>
      </Modal>
      )}
    </>

  );
};

export default RowActions;
