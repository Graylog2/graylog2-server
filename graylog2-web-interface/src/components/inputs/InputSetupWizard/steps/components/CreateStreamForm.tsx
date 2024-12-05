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
import React, { useState } from 'react';
import { Formik, Form } from 'formik';
import styled, { css } from 'styled-components';

import Routes from 'routing/Routes';
import { Button, Col, Row } from 'components/bootstrap';
import { FormikInput, InputOptionalInfo, Spinner } from 'components/common';
import IndexSetSelect from 'components/streams/IndexSetSelect';
import useIndexSetsList from 'components/indices/hooks/useIndexSetsList';
import type { StreamConfiguration } from 'components/inputs/InputSetupWizard/hooks/useSetupInputMutations';

export type StreamFormValues = StreamConfiguration

export type FormValues = {
    create_new_pipeline?: boolean
} & StreamConfiguration

type Props = {
    submitForm: (values: FormValues) => void
}

const NewIndexSetButton = styled(Button)(({ theme }) => css`
  margin-bottom: ${theme.spacings.xs};
`);

const SubmitCol = styled(Col)`
  display: flex;
  justify-content: flex-end;
`;

const CreateStreamForm = ({ submitForm } : Props) => {
  const [indexSetsRefetchInterval, setIndexSetsRefetchInterval] = useState<false | number>(false);
  const { data: indexSetsData, isSuccess: isIndexSetsSuccess } = useIndexSetsList(false, indexSetsRefetchInterval);

  const validate = (values: FormValues) => {
    let errors = {};

    if (!values.title) {
      errors = { ...errors, title: 'Title is required' };
    }

    if (!values.index_set_id) {
      errors = { ...errors, index_set_id: 'Index set is required' };
    }

    return errors;
  };

  const handleNewIndexSetClick = () => {
    window.open(Routes.SYSTEM.INDEX_SETS.CREATE, '_blank');
    setIndexSetsRefetchInterval(10000);
  };

  if (!isIndexSetsSuccess || !indexSetsData) {
    return <Spinner />;
  }

  const { indexSets } = indexSetsData;

  const initialValues = {
    description: undefined,
    title: undefined,
    index_set_id: indexSets?.find((indexSet) => indexSet.default)?.id,
    remove_matches_from_default_stream: undefined,
    create_new_pipeline: undefined,
  };

  return (
    <Formik<FormValues> initialValues={initialValues}
                        onSubmit={submitForm}
                        validate={validate}>
      {({ isValid, isValidating, dirty }) => (

        <Form>
          <FormikInput label="Title"
                       name="title"
                       id="title"
                       help="A descriptive name of the new stream" />
          <FormikInput label={<>Description <InputOptionalInfo /></>}
                       name="description"
                       id="description"
                       help="What kind of messages are routed into this stream?" />

          <IndexSetSelect indexSets={indexSets} />
          <NewIndexSetButton bsSize="xs" onClick={handleNewIndexSetClick}>Create a new Index Set</NewIndexSetButton>
          <FormikInput label={<>Remove matches from &lsquo;Default Stream&rsquo;</>}
                       help={
                         <span>Don&apos;t assign messages that match this stream to the &lsquo;Default Stream&rsquo;.</span>
}
                       name="remove_matches_from_default_stream"
                       id="remove_matches_from_default_stream"
                       type="checkbox" />

          <FormikInput label={<>Create a new pipeline for this stream</>}
                       name="create_new_pipeline"
                       id="create_new_pipeline"
                       type="checkbox" />

          <Row>
            <SubmitCol md={12}>
              <Button bsStyle="primary" type="submit" disabled={isValidating || !isValid || !dirty}>Create</Button>
            </SubmitCol>
          </Row>
        </Form>
      )}
    </Formik>
  );
};

export default CreateStreamForm;
