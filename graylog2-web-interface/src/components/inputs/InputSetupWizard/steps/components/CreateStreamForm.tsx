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
import { useTranslation } from 'react-i18next';

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
  prevCreatedStream?: OpenStepsData['SETUP_ROUTING']['newStream'];
  prevShouldCreateNewPipeline?: OpenStepsData['SETUP_ROUTING']['shouldCreateNewPipeline'];
};

const SubHeadline = styled.h4(
  ({ theme }) => css`
    margin-top: ${theme.spacings.md};
    margin-bottom: ${theme.spacings.xs};
  `,
);

const IndexSetInfoText = styled.p(
  ({ theme }) => css`
    margin-top: ${theme.spacings.xs};
    margin-bottom: ${theme.spacings.md};
  `,
);

const NewIndexSetButton = styled(Button)(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.md};
  `,
);

const CreateStreamForm = ({
  submitForm,
  handleBackClick,
  prevCreatedStream = undefined,
  prevShouldCreateNewPipeline = true,
}: Props) => {
  const { t } = useTranslation('inputs');
  const [indexSetsRefetchInterval, setIndexSetsRefetchInterval] = useState<false | number>(false);
  const { data: indexSetsData, isSuccess: isIndexSetsSuccess } = useIndexSetsList(
    false,
    indexSetsRefetchInterval,
    true,
  );

  const validate = (values: FormValues) => {
    let errors = {};

    if (!values.title) {
      errors = { ...errors, title: t('setup_wizard.create_stream.title_required') };
    }

    if (!values.index_set_id) {
      errors = { ...errors, index_set_id: t('setup_wizard.create_stream.index_set_required') };
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

  const initialFormValues = {
    description: prevCreatedStream?.description ?? undefined,
    title: prevCreatedStream?.title ?? undefined,
    index_set_id: prevCreatedStream?.index_set_id ?? indexSets?.find((indexSet) => indexSet.default)?.id,
    remove_matches_from_default_stream: prevCreatedStream?.remove_matches_from_default_stream ?? true,
    create_new_pipeline: prevShouldCreateNewPipeline,
  };

  return (
    <Formik<FormValues> initialValues={initialFormValues} onSubmit={submitForm} validate={validate}>
      {({ isValid, isValidating, dirty, values }) => (
        <Form>
          <FormikInput
            label={t('setup_wizard.create_stream.title')}
            name="title"
            id="title"
            help={t('setup_wizard.create_stream.title_help')}
          />
          <FormikInput
            label={
              <>
                {t('setup_wizard.create_stream.description')} <InputOptionalInfo />
              </>
            }
            name="description"
            id="description"
            help={t('setup_wizard.create_stream.description_help')}
          />
          <FormikInput
            label={<>{t('setup_wizard.routing.remove_from_default')}</>}
            help={<span>{t('setup_wizard.routing.remove_from_default_help')}</span>}
            name="remove_matches_from_default_stream"
            id="remove_matches_from_default_stream"
            type="checkbox"
          />

          <FormikInput
            label={<>{t('setup_wizard.create_stream.create_pipeline')}</>}
            name="create_new_pipeline"
            id="create_new_pipeline"
            type="checkbox"
          />
          <SubHeadline>{t('setup_wizard.create_stream.select_index_set')}</SubHeadline>
          <SelectedIndexSetAlert indexSets={indexSets} selectedIndexSetId={values.index_set_id} />
          <IndexSetInfoText>
            {t('setup_wizard.create_stream.index_set_info')}
            <br />
            {t('setup_wizard.create_stream.index_set_recommendation')}
          </IndexSetInfoText>
          <RecommendedTooltip opened withArrow position="right" label={t('setup_wizard.routing.recommended')}>
            <NewIndexSetButton onClick={handleNewIndexSetClick}>
              {t('setup_wizard.create_stream.create_index_set')}
            </NewIndexSetButton>
          </RecommendedTooltip>
          <IndexSetSelect label={t('setup_wizard.create_stream.select_index_set')} indexSets={indexSets} />

          <Row>
            <ButtonCol md={12}>
              <Button onClick={handleBackClick}>{t('setup_wizard.back')}</Button>
              <Button
                bsStyle="primary"
                type="submit"
                disabled={isValidating || !isValid || (!dirty && !prevCreatedStream)}>
                {t('common:actions.next')}
              </Button>
            </ButtonCol>
          </Row>
        </Form>
      )}
    </Formik>
  );
};

export default CreateStreamForm;
