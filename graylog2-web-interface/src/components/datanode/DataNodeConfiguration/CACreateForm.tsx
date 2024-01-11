import React from 'react';
import { Formik, Form } from 'formik';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'preflight/util/UserNotification';
import { QUERY_KEY as DATA_NODES_CA_QUERY_KEY } from 'preflight/hooks/useDataNodesCA';
import { FormikInput } from 'components/common';
import { Button } from 'components/bootstrap';

type FormValues = {}

const createCA = (caData: FormValues) => fetch(
  'POST',
  qualifyUrl('ca/create'),
  caData,
  false,
);

const CaCreateForm = () => {
  const queryClient = useQueryClient();

  const { mutateAsync: onCreateCA } = useMutation(createCA, {
    onSuccess: () => {
      UserNotification.success('CA created successfully');
      queryClient.invalidateQueries(DATA_NODES_CA_QUERY_KEY);
    },
    onError: (error) => {
      UserNotification.error(`CA creation failed with error: ${error}`);
    },
  });
  const onSubmit = (formValues: FormValues) => onCreateCA(formValues).catch(() => {});

  return (
    <div>
      <p>
        Here you can quickly create a new certificate authority.
        All you need to do is to click on the &ldquo;Create CA&rdquo; button.
        The CA should only be used to secure your Graylog data nodes.
      </p>
      <Formik initialValues={{ organization: 'Graylog CA' }} onSubmit={(formValues: FormValues) => onSubmit(formValues)}>
        {({ isSubmitting, isValid }) => (
          <Form>
            <FormikInput id="organization"
                         placeholder="Organization Name"
                         name="organization"
                         label="Organization Name"
                         required />
            <Button disabled={isSubmitting || !isValid} type="submit">
              {isSubmitting ? 'Creating CA...' : 'Create CA'}
            </Button>
          </Form>
        )}
      </Formik>
    </div>
  );
};

export default CaCreateForm;
