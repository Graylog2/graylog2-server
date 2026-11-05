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
import { useTranslation } from 'react-i18next';

import { MenuItem } from 'components/bootstrap';
import SharingDisabledPopover from 'components/permissions/SharingDisabledPopover';
import HasOwnership from 'components/common/HasOwnership';

type Props = {
  /**
   * When a custom description is provided
   * the button will be disabled
   */
  disabledInfo?: string;
  entityId: string;
  entityType: string;
  onClick: () => void;
  title?: string;
};

const ShareMenuItem = ({ entityId, entityType, onClick, disabledInfo = undefined, title = undefined }: Props) => {
  const { t } = useTranslation('common');

  return (
    <HasOwnership id={entityId} type={entityType}>
      {({ disabled: hasMissingPermissions }) => (
        <MenuItem
          onSelect={onClick}
          disabled={!!disabledInfo || hasMissingPermissions}
          title={t('share_menu_item.share')}>
          {title ?? t('share_menu_item.share')}{' '}
          {(!!disabledInfo || hasMissingPermissions) && (
            <SharingDisabledPopover type={entityType} description={disabledInfo} />
          )}
        </MenuItem>
      )}
    </HasOwnership>
  );
};

export default ShareMenuItem;
