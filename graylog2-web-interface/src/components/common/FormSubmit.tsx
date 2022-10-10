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
  displayCancel: true,
  disabledCancel?: boolean,
  onCancel: () => void,
}

type WithoutCancelProps = {
  displayCancel: false
}

type WithAsyncSubmit = {
  isAsyncSubmit: true,
  submitLoadingText: string,
  isSubmitting: boolean,
}

type WithSyncSubmit = {
  isAsyncSubmit: false,
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

const FormSubmit = (props: Props) => {
  const {
    bsSize,
    className,
    centerCol,
    displayCancel,
    disabledSubmit,
    formId,
    isAsyncSubmit,
    onSubmit,
    submitButtonText,
    submitButtonType,
    submitIcon,
  } = props;

  return (
    <ButtonToolbar className={className}>
      <Button bsStyle="success"
              bsSize={bsSize}
              disabled={disabledSubmit || (isAsyncSubmit && props.isSubmitting)}
              form={formId}
              title={submitButtonText}
              type={submitButtonType}
              onClick={onSubmit}>
        {(submitIcon && !(isAsyncSubmit && props.isSubmitting)) && <StyledIcon name={submitIcon} />}
        {(isAsyncSubmit && props.isSubmitting) ? <Spinner text={props.submitLoadingText} delay={0} /> : submitButtonText}
      </Button>
      {centerCol}
      {displayCancel === true && (
        <Button type="button"
                bsSize={bsSize}
                onClick={props.onCancel}
                disabled={props.disabledCancel || (isAsyncSubmit && props.isSubmitting)}>
          Cancel
        </Button>
      )}
    </ButtonToolbar>
  );
};

FormSubmit.defaultProps = {
  bsSize: undefined,
  centerCol: undefined,
  className: undefined,
  disabledSubmit: false,
  displayCancel: true,
  formId: undefined,
  isAsyncSubmit: false,
  onSubmit: undefined,
  submitButtonType: 'submit',
  submitIcon: undefined,
};

export default FormSubmit;
