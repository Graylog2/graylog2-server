// @flow strict
import * as React from 'react';
import { useContext, useEffect, useRef } from 'react';
import { Formik, Form } from 'formik';

import { FormikFormGroup } from 'components/common';
import { Button } from 'components/graylog';

import ServiceStepsContext from '../contexts/ServiceStepsContext';

type Props = {
  initialValues?: {
    searchBaseDN?: string,
    searchPattern?: string,
    displayNameAttribute?: string,
  },
  help?: {
    searchBaseDN?: React.Node,
    searchPattern?: React.Node,
    displayNameAttribute?: React.Node,
  },
  onSubmit: (nextStepKey: string) => void,
  onSubmitAll: () => void,
};

const defaultHelp = {
  searchBaseDN: (
    <span>
      The base tree to limit the Active Directory search query to, e.g. <code>cn=users,dc=example,dc=com</code>.
    </span>
  ),
  searchPattern: (
    <span>
      For example <code className="text-nowrap">{'(&(objectClass=user)(sAMAccountName={0}))'}</code>.{' '}
      The string <code>{'{0}'}</code> will be replaced by the entered username.
    </span>
  ),
  displayNameAttribute: (
    <span>
      Which Active Directory attribute to use for the full name of the user in Graylog, e.g. <code>displayName</code>.<br />
      Try to load a test user using the form below, if you are unsure which attribute to use.
    </span>
  ),
};

const StepUserMapping = ({ initialValues, help: propsHelp, onSubmit, onSubmitAll }: Props) => {
  const { setStepsState, ...stepsState } = useContext(ServiceStepsContext);
  const formRef = useRef();
  const help = { ...defaultHelp, ...propsHelp };

  useEffect(() => {
    if (setStepsState) {
      setStepsState({ ...stepsState, forms: { ...stepsState.forms, serverConfig: formRef } });
    }
  }, []);

  return (
    <Formik initialValues={initialValues} onSubmit={() => onSubmit('group-mapping')} innerRef={formRef}>
      {({ isSubmitting, isValid }) => (
        <Form>
          <FormikFormGroup label="Search Base DN"
                           name="searchBaseDN"
                           placeholder="System User DN"
                           required
                           help={help.searchBaseDN} />

          <FormikFormGroup label="Search Pattern"
                           name="searchPattern"
                           placeholder="Search Pattern"
                           required
                           help={help.searchPattern} />

          <FormikFormGroup label="Display Name Attirbute"
                           name="displayNameAttibute"
                           placeholder="Display Name Attirbute"
                           required
                           help={help.displayNameAttribute} />

          <Button bsStyle="secondary"
                  type="button"
                  onClick={() => onSubmitAll()}
                  disabled={!isValid || isSubmitting}>
            Finish & Save Identity Provider
          </Button>
          <Button bsStyle="primary"
                  type="submit"
                  disabled={!isValid || isSubmitting}>
            Setup Group Mapping
          </Button>
        </Form>
      )}
    </Formik>
  );
};

StepUserMapping.defaultProps = {
  help: {},
  initialValues: {},
};

export default StepUserMapping;
