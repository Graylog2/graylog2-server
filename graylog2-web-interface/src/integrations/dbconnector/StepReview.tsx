import React, { useContext, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css, DefaultTheme } from 'styled-components';

import useFetch from '../common/hooks/useFetch';
import FormWrap from '../common/FormWrap';
import { ApiRoutes } from './Routes';
import { FormDataContext } from '../common/context/FormData';
import { Icon } from 'components/common';
import { toDBConnectorInputCreateRequest } from './formDataAdapter';

const Default = ({ value }) => (
  <>{value} <small>(default)</small></>
);

Default.propTypes = {
  value: PropTypes.string.isRequired,
};

const Container = styled.div(({ theme }: { theme: DefaultTheme }) => css`
  border: 1px solid ${theme.colors.variant.darkest.default};
  margin: 25px 0;
  padding: 15px;
  border-radius: 4px;
`);

const Subheader = styled.h3`
  margin: 0 0 10px;
`;

const ReviewItems = styled.ul(({ theme }) => css`
  list-style: none;
  margin: 0 0 25px 10px;
  padding: 0;
  li {
    line-height: 2;
    padding: 0 5px;
    :nth-of-type(odd) {
      background-color: ${theme.colors.table.row.backgroundStriped};
    }
  }
  strong {
    ::after {
      content: ':';
      margin-right: 5px;
    }
  }
`);

type Props = {
  onSubmit: (FormData?) => void,
  externalInputSubmit: boolean,
};

const StepReview = ({ onSubmit, externalInputSubmit }: Props) => {
  const [formError, setFormError] = useState(null);
  const { formData } = useContext(FormDataContext);

  const throttleEnabled = !!formData.enableThrottling?.value;

  const [saveInput, setSaveInput] = useFetch(
    null,
    () => onSubmit(),
    'POST',
    toDBConnectorInputCreateRequest(formData),
  );

  useEffect(() => {
    setSaveInput(null);

    if (saveInput.error) {
      setFormError({
        full_message: saveInput.error,
        nice_message: <span>We were unable to save your Input, please try again in a few moments.</span>,
      });
    }
  }, [saveInput.error, setSaveInput]);

  const handleSubmit = () => {
    if (externalInputSubmit) {
      onSubmit(formData);

      return;
    }

    setSaveInput(ApiRoutes.INTEGRATIONS.DBConnector.SAVE_INPUT);
  };

  return (
    <FormWrap onSubmit={handleSubmit}
      buttonContent="Save and Start Input"
      loading={saveInput.loading}
      error={formError}
      description="Check out everything below to make sure it&apos;s correct, then click the button below to complete your Database Connector setup!">

      <Container>
        <Subheader>Input Configuration</Subheader>
        <ReviewItems>
          <li>
            <strong>Name: </strong>
            <span>{formData.dbConnectorName.value}</span>
          </li>
          <li>
            <strong>Database Type: </strong>
            <span>{formData.dbType.value}</span>
          </li>
          <li>
            <strong>State Field: </strong>
            <span>{formData.stateField.value}</span>
          </li>
          <li>
            <strong>State Field Type: </strong>
            <span>{formData.stateFieldType.value}</span>
          </li>
          <li>
            <strong>Override Source: </strong>
            <span>{formData.overrideSource.value}</span>
          </li>
          <li>
            <strong>Enable Throttling: </strong>
            <span><Icon name={throttleEnabled ? 'check_circle' : 'cancel'} /></span>
          </li>
        </ReviewItems>
      </Container>
    </FormWrap>
  );
};

StepReview.propTypes = {
  onSubmit: PropTypes.func.isRequired,
  externalInputSubmit: PropTypes.bool,
};

StepReview.defaultProps = {
  externalInputSubmit: false,
};

export default StepReview;