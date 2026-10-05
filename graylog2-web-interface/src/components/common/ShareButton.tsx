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

import HasOwnership from 'components/common/HasOwnership';
import IconButton from 'components/common/IconButton';
import type { StyleProps } from 'components/bootstrap/Button';

type Props = {
  /**
   * When a custom description is provided
   * the button will be disabled
   */
  disabledInfo?: string;
  entityId: string;
  entityType: string;
  onClick: () => void;
  bsStyle?: StyleProps;
  bsSize?: 'xs' | 'xsmall' | 'md' | 'medium';
  title?: string;
  /** Show the title text next to the icon, e.g. when multiple share buttons appear together and the icon alone can't tell them apart. */
  showTitle?: boolean;
};

const ShareButton = ({
  entityId,
  entityType,
  onClick,
  disabledInfo = undefined,
  bsSize = undefined,
  bsStyle = 'default',
  title = 'Share',
  showTitle = false,
}: Props) => (
  <HasOwnership id={entityId} type={entityType}>
    {({ disabled: hasMissingPermissions }) => {
      const isDisabled = !!disabledInfo || hasMissingPermissions;
      const tooltipLabel = isDisabled
        ? disabledInfo || `Only owners of this ${entityType.replaceAll('_', ' ')} can share it.`
        : title;

      return (
        <IconButton
          name="person_add"
          title={tooltipLabel}
          ariaLabel={title}
          bsStyle={bsStyle}
          iconSize="inherit"
          size={bsSize}
          onClick={onClick}
          disabled={isDisabled}
          showTitle={showTitle}
          allowClickWhenDisabled
        />
      );
    }}
  </HasOwnership>
);

export default ShareButton;
