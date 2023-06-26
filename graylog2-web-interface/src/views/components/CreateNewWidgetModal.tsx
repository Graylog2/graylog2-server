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
import kebabCase from 'lodash/kebabCase';

import { Modal, Button } from 'components/bootstrap';
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import usePluginEntities from 'hooks/usePluginEntities';
import generateId from 'logic/generateId';
import useView from 'views/hooks/useView';
import useAppDispatch from 'stores/useAppDispatch';
import { addWidget } from 'views/logic/slices/widgetActions';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

const modalTitle = 'Create new widget';

const WidgetList = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
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

  && {
    background: transparent;
    border-radius: 4px;
  }
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
  const view = useView();
  const dispatch = useAppDispatch();
  const sendTelemetry = useSendTelemetry();

  const widgetButtons = useMemo(() => creators.map(({ title, func, icon: WidgetIcon }) => {
    const onClick = async () => {
      sendTelemetry('click', {
        app_pathname: 'search',
        app_section: 'widget',
        app_action_value: `widget-create-${kebabCase(title)}-button`,
      });

      const newId = generateId();
      const newWidget = func({ view }).toBuilder().id(newId).build();

      return dispatch(addWidget(newWidget, position));
    };

    return (
      <CreateWidgetButton type="button" title={`Create ${title} Widget`} onClick={onClick}>
        <HugeIcon><WidgetIcon /></HugeIcon>{title}
      </CreateWidgetButton>
    );
  }), [creators, dispatch, position, sendTelemetry, view]);

  return (
    <Modal onHide={onCancel}
           show
           data-event-element={modalTitle}>
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
