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
import { useMemo } from 'react';
import styled, { css } from 'styled-components';

import { Modal, Button } from 'components/bootstrap';
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import usePluginEntities from 'hooks/usePluginEntities';
import { WidgetActions } from 'views/stores/WidgetStore';
import generateId from 'logic/generateId';
import { CurrentViewStateActions } from 'views/stores/CurrentViewStateStore';
import { useStore } from 'stores/connect';
import { ViewStore } from 'views/stores/ViewStore';

const modalTitle = 'Create new widget';

const WidgetList = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: space-evenly;
`;

const CreateWidgetButton = styled(Button)`
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;  
  text-align: center;
  padding: 10px;
  width: 8rem;
  white-space: normal;
`;

const HugeIcon = styled.div(({ theme }) => css`
  font-size: ${theme.fonts.size.huge};
`);

type Props = {
  onCancel: () => void,
  position: WidgetPosition,
}

const CreateNewWidgetModal = ({ onCancel, position }: Props) => {
  const creators = usePluginEntities('widgetCreators');
  const view = useStore(ViewStore, (store) => store?.view);

  const widgetButtons = useMemo(() => creators.map(({ title, func, icon: WidgetIcon }) => {
    const onClick = async () => {
      const newId = generateId();
      await CurrentViewStateActions.updateWidgetPosition(newId, position);
      const newWidget = func({ view }).toBuilder().id(newId).build();

      return WidgetActions.create(newWidget);
    };

    return (
      <CreateWidgetButton type="button" onClick={onClick}><HugeIcon><WidgetIcon /></HugeIcon>{title}</CreateWidgetButton>
    );
  }), [creators, position, view]);

  return (
    <Modal title={modalTitle}
           onHide={onCancel}
           show>
      <Modal.Header closeButton>
        <Modal.Title>{modalTitle}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <WidgetList>
          {widgetButtons}
        </WidgetList>
      </Modal.Body>
      <Modal.Footer>
        <Button type="button" onClick={onCancel}>
          Cancel
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default CreateNewWidgetModal;
