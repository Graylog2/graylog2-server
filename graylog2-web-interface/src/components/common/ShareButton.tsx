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

import type { StyleProps } from 'components/bootstrap/Button';
import Button from 'components/bootstrap/Button';
import SharingDisabledPopover from 'components/permissions/SharingDisabledPopover';
import HasOwnership from 'components/common/HasOwnership';
import Icon from 'components/common/Icon';
import type { BsSize } from 'components/bootstrap/types';

type Props = {
  /**
   * When a custom description is provided
   * the button will be disabled
   */
  disabledInfo?: string,
  entityId: string,
  entityType: string,
  onClick: () => void,
  bsStyle?: StyleProps,
  bsSize?: BsSize,
};

const ShareButton = ({ bsStyle = 'default', bsSize, entityId, entityType, onClick, disabledInfo }: Props) => (
  <HasOwnership id={entityId} type={entityType}>
    {({ disabled: hasMissingPermissions }) => (
      <Button bsStyle={bsStyle}
              bsSize={bsSize}
              onClick={onClick}
              disabled={!!disabledInfo || hasMissingPermissions}
              title="Share">
        <Icon name="person_add" /> Share {(!!disabledInfo || hasMissingPermissions) && <SharingDisabledPopover type={entityType} description={disabledInfo} />}
      </Button>
    )}
  </HasOwnership>
);

export default ShareButton;
