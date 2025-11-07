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
import React from 'react';
import styled from 'styled-components';
import { Formik, Form } from 'formik';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { FormSubmit } from 'components/common';
import { Col, Row } from 'components/bootstrap';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { useCreateAdapter, useUpdateAdapter } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { LookupTableAdapter } from 'logic/lookup-tables/types';

import AdapterFormFields from './AdapterFormFields';

const FlexForm = styled(Form)`
  display: flex;
  flex-direction: column;
  flex-grow: 1 !important;
`;

type TitleProps = {
  title: string;
  typeName: string;
  create: boolean;
};

const Title = ({ title, typeName, create }: TitleProps) => {
  const TagName = create ? 'h3' : 'h2';

  return (
    <TagName style={{ marginBottom: '12px' }}>
      {title} <small>({typeName})</small>
    </TagName>
  );
};

const INIT_ADAPTER = {
  id: undefined,
  title: '',
  description: '',
  name: '',
  custom_error_ttl_enabled: false,
  custom_error_ttl: null,
  custom_error_ttl_unit: null,
  config: {},
};

type Props = {
  type: string;
  title: string;
  saved: (response: any) => void;
  onCancel: () => void;
  create?: boolean;
  dataAdapter?: LookupTableAdapter;
};

const DataAdapterForm = ({ type, title, saved, onCancel, create = false, dataAdapter = INIT_ADAPTER }: Props) => {
  const sendTelemetry = useSendTelemetry();
  const { createAdapter, creatingAdapter } = useCreateAdapter();
  const { updateAdapter, updatingAdapter } = useUpdateAdapter();

  const plugin = React.useMemo(() => PluginStore.exports('lookupTableAdapters').find((p) => p.type === type), [type]);

  const DocComponent = React.useMemo(() => plugin.documentationComponent, [plugin]);
  const pluginDisplayName = React.useMemo(() => plugin.displayName || type, [plugin, type]);

  const handleSubmit = async (values: LookupTableAdapter) => {
    const promise = create ? createAdapter(values) : updateAdapter(values);

    return promise.then((response) => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.LUT[create ? 'DATA_ADAPTER_CREATED' : 'DATA_ADAPTER_UPDATED'], {
        app_pathname: 'lut',
        app_section: 'lut_data_adapter',
        event_details: {
          type: dataAdapter?.config?.type,
        },
      });

      saved(response);
    });
  };

  return (
    <>
      <Title title={title} typeName={pluginDisplayName} create={create} />
      <Formik initialValues={dataAdapter} onSubmit={handleSubmit} validateOnBlur={false} enableReinitialize>
        {({ isSubmitting, isValid }) => (
          <FlexForm className="form form-horizontal">
            <Row style={{ flexGrow: 1 }}>
              <Col lg={6} style={{ marginTop: 10 }}>
                <AdapterFormFields />
              </Col>
              <Col lg={6} style={{ marginTop: 10 }}>
                {DocComponent ? <DocComponent dataAdapterId={dataAdapter?.id} /> : null}
              </Col>
            </Row>
            <Row style={{ marginBottom: 20 }}>
              <Col mdOffset={9} md={3}>
                <FormSubmit
                  submitButtonText={create ? 'Create adapter' : 'Update adapter'}
                  disabledSubmit={isSubmitting || creatingAdapter || updatingAdapter || !isValid}
                  onCancel={onCancel}
                />
              </Col>
            </Row>
          </FlexForm>
        )}
      </Formik>
    </>
  );
};

export default DataAdapterForm;
