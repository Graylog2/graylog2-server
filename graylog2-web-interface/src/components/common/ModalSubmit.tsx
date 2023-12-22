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
import type { SyntheticEvent } from 'react';

import Button from 'components/bootstrap/Button';
import ButtonToolbar from 'components/bootstrap/ButtonToolbar';
import type { IconName } from 'components/common/Icon';
import Icon from 'components/common/Icon';
import Spinner from 'components/common/Spinner';

const StyledButtonToolbar = styled(ButtonToolbar)`
  display: flex;
  justify-content: flex-end;
  align-items: end;
  gap: 0.25em;
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
  className?: string,
  disabledSubmit?: boolean,
  displayCancel?: boolean,
  formId?: string,
  isAsyncSubmit?: boolean,
  isSubmitting?: boolean,
  leftCol?: React.ReactNode,
  onSubmit?: (event?: SyntheticEvent) => void,
  submitButtonText: React.ReactNode,
  submitButtonType?: 'submit' | 'button',
  submitIcon?: IconName,
} & (WithCancelProps | WithoutCancelProps) & (WithAsyncSubmit | WithSyncSubmit);

const ModalSubmit = (props: Props) => {
  const {
    isAsyncSubmit,
    bsSize,
    className,
    displayCancel,
    disabledSubmit,
    formId,
    leftCol,
    onSubmit,
    submitButtonText,
    submitButtonType,
    submitIcon,
  } = props;

  const title = typeof submitButtonText === 'string' ? submitButtonText : undefined;

  return (
    <StyledButtonToolbar className={className}>
      {leftCol}
      {displayCancel && (
        <Button type="button"
                bsSize={bsSize}
                onClick={props.onCancel}
                title="Cancel"
                aria-label="Cancel"
                disabled={props.disabledCancel || (isAsyncSubmit && props.isSubmitting)}>
          Cancel
        </Button>
      )}
      <Button bsStyle="success"
              bsSize={bsSize}
              disabled={disabledSubmit || (isAsyncSubmit && props.isSubmitting)}
              form={formId}
              title={title}
              aria-label={title}
              type={submitButtonType}
              onClick={onSubmit}>
        {(submitIcon && !(isAsyncSubmit && props.isSubmitting)) && <><Icon name={submitIcon} /> </>}
        {(isAsyncSubmit && props.isSubmitting) ? <Spinner text={props.submitLoadingText} delay={0} /> : submitButtonText}
      </Button>
    </StyledButtonToolbar>
  );
};

ModalSubmit.defaultProps = {
  bsSize: undefined,
  className: undefined,
  disabledSubmit: false,
  displayCancel: true,
  isAsyncSubmit: false,
  formId: undefined,
  isSubmitting: false,
  leftCol: undefined,
  onSubmit: undefined,
  submitButtonType: 'submit',
  submitIcon: undefined,
};

export default ModalSubmit;
