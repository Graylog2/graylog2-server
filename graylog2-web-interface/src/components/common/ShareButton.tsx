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

import SharingDisabledPopover from 'components/permissions/SharingDisabledPopover';
import HasOwnership from 'components/common/HasOwnership';
import IconButton from 'components/common/IconButton';

type Props = {
  /**
   * When a custom description is provided
   * the button will be disabled
   */
  disabledInfo?: string;
  entityId: string;
  entityType: string;
  onClick: () => void;
  bsStyle?: 'default';
  bsSize?: 'xs' | 'xsmall' | 'md' | 'medium';
};

const ShareButton = ({
  entityId,
  bsSize = undefined,
  bsStyle = 'default',
  entityType,
  onClick,
  disabledInfo = undefined,
}: Props) => (
  <HasOwnership id={entityId} type={entityType}>
    {({ disabled: hasMissingPermissions }) => (
      <>
        <IconButton
          name="person_add"
          title="Share"
          bsStyle={bsStyle}
          size={bsSize}
          iconSize={null}
          onClick={onClick}
          disabled={!!disabledInfo || hasMissingPermissions}
        />
        {(!!disabledInfo || hasMissingPermissions) && (
          <SharingDisabledPopover type={entityType} description={disabledInfo} />
        )}
      </>
    )}
  </HasOwnership>
);

export default ShareButton;
