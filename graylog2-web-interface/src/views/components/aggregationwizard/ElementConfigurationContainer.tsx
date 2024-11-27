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

import { Icon, IconButton } from 'components/common';
import type { DraggableProps, DragHandleProps } from 'components/common/SortableList';

const Container = styled.div(({ theme }) => css`
  display: flex;
  padding: 6px 5px 3px 7px;
  margin-bottom: 5px;
  border-radius: 3px;
  border: 1px solid ${theme.colors.variant.lighter.default};
  background-color: ${theme.colors.variant.lightest.default};
  flex-direction: column;
  position: relative;
`);

const ElementActions = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: end;
  align-items: center;
  position: absolute;
  right: 0;
  top: 5px;
`;

const StyledIconButton = styled(IconButton)`
  width: fit-content;
  height: fit-content;
`;

const ElementConfiguration = styled.div`
  flex: 1;
  // The min-width is required to avoid an overflow problem with the parent component. 
  min-width: 0;
`;

const DragHandle = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
`;

type Props = {
  children: React.ReactNode,
  onRemove?: () => void,
  elementTitle: string,
  draggableProps?: DraggableProps;
  dragHandleProps?: DragHandleProps;
  className?: string,
  testIdPrefix?: string,
};

const ElementConfigurationContainer = forwardRef<HTMLDivElement, Props>(({ children, onRemove, testIdPrefix = 'configuration', dragHandleProps, className, draggableProps, elementTitle }: Props, ref) => (
  <Container className={className} ref={ref} {...(draggableProps ?? {})}>
    <ElementActions>
      {dragHandleProps && (
      <DragHandle {...dragHandleProps} data-testid={`${testIdPrefix}-drag-handle`}>
        <Icon size="sm" name="drag_indicator" />
      </DragHandle>
      )}
      {onRemove && <StyledIconButton size="sm" onClick={onRemove} name="delete" title={`Remove ${elementTitle}`} />}
    </ElementActions>
    <ElementConfiguration>
      {children}
    </ElementConfiguration>
  </Container>
));

export default ElementConfigurationContainer;
