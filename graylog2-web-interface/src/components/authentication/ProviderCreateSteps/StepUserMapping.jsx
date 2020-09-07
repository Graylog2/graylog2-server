// @flow strict
import * as React from 'react';
import { Formik, Form } from 'formik';
import { useContext } from 'react';

import { FormikFormGroup } from 'components/common';
import { Button, ButtonToolbar } from 'components/graylog';

import ServiceStepsContext from '../contexts/ServiceStepsContext';

type Props = {
  help?: {
    searchBaseDN?: React.Node,
    searchPattern?: React.Node,
    displayNameAttribute?: React.Node,
  },
  onChange: (event: Event, values: any) => void,
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

const StepUserMapping = ({ help: propsHelp, onSubmit, onSubmitAll, onChange }: Props) => {
  const help = { ...defaultHelp, ...propsHelp };
  const { setStepsState, ...stepsState } = useContext(ServiceStepsContext);

  return (
    <Formik initialValues={stepsState?.formValues?.['user-mapping']} onSubmit={() => onSubmit('group-mapping')}>
      {({ isSubmitting, isValid, values }) => (
        <Form onChange={(event) => onChange(event, values)} className="form form-horizontal">
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

          <ButtonToolbar className="pull-right">
            <Button type="button"
                    onClick={() => onSubmitAll()}
                    disabled={!isValid || isSubmitting}>
              Finish & Save Identity Provider
            </Button>
            <Button bsStyle="primary"
                    type="submit"
                    disabled={!isValid || isSubmitting}>
              Setup Group Mapping
            </Button>
          </ButtonToolbar>
        </Form>
      )}
    </Formik>
  );
};

StepUserMapping.defaultProps = {
  help: {},
};

export default StepUserMapping;
