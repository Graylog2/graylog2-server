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
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { SystemIndexRanges } from '@graylog/server-api';

import { ButtonGroup, DropdownButton, MenuItem } from 'components/bootstrap';
import IncompatibleIndicesModal from 'components/indices/IncompatibleIndicesModal';

const AllIndicesMaintenanceDropdown = () => {
  const { t } = useTranslation('indexsets');
  const [showIncompatibleModal, setShowIncompatibleModal] = useState(false);

  const _onRecalculateAllIndexRange = () => {
    // eslint-disable-next-line no-alert
    if (window.confirm(t('indices.maintenance.confirm_recalculate_all'))) {
      SystemIndexRanges.rebuild();
    }
  };

  return (
    <ButtonGroup>
      <DropdownButton bsStyle="info" title={t('indices.maintenance.title')} id="indices-maintenance-actions" pullRight>
        <MenuItem eventKey="1" onClick={_onRecalculateAllIndexRange}>
          {t('indices.maintenance.recalculate_all')}
        </MenuItem>
        <MenuItem eventKey="4" onClick={() => setShowIncompatibleModal(true)}>
          {t('indices.maintenance.check_versions')}
        </MenuItem>
      </DropdownButton>
      {showIncompatibleModal && <IncompatibleIndicesModal show onClose={() => setShowIncompatibleModal(false)} />}
    </ButtonGroup>
  );
};

export default AllIndicesMaintenanceDropdown;
