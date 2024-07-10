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
import type { SyntheticEvent } from 'react';

import Button from 'components/bootstrap/Button';
import type { IconName } from 'components/common/Icon';
import Icon from 'components/common/Icon';
import Spinner from 'components/common/Spinner';
import ModalButtonToolbar from 'components/common/ModalButtonToolbar';
import { SplitButton, Menu, MenuItem } from 'components/bootstrap';

type WithCancelProps = {
  onCancel: () => void,
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
  isAsyncSubmit?: boolean,
  isSubmitting?: boolean,
  leftCol?: React.ReactNode,
  onSubmit?: (event?: SyntheticEvent) => void,
  submitButtonText: React.ReactNode,
  submitIcon?: IconName,
} & WithCancelProps & (WithAsyncSubmit | WithSyncSubmit);

const ModalSubmitWitSplitButton = (props: Props) => {
  const {
    isAsyncSubmit,
    bsSize,
    className,
    disabledSubmit,
    leftCol,
    onSubmit,
    submitButtonText,
    submitIcon,
  } = props;

  const title = typeof submitButtonText === 'string' ? submitButtonText : undefined;

  return (
    <ModalButtonToolbar className={className}>
      {leftCol}
      <Button type="button"
              bsSize={bsSize}
              onClick={props.onCancel}
              title="Cancel"
              aria-label="Cancel"
              disabled={isAsyncSubmit && props.isSubmitting}>
        Cancel
      </Button>
      <SplitButton id="bundle-dropdown"
                   onClick={onSubmit}
                   bsStyle="success"
                   bsSize={bsSize}
                   disabled={disabledSubmit || (isAsyncSubmit && props.isSubmitting)}
                   title={title}
                   aria-label={title}>
        {(submitIcon && !(isAsyncSubmit && props.isSubmitting)) && <><Icon name={submitIcon} /> </>}
        {(isAsyncSubmit && props.isSubmitting) ? <Spinner text={props.submitLoadingText} delay={0} /> : submitButtonText}
        <Menu>
          <MenuItem onClick={onSubmit}>{title} and generate</MenuItem>
        </Menu>
      </SplitButton>
    </ModalButtonToolbar>
  );
};

ModalSubmitWitSplitButton.defaultProps = {
  bsSize: undefined,
  className: undefined,
  disabledSubmit: false,
  isAsyncSubmit: false,
  isSubmitting: false,
  leftCol: undefined,
  onSubmit: undefined,
  submitIcon: undefined,
};

export default ModalSubmitWitSplitButton;
