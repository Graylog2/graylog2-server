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

import FormSubmit from 'components/common/FormSubmit';

type Props = React.ComponentProps<typeof FormSubmit>

/* eslint-disable react/prop-types */
const ModalSubmit = ({
  className,
  disabledSubmit,
  disableCancel,
  isSubmitting,
  leftCol,
  onCancel,
  onSubmit,
  submitLoadingText,
  submitIcon,
  submitButtonText,
}: Props) => (
  <FormSubmit disableCancel={disableCancel}
              disabledSubmit={disabledSubmit}
              isSubmitting={isSubmitting}
              leftCol={leftCol}
              onCancel={onCancel}
              onSubmit={onSubmit}
              submitButtonText={submitButtonText}
              submitIcon={submitIcon}
              submitLoadingText={submitLoadingText}
              className={className} />
);

export default ModalSubmit;
