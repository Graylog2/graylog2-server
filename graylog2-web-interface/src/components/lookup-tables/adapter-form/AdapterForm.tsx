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
import React, { useMemo } from 'react';
import styled from 'styled-components';
import { Formik, Form } from 'formik';

import { Col, Row, RowContainer } from 'components/lookup-tables/layout-componets';
import { FormSubmit } from 'components/common';
import { useCreateAdapter, useUpdateAdapter } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import useScopePermissions from 'hooks/useScopePermissions';
import usePluginEntities from 'hooks/usePluginEntities';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { LookupTableAdapter } from 'logic/lookup-tables/types';

import AdapterFormFields from './AdapterFormFields';

const FlexForm = styled(Form)`
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  width: 100%;
`;

type TitleProps = {
  title: string;
  typeName: string;
  create: boolean;
};

const Title = ({ title, typeName, create }: TitleProps) => {
  const TagName = create ? 'h3' : 'h2';

  return (
    <TagName style={{ marginBottom: '12px', width: '100%' }}>
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
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(dataAdapter);
  const { createAdapter, creatingAdapter } = useCreateAdapter();
  const { updateAdapter, updatingAdapter } = useUpdateAdapter();

  const adapterPlugins = usePluginEntities('lookupTableAdapters');
  const plugin = useMemo(() => adapterPlugins.find((p) => p.type === type), [adapterPlugins, type]);

  const DocComponent = useMemo(() => plugin?.documentationComponent, [plugin]);
  const pluginDisplayName = useMemo(() => plugin?.displayName || type, [plugin, type]);

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

  const canModify = useMemo(
    () => create || (!loadingScopePermissions && scopePermissions?.is_mutable),
    [create, loadingScopePermissions, scopePermissions?.is_mutable],
  );

  return (
    <RowContainer style={{ flexGrow: 1 }} $withDocs={!!DocComponent}>
      <Col style={{ flexGrow: 1, height: '100%' }}>
        <Title title={title} typeName={pluginDisplayName} create={create} />
        <Formik initialValues={dataAdapter} onSubmit={handleSubmit} validateOnBlur={false} enableReinitialize>
          {({ isSubmitting, isValid }) => (
            <FlexForm className="form form-horizontal">
              <Row $gap="xl" style={{ flexGrow: 1 }}>
                <div style={{ width: DocComponent ? '60%' : '100%' }}>
                  <AdapterFormFields />
                </div>
                {DocComponent && (
                  <div style={{ width: '40%', flexGrow: 0 }}>
                    <DocComponent dataAdapterId={dataAdapter?.id} />
                  </div>
                )}
              </Row>
              {canModify && (
                <Row $align="center" $justify="flex-end">
                  <FormSubmit
                    submitButtonText={create ? 'Create adapter' : 'Update adapter'}
                    submitLoadingText={create ? 'Creating adapter...' : 'Updating adapter...'}
                    isSubmitting={isSubmitting || creatingAdapter || updatingAdapter}
                    disabledSubmit={isSubmitting || creatingAdapter || updatingAdapter || !isValid}
                    isAsyncSubmit
                    onCancel={onCancel}
                  />
                </Row>
              )}
            </FlexForm>
          )}
        </Formik>
      </Col>
    </RowContainer>
  );
};

export default DataAdapterForm;
