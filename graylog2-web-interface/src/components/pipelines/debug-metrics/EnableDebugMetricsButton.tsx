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
import React, { useState } from 'react';

import { Button } from 'components/bootstrap';
import IfPermitted from 'components/common/IfPermitted';
import Popover from 'components/common/Popover';
import RuleMetricsConfigContainer from 'components/rules/RuleMetricsConfigContainer';

import useDebugMetricsConfig from './useDebugMetricsConfig';

const EDIT_PERMISSION = 'pipeline:edit';

const HELP_TEXT =
  'Pipeline Load (15m) shows the relative share of pipeline rule execution time across the cluster. ' +
  'Enable debug metrics to start collecting timer data. Values may take up to about 15 minutes to populate ' +
  'after enabling, and collection adds overhead to message processing.';

const EnableDebugMetricsButton = () => {
  const { metricsEnabled, isLoading, refresh } = useDebugMetricsConfig();
  const [showConfigModal, setShowConfigModal] = useState(false);
  const [showHelp, setShowHelp] = useState(false);

  if (isLoading || metricsEnabled) {
    return null;
  }

  const closeConfigModal = () => {
    setShowConfigModal(false);
    refresh();
  };

  return (
    <IfPermitted permissions={[EDIT_PERMISSION]}>
      <Popover opened={showHelp} position="bottom" width={360} withArrow>
        <Popover.Target>
          <span
            onMouseOver={() => setShowHelp(true)}
            onMouseOut={() => setShowHelp(false)}
            onFocus={() => setShowHelp(true)}
            onBlur={() => setShowHelp(false)}>
            <Button bsStyle="info" bsSize="md" onClick={() => setShowConfigModal(true)}>
              Enable debug metrics
            </Button>
          </span>
        </Popover.Target>
        <Popover.Dropdown>{HELP_TEXT}</Popover.Dropdown>
      </Popover>
      {showConfigModal && <RuleMetricsConfigContainer onClose={closeConfigModal} />}
    </IfPermitted>
  );
};

export default EnableDebugMetricsButton;
