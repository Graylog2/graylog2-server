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
import Spinner from 'components/common/Spinner';

type Props = {
  className?: string,
  disableCancel?: boolean,
  disableSubmit?: boolean,
  isSubmitting?: boolean,
  onCancel: () => void,
  onSubmit?: () => void,
  submitButtonText: string,
  submitButtonType?: 'submit' | 'button',
  submitLoadingText?: string,
}

const FormSubmit = ({
  className,
  disableCancel,
  disableSubmit,
  isSubmitting,
  onCancel,
  onSubmit,
  submitButtonText,
  submitButtonType,
  submitLoadingText,
}: Props) => (
  <ButtonToolbar className={`${className} pull-right`}>
    <Button type="button" onClick={onCancel} disabled={disableCancel}>Cancel</Button>
    <Button bsStyle="success"
            disabled={disableSubmit}
            title={submitButtonText}
            type={submitButtonType}
            onClick={onSubmit}>
      {isSubmitting ? <Spinner text={submitLoadingText} delay={0} /> : submitButtonText}
    </Button>
  </ButtonToolbar>
);

FormSubmit.defaultProps = {
  className: undefined,
  disableCancel: false,
  disableSubmit: false,
  isSubmitting: false,
  onSubmit: undefined,
  submitButtonType: 'submit',
  submitLoadingText: undefined,
};

export default FormSubmit;
