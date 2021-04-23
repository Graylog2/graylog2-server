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
import { FieldArray, useFormikContext } from 'formik';

import { SortableList } from 'components/common';
import Sort from 'views/components/aggregationwizard/elementConfiguration/Sort';
import ElementConfigurationContainer
  from 'views/components/aggregationwizard/elementConfiguration/ElementConfigurationContainer';
import { WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';

import SortElement from '../aggregationElements/SortElement';

const SortConfiguration = () => {
  const { values: { sort }, setFieldValue } = useFormikContext<WidgetConfigFormValues>();

  return (
    <FieldArray name="sort"
                render={({ remove }) => (
                  <SortableList items={sort}
                                onMoveItem={(newSort) => setFieldValue('sort', newSort)}
                                customListItemRender={({ index, dragHandleProps, draggableProps, className, ref }) => (
                                  <ElementConfigurationContainer key={`sort-${index}`}
                                                                 dragHandleProps={dragHandleProps}
                                                                 draggableProps={draggableProps}
                                                                 className={className}
                                                                 testIdPrefix={`sort-${index}`}
                                                                 onRemove={() => remove(index)}
                                                                 elementTitle={SortElement.title}
                                                                 ref={ref}>
                                    <Sort index={index} />
                                  </ElementConfigurationContainer>
                                )} />
                )} />
  );
};

export default SortConfiguration;
