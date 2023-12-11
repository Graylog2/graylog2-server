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
import upperCase from 'lodash/upperCase';

import { Modal, Button } from 'components/bootstrap';
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import usePluginEntities from 'hooks/usePluginEntities';
import generateId from 'logic/generateId';
import useView from 'views/hooks/useView';
import useAppDispatch from 'stores/useAppDispatch';
import { addWidget } from 'views/logic/slices/widgetActions';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';

const modalTitle = 'Create new widget';

const WidgetList = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0.7rem;
`;

const CreateWidgetButton = styled(Button)(({ theme }) => css`
  background-color: transparent;
  border-color: ${theme.colors.variant.gray};
  border-radius: 4px;
  height: 8rem;
  width: 8rem;
`);

const ButtonInner = styled.div`
  display: flex;
  flex-flow: column nowrap;
  align-items: center;
  word-wrap: break-word;
  white-space: break-spaces;
  text-align: center;
  gap: 0.3rem;
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
  const location = useLocation();
  const sendTelemetry = useSendTelemetry();

  const widgetButtons = useMemo(() => creators.map(({ title, func, icon: WidgetIcon }) => {
    const onClick = async () => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_WIDGET_CREATE[upperCase(title).replace(/ /g, '_')], {
        app_pathname: getPathnameWithoutId(location.pathname),
        app_section: 'search-widget',
      });

      const newId = generateId();
      const newWidget = func({ view }).toBuilder().id(newId).build();

      return dispatch(addWidget(newWidget, position));
    };

    return (
      <CreateWidgetButton key={title} type="button" title={`Create ${title} Widget`} onClick={onClick}>
        <ButtonInner>
          <HugeIcon><WidgetIcon /></HugeIcon>
          {title}
        </ButtonInner>
      </CreateWidgetButton>
    );
  }), [creators, dispatch, location.pathname, position, sendTelemetry, view]);

  return (
    <Modal onHide={onCancel}
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
