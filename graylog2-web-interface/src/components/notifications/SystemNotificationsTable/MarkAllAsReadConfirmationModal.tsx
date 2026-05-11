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

import { BootstrapModalConfirm } from 'components/bootstrap';

type Props = {
  show: boolean;
  onClose: () => void;
  onConfirm: () => void;
};

const MarkAllAsReadConfirmationModal = ({ show, onClose, onConfirm }: Props) => (
  <BootstrapModalConfirm
    showModal={show}
    title="Mark all notifications as read?"
    confirmButtonText="Mark all as read"
    onConfirm={onConfirm}
    onCancel={onClose}>
    All notifications you have permission to view will be marked as read, including ones not
    currently visible on this page.
  </BootstrapModalConfirm>
);

export default MarkAllAsReadConfirmationModal;
