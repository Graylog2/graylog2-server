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
import { Formik, Form, Field } from 'formik';
import { PluginStore } from 'graylog-web-plugin/plugin';
import styled from 'styled-components';

import Routes from 'routing/Routes';
import { validateField } from 'util/FormsUtils';
import { defaultCompare } from 'logic/DefaultCompare';
import { Select, InputDescription } from 'components/common';
import { Button } from 'components/bootstrap';
import type { HistoryFunction } from 'routing/useHistory';
import useHistory from 'routing/useHistory';
import { getPathnameWithoutId } from 'util/URLUtils';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const ElementsContainer = styled.div`
  display: flex;
  align-items: start;
  width: 100%;
`;

const StyledForm = styled(Form)`
  max-width: 360px;
  width: 100%;
`;

const FormGroup = styled.div`
  flex: 1;
`;

type FormState = { authServiceType: string };

const _onSubmit = (history: HistoryFunction, { authServiceType }: FormState) => {
  const createRoute = Routes.SYSTEM.AUTHENTICATION.BACKENDS.createBackend(authServiceType);
  history.push(createRoute);
};

const BackendCreateSelect = () => {
  const authServices = PluginStore.exports('authentication.services');
  const sortedAuthServices = authServices.sort((s1, s2) => defaultCompare(s1.displayName, s2.displayName));
  const authServicesOptions = sortedAuthServices.map((service) => ({ label: service.displayName, value: service.name }));
  const history = useHistory();
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  const onSubmit = useCallback((formState: FormState) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.AUTHENTICATION.SERVICE_CREATED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'services',
      app_action_value: 'create-service-form',
    });

    _onSubmit(history, formState);
  }, [history, pathname, sendTelemetry]);

  return (
    <Formik onSubmit={onSubmit} initialValues={{ authServiceType: undefined }}>
      {({ isSubmitting, isValid }) => (
        <StyledForm>
          <ElementsContainer>
            <FormGroup className="form-group">
              <Field name="authServiceType" validate={validateField({ required: true })}>
                {({ field: { name, value, onChange }, meta: { error } }) => (
                  <>
                    <Select clearable={false}
                            onChange={(authService) => {
                              sendTelemetry(TELEMETRY_EVENT_TYPE.AUTHENTICATION.SERVICE_SELECTED, {
                                app_pathname: getPathnameWithoutId(pathname),
                                app_section: 'services',
                                app_action_value: 'selectservice',
                              });

                              onChange({ target: { value: authService, name } });
                            }}
                            options={authServicesOptions}
                            placeholder="Select a service"
                            value={value} />
                    <InputDescription error={error} />
                  </>
                )}
              </Field>
            </FormGroup>
            &nbsp;
            <Button bsStyle="success"
                    disabled={isSubmitting || !isValid}
                    type="submit">
              Get started
            </Button>
          </ElementsContainer>
        </StyledForm>
      )}
    </Formik>
  );
};

export default BackendCreateSelect;
