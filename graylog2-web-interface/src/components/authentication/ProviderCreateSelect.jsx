// @flow strict
import * as React from 'react';
import { Formik, Form, Field } from 'formik';
import styled from 'styled-components';

import { availableProvidersOptions, availableProviders } from 'logic/authentication/availableProviders';
import history from 'util/History';
import { Select } from 'components/common';
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

const _onSubmit = ({ authProvider }) => {
  const { route } = availableProviders[authProvider];
  history.push(route);
};

const ProviderCreateSelect = () => (
  <Formik onSubmit={_onSubmit}
          initialValues={{ authProvider: 'ldap' }}>
    {({ isSubmitting, isValid }) => (
      <StyledForm>
        <ElementsContainer>
          <FormGroup className="form-group">
            <Field name="authProvider">
              {({ field: { name, value, onChange } }) => (
                <Select placeholder="Select a provider"
                        inputProps={{ 'aria-label': 'Select a provider' }}
                        options={availableProvidersOptions}
                        matchProp="label"
                        onChange={(authProvider) => onChange({ target: { value: authProvider, name } })}
                        value={value}
                        clearable={false} />
              )}
            </Field>
          </FormGroup>
            &nbsp;
          <Button bsStyle="success" type="submit" disabled={isSubmitting || !isValid}>Get started</Button>
        </ElementsContainer>
      </StyledForm>
    )}
  </Formik>
);

export default ProviderCreateSelect;
