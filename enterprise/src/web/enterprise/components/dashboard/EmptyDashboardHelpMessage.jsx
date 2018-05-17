import React from 'react';

import PageHeader from 'components/common/PageHeader';

const EmptyDashboardHelpMessage = () => (
  <PageHeader title="Empty Overview">
    <span>
      This overview is empty, because no widgets have been added to it.

      Add widgets by clicking &quot;Add to overview&quot; in the widget&apos;s menu.
    </span>
  </PageHeader>
);

EmptyDashboardHelpMessage.propTypes = {};

export default EmptyDashboardHelpMessage;
