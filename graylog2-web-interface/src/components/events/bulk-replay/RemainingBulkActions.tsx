import * as React from 'react';

import useEventBulkActions from 'components/events/events/hooks/useEventBulkActions';
import type { RemainingBulkActionsProps } from 'components/events/bulk-replay/types';

import DropdownButton from '../../bootstrap/DropdownButton';

const RemainingBulkActions = ({ completed, events }: RemainingBulkActionsProps) => {
  const { actions, pluggableActionModals } = useEventBulkActions(events);

  return (
    <>
      <DropdownButton title="Bulk actions"
                      bsStyle={completed ? 'success' : 'default'}
                      id="bulk-actions-dropdown"
                      disabled={!events?.length}>
        {actions}
      </DropdownButton>
      {pluggableActionModals}
    </>
  );
};

export default RemainingBulkActions;
