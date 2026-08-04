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

import { Label } from 'components/bootstrap';

import { INSTANCE_STATUS_LABELS } from './Constants';

import type { CollectorInstanceView } from '../types';

type Props = {
  status: CollectorInstanceView['status'];
};

/**
 * The collector's online/offline state. Shared between the instances table column, the instance
 * detail drawer and the onboarding summary so all three render identically.
 */
const InstanceStatusLabel = ({ status }: Props) => {
  // A status the frontend does not know about reads as offline, matching the previous behaviour of
  // the `status === 'online'` checks this replaced.
  const { label, style } = INSTANCE_STATUS_LABELS[status] ?? INSTANCE_STATUS_LABELS.offline;

  return <Label bsStyle={style}>{label}</Label>;
};

export default InstanceStatusLabel;
