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
import styled from 'styled-components';

import ButtonToolbar from 'components/bootstrap/ButtonToolbar';
import Button from 'components/bootstrap/Button';
import type { IconName } from 'components/common/Icon';
import Icon from 'components/common/Icon';
import Spinner from 'components/common/Spinner';

const StyledIcon = styled(Icon)`
  margin-right: 0.2em;
`;

type WithCancelProps = {
  displayCancel?: true,
  disabledCancel?: boolean,
  onCancel: () => void,
}

type WithoutCancelProps = {
  displayCancel?: false
}

type WithAsyncSubmit = {
  isAsyncSubmit?: true,
  submitLoadingText: string,
  isSubmitting: boolean,
}

type WithSyncSubmit = {
  isAsyncSubmit?: false,
}

type Props = {
  bsSize?: 'large' | 'small' | 'xsmall',
  centerCol?: React.ReactNode,
  className?: string,
  disabledSubmit?: boolean,
  displayCancel?: boolean,
  formId?: string,
  isAsyncSubmit?: boolean,
  onSubmit?: () => void,
  submitButtonText: string,
  submitButtonType?: 'submit' | 'button',
  submitIcon?: IconName,
} & (WithCancelProps | WithoutCancelProps) & (WithAsyncSubmit | WithSyncSubmit);

// @ts-expect-error
const isWithCancelProps = (props: Props): props is WithCancelProps => props.displayCancel === undefined || props.displayCancel === true;
// @ts-expect-error
const isWithAsyncSubmit = (props: Props): props is WithAsyncSubmit => props.isAsyncSubmit === true;

const FormSubmit = (props: Props) => {
  const {
    bsSize,
    className,
    centerCol,
    disabledSubmit = false,
    formId,
    onSubmit,
    submitButtonText,
    submitButtonType = 'submit',
    submitIcon,
  } = props;

  const submittingAsync = isWithAsyncSubmit(props) && props.isSubmitting;

  return (
    <ButtonToolbar className={className}>
      <Button bsStyle="success"
              bsSize={bsSize}
              disabled={disabledSubmit || submittingAsync}
              form={formId}
              title={submitButtonText}
              aria-label={submitButtonText}
              type={submitButtonType}
              onClick={onSubmit}>
        {(submitIcon && !submittingAsync) && <StyledIcon name={submitIcon} />}
        {submittingAsync ? <Spinner text={props.submitLoadingText} delay={0} /> : submitButtonText}
      </Button>
      {centerCol}
      {isWithCancelProps(props) && (
        <Button type="button"
                bsSize={bsSize}
                onClick={props.onCancel}
                title="Cancel"
                aria-label="Cancel"
                disabled={props.disabledCancel || submittingAsync}>
          Cancel
        </Button>
      )}
    </ButtonToolbar>
  );
};

export default FormSubmit;
