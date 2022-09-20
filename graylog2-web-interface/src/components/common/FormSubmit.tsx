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

import { Button, ButtonToolbar } from 'components/bootstrap';
import type { IconName } from 'components/common/Icon';
import Icon from 'components/common/Icon';
import Spinner from 'components/common/Spinner';

type WithCancelProps = {
  displayCancel: true,
  disableCancel?: boolean,
  onCancel: () => void,
}

type WithoutCancelProps = {
  displayCancel: false
}

type Props = {
  className?: string,
  displayCancel?: boolean,
  disabledSubmit?: boolean,
  isSubmitting?: boolean,
  leftCol?: React.ReactNode,
  onSubmit?: () => void,
  submitButtonText: string,
  submitIcon?: IconName,
  submitButtonType?: 'submit' | 'button',
  submitLoadingText?: string,
} & (WithCancelProps | WithoutCancelProps)

const FormSubmit = (props: Props) => {
  const {
    className,
    displayCancel,
    disabledSubmit,
    isSubmitting,
    leftCol,
    onSubmit,
    submitButtonText,
    submitButtonType,
    submitIcon,
    submitLoadingText,
  } = props;

  return (
    <ButtonToolbar className={`${className} pull-right`}>
      {leftCol}
      {displayCancel === true && <Button type="button" onClick={props.onCancel} disabled={props.disableCancel}>Cancel</Button>}
      <Button bsStyle="success"
              disabled={disabledSubmit}
              title={submitButtonText}
              type={submitButtonType}
              onClick={onSubmit}>
        {(submitIcon && !isSubmitting) && <><Icon name={submitIcon} /> </>}
        {isSubmitting ? <Spinner text={submitLoadingText} delay={0} /> : submitButtonText}
      </Button>
    </ButtonToolbar>
  );
};

FormSubmit.defaultProps = {
  className: undefined,
  disabledSubmit: false,
  displayCancel: true,
  isSubmitting: false,
  leftCol: undefined,
  onSubmit: undefined,
  submitButtonType: 'submit',
  submitIcon: undefined,
  submitLoadingText: undefined,
};

export default FormSubmit;
