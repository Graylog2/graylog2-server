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
import PropTypes from 'prop-types';
import styled from 'styled-components';
import type { $PropertyType } from 'utility-types';

import type { EditWidgetComponentProps } from 'views/types';
import usePluginEntities from 'hooks/usePluginEntities';
import type SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import { Row, Col, Checkbox } from 'components/bootstrap';
import CustomPropTypes from 'views/components/CustomPropTypes';
import FieldSortSelect from 'views/components/widgets/FieldSortSelect';
import SortDirectionSelect from 'views/components/widgets/SortDirectionSelect';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import DescriptionBox from 'views/components/aggregationbuilder/DescriptionBox';
import DecoratorSidebar from 'views/components/messagelist/decorators/DecoratorSidebar';
import { HoverForHelp } from 'components/common';
import { defaultCompare } from 'logic/DefaultCompare';
import SaveOrCancelButtons from 'views/components/widgets/SaveOrCancelButtons';
import StickyBottomActions from 'views/components/widgets/StickyBottomActions';
import FieldsConfiguration from 'views/components/widgets/FieldsConfiguration';

const FullHeightRow = styled(Row)`
  height: 100%;
  padding-bottom: 15px;
  flex: 1;
`;

const FullHeightCol = styled(Col)`
  height: 100%;
  overflow: auto;
`;

const PreviewOptionCheckbox = styled(Checkbox)`
  label {
    display: flex;
    justify-content: space-between;
  }
`;

const _onFieldSelectionChanged = (fields: Array<string>, config: MessagesWidgetConfig, onChange: (newConfig: MessagesWidgetConfig) => void) => {
  const newConfig = config.toBuilder().fields(fields).build();

  return onChange(newConfig);
};

const _onSortChange = (sort: $PropertyType<AggregationWidgetConfig, 'sort'>, config, onChange) => {
  const newConfig = config.toBuilder().sort(sort).build();

  return onChange(newConfig);
};

const _onSortDirectionChange = (direction: SortConfig['direction'], config, onChange) => {
  const newConfig = config.toBuilder().sort(config.sort.map((sort) => sort.toBuilder().direction(direction).build())).build();

  return onChange(newConfig);
};

const EditMessageList = ({ children, config, fields, onChange, onCancel, onSubmit }: EditWidgetComponentProps<MessagesWidgetConfig>) => {
  const { sort } = config;
  const [sortDirection] = (sort || []).map((s) => s.direction);
  const onDecoratorsChange = (newDecorators) => onChange(config.toBuilder().decorators(newDecorators).build());
  const messagePreviewOptions = usePluginEntities('views.components.widgets.messageTable.previewOptions');
  const sortedMessagePreviewOptions = messagePreviewOptions.sort((o1, o2) => defaultCompare(o1.sort, o2.sort));

  return (
    <FullHeightRow>
      <FullHeightCol md={3}>
        <StickyBottomActions actions={<SaveOrCancelButtons onCancel={onCancel} onSubmit={onSubmit} />}
                             alignActionsAtBottom>
          <DescriptionBox description="Fields">
            <FieldsConfiguration onChange={(newFields) => _onFieldSelectionChanged(newFields, config, onChange)}
                                 menuPortalTarget={document.body}
                                 selectedFields={config.fields} />

          </DescriptionBox>
          <DescriptionBox description="Message Preview">
            {sortedMessagePreviewOptions.map((option) => (
              <PreviewOptionCheckbox key={option.title} checked={option.isChecked(config)} onChange={() => option.onChange(config, onChange)} disabled={option.isDisabled(config)}>
                {option.title}
                {option.help && (
                  <HoverForHelp title={option.title}>
                    {option.help}
                  </HoverForHelp>
                )}
              </PreviewOptionCheckbox>
            ))}
          </DescriptionBox>
          <DescriptionBox description="Sorting">
            <FieldSortSelect fields={fields} sort={sort} onChange={(data) => _onSortChange(data, config, onChange)} />
          </DescriptionBox>
          <DescriptionBox description="Direction">
            <SortDirectionSelect disabled={!sort || sort.length === 0}
                                 direction={sortDirection && sortDirection.direction}
                                 onChange={(data) => _onSortDirectionChange(data, config, onChange)} />
          </DescriptionBox>
          <DescriptionBox description="Decorators">
            <DecoratorSidebar decorators={config.decorators}
                              onChange={onDecoratorsChange} />
          </DescriptionBox>
        </StickyBottomActions>
      </FullHeightCol>
      <FullHeightCol md={9}>
        {children}
      </FullHeightCol>
    </FullHeightRow>
  );
};

EditMessageList.propTypes = {
  children: CustomPropTypes.OneOrMoreChildren.isRequired,
  config: PropTypes.object.isRequired,
  fields: CustomPropTypes.FieldListType.isRequired,
  onChange: PropTypes.func.isRequired,
};

export default EditMessageList;
