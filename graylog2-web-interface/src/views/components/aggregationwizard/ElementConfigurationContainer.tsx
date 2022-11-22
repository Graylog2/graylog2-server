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
import { useFormikContext } from 'formik';

import { Icon, IconButton } from 'components/common';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';

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

const RemoveElementButton = ({ onRemove, elementTitle, index }: {
  onRemove: (index: number, values: WidgetConfigFormValues) => void,
  elementTitle: string,
  index: number,
}) => {
  const { values } = useFormikContext();
  const onClick = () => onRemove(index, values);

  return (
    <IconButton onClick={onClick}
                name="trash-alt"
                title={`Remove ${elementTitle}`} />
  );
};

type Props = {
  children: React.ReactNode,
  className?: string,
  dragHandleProps?: DraggableProvidedDragHandleProps;
  draggableProps?: DraggableProvidedDraggableProps;
  elementTitle: string,
  index?: number,
  onRemove?: (index: number, values: WidgetConfigFormValues) => void,
  testIdPrefix?: string,
};

const ElementConfigurationContainer = forwardRef<HTMLDivElement, Props>(({
  children,
  className,
  dragHandleProps,
  draggableProps,
  elementTitle,
  index,
  onRemove,
  testIdPrefix,
}: Props, ref) => (
  <Container className={className} ref={ref} {...(draggableProps ?? {})}>
    <ElementConfiguration>
      {children}
    </ElementConfiguration>
    <ElementActions>
      {dragHandleProps && (
        <DragHandle {...dragHandleProps} data-testid={`${testIdPrefix}-drag-handle`}>
          <Icon name="bars" />
        </DragHandle>
      )}
      {onRemove && (
        <RemoveElementButton onRemove={onRemove}
                             index={index}
                             elementTitle={elementTitle} />
      )}
    </ElementActions>
  </Container>
));

ElementConfigurationContainer.defaultProps = {
  className: undefined,
  draggableProps: undefined,
  dragHandleProps: undefined,
  onRemove: undefined,
  index: undefined,
  testIdPrefix: 'configuration',
};

export default React.memo(ElementConfigurationContainer);
