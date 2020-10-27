// @flow strict
import * as React from 'react';

import Button from 'components/graylog/Button';
import SharingDisabledPopover from 'components/permissions/SharingDisabledPopover';
import HasOwnership from 'components/common/HasOwnership';
import Icon from 'components/common/Icon';

type Props = {
  entityId: string,
  entityType: string,
  onClick: () => void,
};

const ShareButton = ({ entityId, entityType, onClick }: Props) => (
  <HasOwnership id={entityId} type={entityType}>
    {({ disabled }) => (
      <Button bsStyle="info" onClick={onClick} disabled={disabled}>
        <Icon name="user-plus" /> Share {disabled && <SharingDisabledPopover type={entityType} />}
      </Button>
    )}
  </HasOwnership>
);

export default ShareButton;
