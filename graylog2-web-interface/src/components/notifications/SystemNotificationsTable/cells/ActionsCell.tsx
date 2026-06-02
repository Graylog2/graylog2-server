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

import { Button } from 'components/bootstrap';
import { IfPermitted } from 'components/common';
import type { NotificationType } from 'components/notifications/types';
import useNotificationDismiss from 'components/notifications/hooks/useNotificationDismiss';

type Props = { row: NotificationType };

const ActionsCell = ({ row }: Props) => {
  const { mutate: dismiss, isPending } = useNotificationDismiss();

  return (
    <IfPermitted permissions="notifications:delete">
      <Button
        bsSize="xs"
        bsStyle="default"
        onClick={() => dismiss({ id: row.id })}
        disabled={isPending}
        aria-label="Dismiss">
        Dismiss
      </Button>
    </IfPermitted>
  );
};

export default ActionsCell;
