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
import { useCallback } from 'react';
import { FieldArray, useFormikContext } from 'formik';
import type { DraggableProvidedDragHandleProps, DraggableProvidedDraggableProps } from 'react-beautiful-dnd';

import { SortableList } from 'components/common';
import type { WidgetConfigFormValues, SortFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';

import SortConfiguration from './SortConfiguration';
import SortElement from './SortElement';

import ElementConfigurationContainer from '../ElementConfigurationContainer';

type ListItemProps = {
  /* eslint-disable react/no-unused-prop-types */
  className: string,
  dragHandleProps: DraggableProvidedDragHandleProps,
  draggableProps: DraggableProvidedDraggableProps,
  index: number,
  item: SortFormValues,
  ref: React.Ref<HTMLDivElement>,
  /* eslint-enable react/no-unused-prop-types */
}

const SortsConfiguration = () => {
  const { values: { sort }, setFieldValue } = useFormikContext<WidgetConfigFormValues>();

  const onChangeSort = useCallback(
    (newSort: Array<SortFormValues>) => setFieldValue('sort', newSort),
    [setFieldValue],
  );

  const customListItemRender = useCallback(({
    className,
    dragHandleProps,
    draggableProps,
    index,
    item,
    ref,
  }: ListItemProps) => (
    <ElementConfigurationContainer key={`sort-${item.id}`}
                                   dragHandleProps={dragHandleProps}
                                   draggableProps={draggableProps}
                                   className={className}
                                   testIdPrefix={`sort-${index}`}
                                   index={index}
                                   onRemove={SortElement.onRemove}
                                   elementTitle={SortElement.title}
                                   ref={ref}>
      <SortConfiguration index={index} />
    </ElementConfigurationContainer>
  ), []);

  return (
    <FieldArray name="sort"
                validateOnChange={false}
                render={() => (
                  <SortableList<SortFormValues> items={sort}
                                                onMoveItem={onChangeSort}
                                                customListItemRender={customListItemRender} />
                )} />
  );
};

export default React.memo(SortsConfiguration);
