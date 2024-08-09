import * as React from 'react';
import styled, { css } from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Icon, Tooltip } from 'components/common';

type Props = {
  isArchivingEnabled: boolean,
  streamId: string,
}
const Wrapper = styled.div<{ $enabled: boolean }>(({ theme, $enabled }) => css`
  color: ${$enabled ? theme.colors.variant.success : theme.colors.variant.default};
`);
const StyledDiv = styled.div`
  display: flex;
`;

const IndexSetArchivingCell = ({ isArchivingEnabled, streamId }: Props) => {
  const StreamIndexSetDataWarehouseWarning = PluginStore.exports('dataWarehouse')?.[0]?.StreamIndexSetDataWarehouseWarning;

  return (
    <StyledDiv>
      <Tooltip withArrow position="right" label={`Archiving is ${isArchivingEnabled ? 'enabled' : 'disabled'}`}>
        <Wrapper $enabled={isArchivingEnabled}>
          <Icon name={isArchivingEnabled ? 'check_circle' : 'cancel'} />
        </Wrapper>
      </Tooltip>
      {StreamIndexSetDataWarehouseWarning && (<StreamIndexSetDataWarehouseWarning streamId={streamId} isArchivingEnabled={isArchivingEnabled} />)}
    </StyledDiv>
  );
};

export default IndexSetArchivingCell;
