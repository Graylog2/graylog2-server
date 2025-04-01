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
import styled from 'styled-components';

import { Button } from 'components/bootstrap';
import { Icon } from 'components/common';
import Popover from 'components/common/Popover';

const StyledButton = styled(Button)`
  padding: 1px 0;
`;

const DataNodeUpgradeHelp = () => {
  const [showHelp, setShowHelp] = useState(false);
  const toggleHelp = () => setShowHelp((cur) => !cur);

  return (
    <Popover
      position="right"
      width={500}
      opened={showHelp}
      withArrow
      onChange={setShowHelp}
      closeOnClickOutside
      withinPortal>
      <Popover.Target>
        <StyledButton bsStyle="transparent" bsSize="xs" onClick={toggleHelp}>
          <Icon name="question_mark" />
        </StyledButton>
      </Popover.Target>
      <Popover.Dropdown>
        <p>How does my cluster change state during the rolling upgrade?</p>
        <p>
          RED - if you are using indices with no replication and upgrade the node hosting the shards of these indices,
          the cluster will go to a red state and no data will be ingested into or searchable from these indices.
        </p>
        <p>
          YELLOW - after starting the upgrade of a node, shard allocation will be set to no replication to allow
          OpenSearch to use only the available shards.
        </p>
        <p>
          After a node has been upgraded and you click on <em>Confirm Upgrade</em>, shard replication will be re-enabled
          and all shards that were unavailable due to the node being upgraded will be re-allocated and the cluster will
          return to a GREEN state.
        </p>
      </Popover.Dropdown>
    </Popover>
  );
};

export default DataNodeUpgradeHelp;
