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
import { Formik, Form, Field } from 'formik';
import { PluginStore } from 'graylog-web-plugin/plugin';
import styled from 'styled-components';

import Routes from 'routing/Routes';
import { validateField } from 'util/FormsUtils';
import history from 'util/History';
import { defaultCompare } from 'views/logic/DefaultCompare';
import { Select, InputDescription } from 'components/common';
import { Button } from 'components/graylog';

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

const _onSubmit = ({ authServiceType }) => {
  const createRoute = Routes.SYSTEM.AUTHENTICATION.BACKENDS.createBackend(authServiceType);
  history.push(createRoute);
};

const BackendCreateSelect = () => {
  const authServices = PluginStore.exports('authentication.services');
  const sortedAuthServices = authServices.sort((s1, s2) => defaultCompare(s1.displayName, s2.displayName));
  const authServicesOptions = sortedAuthServices.map((service) => ({ label: service.displayName, value: service.name }));

  return (
    <Formik onSubmit={_onSubmit} initialValues={{ authServiceType: undefined }}>
      {({ isSubmitting, isValid }) => (
        <StyledForm>
          <ElementsContainer>
            <FormGroup className="form-group">
              <Field name="authServiceType" validate={validateField({ required: true })}>
                {({ field: { name, value, onChange }, meta: { error } }) => (
                  <>
                    <Select clearable={false}
                            inputProps={{ 'aria-label': 'Select a service' }}
                            onChange={(authService) => onChange({ target: { value: authService, name } })}
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
