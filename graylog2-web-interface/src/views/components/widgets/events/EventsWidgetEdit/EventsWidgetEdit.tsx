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
import { useCallback, useMemo, useEffect } from 'react';
import styled from 'styled-components';
import Immutable from 'immutable';
import { Formik, Form, useFormikContext } from 'formik';

import type { EditWidgetComponentProps } from 'views/types';
import type { DirectionJson } from 'views/logic/aggregationbuilder/Direction';
import Direction from 'views/logic/aggregationbuilder/Direction';
import { Row, Col } from 'components/bootstrap';
import DescriptionBox from 'views/components/aggregationbuilder/DescriptionBox';
import SaveOrCancelButtons from 'views/components/widgets/SaveOrCancelButtons';
import StickyBottomActions from 'views/components/widgets/StickyBottomActions';
import type { VisualizationType, Filter } from 'views/logic/widgets/events/EventsWidgetConfig';
import EventsWidgetConfig, {
  LIST_MODE,
  NUMBER_MODE,
} from 'views/logic/widgets/events/EventsWidgetConfig';
import WidgetModeConfiguration from 'views/components/widgets/events/EventsWidgetEdit/WidgetModeConfiguration';

import SortConfiguration from './SortConfiguration';
import ColumnsConfiguration from './ColumnsConfiguration';

import { SORT_DIRECTION_OPTIONS, EVENT_ATTRIBUTES } from '../Constants';

const COLUMNS = Object.keys(EVENT_ATTRIBUTES);

// const ATTRIBUTES_FILTER: AttributesFilter = {
//   created_at: {
//     configuration: (filterValues, setFilterValues) => <div>filter</div>,
//     valuesForConfig: (filterValues) => [filterValues.map(({ value }) => value)?.join(',')],
//     valueFromConfig: (value: string) => value.split(',').map((dateTime) => ({ value: dateTime, label: dateTime, type: 'date' })),
//     renderValue: (values) => values.replace(',', ' to '),
//   },
// };

const WIDGET_MODE_OPTIONS = [
  { label: 'List', value: LIST_MODE },
  { label: 'Count', value: NUMBER_MODE },
];

type FormValues = {
  mode: VisualizationType,
  fields: Immutable.OrderedSet<string>,
  filters: Immutable.OrderedSet<Filter>,
  sort: { field: string, direction: DirectionJson },
}

const StyledForm = styled(Form)`
  display: flex;
  width: 100%;
`;

const Container = styled.div`
  height: 100%;
  
  .form-group {
    margin: 0 0 3px;
    
    &:last-child {
      margin: 0;
    }
    
    .control-label {
      padding-left: 0;
      padding-right: 0;
      padding-top: 5px;
      font-weight: normal;
      text-align: left;
      hyphens: auto;
    }
  }

  div[class^="col-"] {
    padding-right: 0;
    padding-left: 0;
  }
`;

const FullHeightRow = styled(Row)`
  height: 100%;
  padding-bottom: 15px;
  flex: 1;
`;

const FullHeightCol = styled(Col)`
  height: 100%;
  overflow: auto;
`;

const SORTABLE_COLUMNS = Object.entries(EVENT_ATTRIBUTES)
  .filter(([, { sortable }]) => sortable)
  .map(([columnName]) => (columnName));

const SubmitOnChange = () => {
  const { values, submitForm } = useFormikContext();

  useEffect(
    () => {
      submitForm();
    }, [values, submitForm],
  );

  // eslint-disable-next-line react/jsx-no-useless-fragment
  return <></>;
};

const EventsWidgetEdit = ({ children, onCancel, config, onChange, onSubmit }: EditWidgetComponentProps<EventsWidgetConfig>) => {
  const initialValues = useMemo(() => ({
    mode: config.mode,
    filters: config.filters,
    fields: config.fields,
    sort: { field: config.sort.field, direction: config.sort.direction.direction },
  }), [config.fields, config.filters, config.sort.direction, config.sort.field, config.mode]);

  const _onSubmit = ({ sort, mode, filters, fields }: FormValues) => {
    const newSort = config.sort.toBuilder()
      .field(sort.field)
      .direction(Direction.fromJSON(sort.direction))
      .build();

    onChange(
      config.toBuilder()
        .mode(mode)
        .filters(filters)
        .fields(fields)
        .sort(newSort)
        .build(),
    );
  };

  const columnTitle = useCallback((column: string) => EVENT_ATTRIBUTES[column]?.title ?? column, []);

  return (
    <Formik<FormValues> initialValues={initialValues}
                        enableReinitialize
                        validateOnChange
                        validateOnMount
                        onSubmit={_onSubmit}>
      {({ setValues, values }) => {
        const onChangeType = (newMode: VisualizationType) => {
          if (newMode !== values.mode) {
            if (newMode === NUMBER_MODE) {
              setValues({ ...values, fields: Immutable.OrderedSet(), filters: Immutable.OrderedSet() });
            }

            if (newMode === LIST_MODE) {
              setValues({
                ...values,
                fields: EventsWidgetConfig.defaultFields,
                sort: {
                  field: EventsWidgetConfig.defaultSort.field,
                  direction: EventsWidgetConfig.defaultSort.direction.direction,
                },
              });
            }
          }
        };

        return (

          <StyledForm className="form form-horizontal">
            <SubmitOnChange />
            <FullHeightRow>
              <FullHeightCol md={4} lg={3}>
                <Container>
                  <StickyBottomActions actions={<SaveOrCancelButtons onCancel={onCancel} onSubmit={onSubmit} />}
                                       alignActionsAtBottom>
                    <DescriptionBox description="Visualization Type">
                      <WidgetModeConfiguration name="mode" onChange={onChangeType} options={WIDGET_MODE_OPTIONS} />
                    </DescriptionBox>
                    {values.mode === LIST_MODE && (
                      <DescriptionBox description="Columns">
                        <ColumnsConfiguration columns={COLUMNS}
                                              createSelectPlaceholder="Select a new column"
                                              name="fields"
                                              menuPortalTarget={document.body}
                                              columnTitle={columnTitle} />
                      </DescriptionBox>
                    )}

                    {values.mode === LIST_MODE && (
                      <DescriptionBox description="Sorting">
                        <SortConfiguration columns={SORTABLE_COLUMNS}
                                           name="sort"
                                           columnTitle={columnTitle}
                                           directions={SORT_DIRECTION_OPTIONS} />
                      </DescriptionBox>
                    )}
                  </StickyBottomActions>
                </Container>
              </FullHeightCol>
              <FullHeightCol md={8} lg={9}>
                {children}
              </FullHeightCol>
            </FullHeightRow>
          </StyledForm>
        );
      }}
    </Formik>
  );
};

export default EventsWidgetEdit;
