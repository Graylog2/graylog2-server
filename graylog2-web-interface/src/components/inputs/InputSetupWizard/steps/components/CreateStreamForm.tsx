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
import { Button, Row } from 'components/bootstrap';
import { FormikInput, InputOptionalInfo, Spinner } from 'components/common';
import IndexSetSelect from 'components/streams/IndexSetSelect';
import useIndexSetsList from 'components/indices/hooks/useIndexSetsList';
import SelectedIndexSetAlert from 'components/inputs/InputSetupWizard/steps/components/SelectedIndexSetAlert';
import type { OpenStepsData } from 'components/inputs/InputSetupWizard/types';

import { ButtonCol, RecommendedTooltip } from './StepWrapper';

type FormValues = {
  create_new_pipeline?: boolean;
} & OpenStepsData['SETUP_ROUTING']['newStream'];

type Props = {
  submitForm: (values: FormValues) => void;
  handleBackClick: () => void;
};

const NewIndexSetButton = styled(Button)(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.xs};
  `,
);

const CreateStreamForm = ({ submitForm, handleBackClick }: Props) => {
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
    remove_matches_from_default_stream: true,
    create_new_pipeline: true,
  };

  return (
    <Formik<FormValues> initialValues={initialValues} onSubmit={submitForm} validate={validate}>
      {({ isValid, isValidating, dirty, values }) => (
        <Form>
          <FormikInput label="Title" name="title" id="title" help="A descriptive name of the new stream" />
          <FormikInput
            label={
              <>
                Description <InputOptionalInfo />
              </>
            }
            name="description"
            id="description"
            help="What kind of messages are routed into this stream?"
          />
          <SelectedIndexSetAlert indexSets={indexSets} selectedIndexSetId={values.index_set_id} />

          <IndexSetSelect
            indexSets={indexSets}
            help={
              <>
                Messages that match this stream will be written to the configured Index Set. Index Sets are used to
                rationally partition data to allow faster searches.
                <br />
                We recommend creating a new Index Set for each Input type.
              </>
            }
          />
          <RecommendedTooltip opened withArrow position="right" label="Recommended!">
            <NewIndexSetButton onClick={handleNewIndexSetClick}>Create a new Index Set</NewIndexSetButton>
          </RecommendedTooltip>
          <FormikInput
            label={<>Remove matches from &lsquo;Default Stream&rsquo;</>}
            help={<span>Don&apos;t assign messages that match this stream to the &lsquo;Default Stream&rsquo;.</span>}
            name="remove_matches_from_default_stream"
            id="remove_matches_from_default_stream"
            type="checkbox"
          />

          <FormikInput
            label={<>Create a new pipeline for this stream</>}
            name="create_new_pipeline"
            id="create_new_pipeline"
            type="checkbox"
          />

          <Row>
            <ButtonCol md={12}>
              <Button onClick={handleBackClick}>Back</Button>
              <Button bsStyle="primary" type="submit" disabled={isValidating || !isValid || !dirty}>
                Next
              </Button>
            </ButtonCol>
          </Row>
        </Form>
      )}
    </Formik>
  );
};

export default CreateStreamForm;
