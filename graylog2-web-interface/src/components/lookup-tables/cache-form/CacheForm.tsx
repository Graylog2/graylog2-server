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
import styled from 'styled-components';
import { Formik, Form } from 'formik';

import { Col, Row } from 'components/bootstrap';
import { FormSubmit } from 'components/common';
import { useCreateCache, useUpdateCache } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import useScopePermissions from 'hooks/useScopePermissions';
import usePluginEntities from 'hooks/usePluginEntities';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { LookupTableCache } from 'logic/lookup-tables/types';

import CacheFormFields from './CacheFormFields';

const INIT_CACHE: LookupTableCache = {
  id: undefined,
  title: '',
  description: '',
  name: '',
  config: {},
};

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

type Props = {
  type: string;
  title: string;
  saved: (response: any) => void;
  onCancel: () => void;
  create?: boolean;
  cache?: LookupTableCache;
};

function CacheForm({ type, saved, title, onCancel, create = false, cache = INIT_CACHE }: Props) {
  const sendTelemetry = useSendTelemetry();
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(cache);
  const { createCache, creatingCache } = useCreateCache();
  const { updateCache, updatingCache } = useUpdateCache();

  const cachePlugins = usePluginEntities('lookupTableCaches');
  const plugin = React.useMemo(() => cachePlugins.find((p) => p.type === type), [cachePlugins, type]);

  const DocComponent = React.useMemo(() => plugin?.documentationComponent, [plugin]);
  const pluginDisplayName = React.useMemo(() => plugin?.displayName || type, [plugin, type]);

  const handleSubmit = async (values: LookupTableCache) => {
    const promise = create ? createCache(values) : updateCache(values);

    return promise.then((response) => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.LUT[create ? 'CACHE_CREATED' : 'CACHE_UPDATED'], {
        app_pathname: 'lut',
        app_section: 'lut_cache',
        event_details: {
          type: values?.config?.type,
        },
      });

      saved(response);
    });
  };

  const canModify = React.useMemo(
    () => !create && !loadingScopePermissions && scopePermissions?.is_mutable,
    [create, loadingScopePermissions, scopePermissions?.is_mutable],
  );

  return (
    <>
      <Title title={title} typeName={pluginDisplayName} create={create} />
      <Formik initialValues={cache} onSubmit={handleSubmit} validateOnBlur={false} enableReinitialize>
        {({ isSubmitting, isValid }) => (
          <FlexForm className="form form-horizontal">
            <Row style={{ flexGrow: 1 }}>
              <Col lg={6}>
                <CacheFormFields />
              </Col>
              <Col lg={6}>{DocComponent ? <DocComponent /> : null}</Col>
            </Row>
            <Row>
              {canModify && (
                <Col md={3} mdOffset={9}>
                  <FormSubmit
                    submitButtonText="Create cache"
                    submitLoadingText={create ? 'Creating cache...' : 'Updating cache...'}
                    isSubmitting={isSubmitting || creatingCache || updatingCache}
                    disabledSubmit={isSubmitting || creatingCache || updatingCache || !isValid}
                    isAsyncSubmit
                    onCancel={onCancel}
                  />
                </Col>
              )}
            </Row>
          </FlexForm>
        )}
      </Formik>
    </>
  );
}

export default CacheForm;
