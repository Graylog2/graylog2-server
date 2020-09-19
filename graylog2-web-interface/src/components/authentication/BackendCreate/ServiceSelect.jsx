// @flow strict
import * as React from 'react';
import { Formik, Form, Field } from 'formik';
import styled from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import FormsUtils from 'util/FormsUtils';
import { defaultCompare } from 'views/logic/DefaultCompare';
import history from 'util/History';
import { Select, FieldError } from 'components/common';
import { Button } from 'components/graylog';
import Routes from 'routing/Routes';

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

const _onSubmit = ({ authenticationService }) => {
  const createRoute = Routes.SYSTEM.AUTHENTICATION.PROVIDERS.CREATE_BACKEND(authenticationService);
  history.push(createRoute);
};

const BackendCreateSelect = () => {
  const authServices = PluginStore.exports('authentication.services');
  const sortedAuthServices = authServices.sort((s1, s2) => defaultCompare(s1.displayName, s2.displayName));
  const authServicesOptions = sortedAuthServices.map((service) => ({ label: service.displayName, value: service.name }));

  return (
    <Formik onSubmit={_onSubmit} initialValues={{}}>
      {({ isSubmitting, isValid }) => (
        <StyledForm>
          <ElementsContainer>
            <FormGroup className="form-group">
              <Field name="authenticationService" validate={FormsUtils.validation.isRequired('provider')}>
                {({ field: { name, value, onChange }, meta: { error } }) => (
                  <>
                    <Select placeholder="Select a provider"
                            inputProps={{ 'aria-label': 'Select a provider' }}
                            options={authServicesOptions}
                            onChange={(authService) => onChange({ target: { value: authService, name } })}
                            value={value}
                            clearable={false} />
                    {error && <FieldError>{error}</FieldError>}
                  </>
                )}
              </Field>
            </FormGroup>
            &nbsp;
            <Button bsStyle="success"
                    type="submit"
                    disabled={isSubmitting || !isValid}>
              Get started
            </Button>
          </ElementsContainer>
        </StyledForm>
      )}
    </Formik>
  );
};

export default BackendCreateSelect;
