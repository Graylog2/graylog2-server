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
import { useContext } from 'react';
import { Formik, Form } from 'formik';
import styled from 'styled-components';

import type { EditWidgetComponentProps } from 'views/types';
import { Row, Col } from 'components/bootstrap';
import Input from 'components/bootstrap/Input';
import DescriptionBox from 'views/components/aggregationbuilder/DescriptionBox';
import SaveOrCancelButtons from 'views/components/widgets/SaveOrCancelButtons';
import StickyBottomActions from 'views/components/widgets/StickyBottomActions';
import FieldSelectBase from 'views/components/aggregationwizard/FieldSelectBase';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import type SwimlaneWidgetConfig from 'views/logic/widgets/SwimlaneWidgetConfig';
import { DEFAULT_LIMIT, DEFAULT_MAX_LANES } from 'views/logic/widgets/SwimlaneWidgetConfig';

const StyledForm = styled(Form)`
  display: flex;
  width: 100%;
`;

const FullHeightRow = styled(Row)`
  height: 100%;
  padding-bottom: 15px;
  flex: 1;
`;

const FullHeightCol = styled(Col)`
  height: 100%;
`;

const Container = styled.div`
  height: 100%;

  .form-group {
    margin: 0 0 8px;
  }
`;

type FormValues = {
  laneField: string;
  colorField: string;
  limit: number;
  maxLanes: number;
};

const SwimlaneEdit = ({ children, config, onChange, onCancel }: EditWidgetComponentProps<SwimlaneWidgetConfig>) => {
  const fieldTypes = useContext(FieldTypesContext);
  const fieldOptions = fieldTypes.currentQuery?.toArray() ?? [];

  const initialValues: FormValues = {
    laneField: config.laneField,
    colorField: config.colorField ?? '',
    limit: config.limit,
    maxLanes: config.maxLanes,
  };

  const onSubmit = ({ laneField, colorField, limit, maxLanes }: FormValues) => {
    onChange(
      config
        .toBuilder()
        .laneField(laneField)
        .colorField(colorField || undefined)
        .limit(Number(limit))
        .maxLanes(Number(maxLanes))
        .build(),
    );
  };

  return (
    <Formik<FormValues> initialValues={initialValues} enableReinitialize onSubmit={onSubmit}>
      {({ values, setFieldValue, submitForm }) => {
        const handleChange = (field: keyof FormValues, value: unknown) => {
          setFieldValue(field, value);
          submitForm();
        };

        return (
          <StyledForm className="form form-horizontal">
            <FullHeightRow>
              <FullHeightCol md={4} lg={3}>
                <Container>
                  <StickyBottomActions actions={<SaveOrCancelButtons onCancel={onCancel} />} alignActionsAtBottom>
                    <DescriptionBox description="Lane field">
                      <FieldSelectBase
                        id="lane-field"
                        name="laneField"
                        placeholder="Select lane field…"
                        value={values.laneField}
                        options={fieldOptions}
                        onChange={(v) => handleChange('laneField', v)}
                      />
                    </DescriptionBox>

                    <DescriptionBox description="Color field (optional)">
                      <FieldSelectBase
                        id="color-field"
                        name="colorField"
                        placeholder="None"
                        clearable
                        value={values.colorField || undefined}
                        options={fieldOptions}
                        onChange={(v) => handleChange('colorField', v ?? '')}
                      />
                    </DescriptionBox>

                    <DescriptionBox description="Max lanes">
                      <Input
                        type="number"
                        id="max-lanes"
                        name="maxLanes"
                        min={1}
                        max={100}
                        value={values.maxLanes}
                        placeholder={String(DEFAULT_MAX_LANES)}
                        onChange={(e) => handleChange('maxLanes', Number((e.target as HTMLInputElement).value))}
                        formGroupClassName=""
                        label=""
                      />
                    </DescriptionBox>

                    <DescriptionBox description="Event limit">
                      <Input
                        type="number"
                        id="limit"
                        name="limit"
                        min={1}
                        max={1000}
                        value={values.limit}
                        placeholder={String(DEFAULT_LIMIT)}
                        onChange={(e) => handleChange('limit', Number((e.target as HTMLInputElement).value))}
                        formGroupClassName=""
                        label=""
                      />
                    </DescriptionBox>
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

export default SwimlaneEdit;
