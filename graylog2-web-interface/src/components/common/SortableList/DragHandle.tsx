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
import { styled, css } from 'styled-components';

import { Icon } from 'components/common';
import type { DragHandleProps } from 'components/common/SortableList/types';

export const DRAG_HANDLE_DEFAULT_TITLE = 'Drag or press space to reorder';

const Container = styled.button<{ $isDragging: boolean }>(
  ({ $isDragging, theme }) => css`
    margin-right: ${theme.spacings.xxs};
    cursor: ${$isDragging ? 'grabbing' : 'grab'};
    background: transparent;
    border: 0;
    padding: 0;
  `,
);

type Props = {
  itemTitle?: string | React.ReactElement;
  dragHandleProps: DragHandleProps;
  isDragging: boolean;
  index: number;
};

const DragHandle = (
  { itemTitle = undefined, dragHandleProps, isDragging, index }: Props,
  ref: React.Ref<HTMLButtonElement>,
) => {
  const dragHandleTitle =
    typeof itemTitle === 'string'
      ? `${DRAG_HANDLE_DEFAULT_TITLE} ${itemTitle.toLocaleLowerCase()}`
      : `${DRAG_HANDLE_DEFAULT_TITLE}`;

  return (
    <Container
      {...dragHandleProps}
      ref={ref}
      $isDragging={isDragging}
      title={dragHandleTitle}
      aria-label={dragHandleTitle}
      data-sortable-index={index}>
      <Icon name="drag_indicator" />
    </Container>
  );
};

export default React.forwardRef(DragHandle);
