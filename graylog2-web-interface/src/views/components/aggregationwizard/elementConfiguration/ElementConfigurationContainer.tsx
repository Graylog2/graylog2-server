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
import { forwardRef } from 'react';
import styled, { css } from 'styled-components';
import type { DraggableProvidedDraggableProps, DraggableProvidedDragHandleProps } from 'react-beautiful-dnd';

import { Icon, IconButton } from 'components/common';

const Container = styled.div(({ theme }) => css`
  display: flex;
  padding: 6px 5px 3px 7px;
  margin-bottom: 5px;
  border-radius: 3px;
  border: 1px solid ${theme.colors.variant.lighter.default};
  background-color: ${theme.colors.variant.lightest.default};
`);

const ElementActions = styled.div`
  display: flex;
  flex-direction: column;
  min-width: 25px;
  margin-left: 5px;
`;

const ElementConfiguration = styled.div`
  flex: 1;
`;

const DragHandle = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 25px;
`;

type Props = {
  children: React.ReactNode,
  onRemove?: () => void,
  elementTitle: string,
  draggableProps?: DraggableProvidedDraggableProps;
  dragHandleProps?: DraggableProvidedDragHandleProps;
  className?: string,
};

const ElementConfigurationContainer = forwardRef<HTMLDivElement, Props>(({
  children,
  className,
  draggableProps,
  dragHandleProps,
  elementTitle,
  onRemove,
}: Props, ref) => (
  <Container className={className} ref={ref} {...(draggableProps ?? {})}>
    <ElementConfiguration>
      {children}
    </ElementConfiguration>
    <ElementActions>
      {dragHandleProps && (
        <DragHandle {...dragHandleProps}>
          <Icon name="bars" />
        </DragHandle>
      )}
      {onRemove && <IconButton onClick={onRemove} name="trash" title={`Remove ${elementTitle}`} />}
    </ElementActions>
  </Container>
));

ElementConfigurationContainer.defaultProps = {
  className: undefined,
  draggableProps: undefined,
  dragHandleProps: undefined,
  onRemove: undefined,
};

export default ElementConfigurationContainer;
