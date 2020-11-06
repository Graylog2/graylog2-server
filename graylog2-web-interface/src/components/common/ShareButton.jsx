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
