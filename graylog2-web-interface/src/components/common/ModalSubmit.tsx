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
import { useEffect, useRef } from 'react';
import type { SyntheticEvent } from 'react';
import styled, { css } from 'styled-components';

import { Button } from 'components/bootstrap';
import type { IconName } from 'components/common/Icon';
import Icon from 'components/common/Icon';
import Spinner from 'components/common/Spinner';
import ModalButtonToolbar from 'components/common/ModalButtonToolbar';

const Container = styled.div<{ $hasLeftCol: boolean }>(
  ({ $hasLeftCol }) =>
    $hasLeftCol &&
    css`
      display: flex;
      justify-content: space-between;
      align-items: end;
    `,
);

const buttonTitle = (isSubmitting: boolean, submitText: React.ReactNode, submitLoadingText: string) => {
  if (isSubmitting && typeof submitLoadingText === 'string') {
    return submitLoadingText;
  }

  if (!isSubmitting && typeof submitText === 'string') {
    return submitText;
  }

  return undefined;
};

type WithCancelProps = {
  displayCancel?: true;
  disabledCancel?: boolean;
  onCancel: () => void;
};

type WithoutCancelProps = {
  displayCancel?: false;
};

type WithAsyncSubmit = {
  isAsyncSubmit?: true;
  submitLoadingText: string;
  isSubmitting: boolean;
};

type WithSyncSubmit = {
  isAsyncSubmit?: false;
};

type Props = {
  autoFocus?: boolean;
  bsSize?: 'large' | 'small' | 'xsmall';
  className?: string;
  disabledSubmit?: boolean;
  displayCancel?: boolean;
  formId?: string;
  isAsyncSubmit?: boolean;
  isSubmitting?: boolean;
  leftCol?: React.ReactNode;
  onSubmit?: (event?: SyntheticEvent) => void;
  submitButtonText: React.ReactNode;
  submitButtonType?: 'submit' | 'button';
  submitIcon?: IconName;
} & (WithCancelProps | WithoutCancelProps) &
  (WithAsyncSubmit | WithSyncSubmit);

// @ts-expect-error
const isWithCancelProps = (props: Props): props is WithCancelProps =>
  props.displayCancel === undefined || props.displayCancel === true;
// @ts-expect-error
const isWithAsyncSubmit = (props: Props): props is WithAsyncSubmit => props.isAsyncSubmit === true;

const ModalSubmit = ({ ...props }: Props) => {
  const {
    autoFocus = false,
    bsSize,
    className,
    disabledSubmit = false,
    formId,
    leftCol,
    onSubmit,
    submitButtonText,
    submitButtonType = 'submit',
    submitIcon,
  } = props;

  const submittingAsync = isWithAsyncSubmit(props) && props.isSubmitting;
  const title = buttonTitle(
    submittingAsync,
    submitButtonText,
    'submitLoadingText' in props ? props.submitLoadingText : undefined,
  );
  const confirmRef = useRef<HTMLButtonElement>();
  useEffect(() => {
    if (autoFocus && !disabledSubmit && confirmRef.current) {
      confirmRef.current.focus();
    }
    // Should only run once during mount to avoid refocussing
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <Container $hasLeftCol={!!leftCol} className={className}>
      {leftCol}
      <ModalButtonToolbar>
        {isWithCancelProps(props) && (
          <Button
            type="button"
            bsSize={bsSize}
            onClick={props.onCancel}
            title="Cancel"
            aria-label="Cancel"
            disabled={props.disabledCancel || submittingAsync}>
            Cancel
          </Button>
        )}
        <Button
          ref={confirmRef}
          bsStyle="primary"
          bsSize={bsSize}
          disabled={disabledSubmit || submittingAsync}
          form={formId}
          title={title}
          aria-label={title}
          type={submitButtonType}
          onClick={onSubmit}>
          {submitIcon && !submittingAsync && (
            <>
              <Icon name={submitIcon} />{' '}
            </>
          )}
          {submittingAsync ? <Spinner text={props.submitLoadingText} delay={0} /> : submitButtonText}
        </Button>
      </ModalButtonToolbar>
    </Container>
  );
};

export default ModalSubmit;
