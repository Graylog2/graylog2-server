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
// @flow strict
import * as React from 'react';

import Button from 'components/graylog/Button';
import SharingDisabledPopover from 'components/permissions/SharingDisabledPopover';
import HasOwnership from 'components/common/HasOwnership';
import Icon from 'components/common/Icon';

type Props = {
  /**
   * When a custom description is provided
   * the button will be disabled
   */
  disabledInfo?: string,
  entityId: string,
  entityType: string,
  onClick: () => void,
  bsStyle?: string,
};

const ShareButton = ({ bsStyle, entityId, entityType, onClick, disabledInfo }: Props) => (
  <HasOwnership id={entityId} type={entityType}>
    {({ disabled: hasMissingPermissions }) => (
      <Button bsStyle={bsStyle} onClick={onClick} disabled={!!disabledInfo || hasMissingPermissions} title="Share">
        <Icon name="user-plus" /> Share {(!!disabledInfo || hasMissingPermissions) && <SharingDisabledPopover type={entityType} description={disabledInfo} />}
      </Button>
    )}
  </HasOwnership>
);

ShareButton.defaultProps = {
  bsStyle: 'info',
  disabledInfo: undefined,
};

export default ShareButton;
