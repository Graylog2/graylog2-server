// @flow strict
import * as React from 'react';

import Button from 'components/graylog/Button';
import SharingDisabledPopover from 'components/permissions/SharingDisabledPopover';
import HasOwnership from 'components/common/HasOwnership';
import Icon from 'components/common/Icon';

type Props = {
  disabled?: boolean,
  entityId: string,
  entityType: string,
  onClick: () => void,
  bsStyle?: string,
};

const ShareButton = ({ bsStyle, entityId, entityType, onClick, disabled }: Props) => (
  <HasOwnership id={entityId} type={entityType}>
    {({ disabled: hasMissingPermissions }) => (
      <Button bsStyle={bsStyle} onClick={onClick} disabled={disabled || hasMissingPermissions} title="Share">
        <Icon name="user-plus" /> Share {(!disabled && hasMissingPermissions) && <SharingDisabledPopover type={entityType} />}
      </Button>
    )}
  </HasOwnership>
);

ShareButton.defaultProps = {
  bsStyle: 'info',
  disabled: false,
};

export default ShareButton;
