import React from 'react';

import PageHeader from 'components/common/PageHeader';

const EmptyDashboardHelpMessage = () => (
  <PageHeader title="Empty Dashboard">
    <span>
      This dashboard is empty, because no widgets have been added to it.
      <p />
      Add widgets by clicking &quot;Add to dashboard&quot; in the widget&apos;s menu.
    </span>
  </PageHeader>
);

EmptyDashboardHelpMessage.propTypes = {};

export default EmptyDashboardHelpMessage;
