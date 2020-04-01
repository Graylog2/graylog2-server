import React from 'react';

import { DocumentationLink } from 'components/support';
import { Icon } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import PortaledPopover from 'views/components/common/PortaledPopover';
import styles from './MessageWidgets.css';

const popover = (
  <span>
    <p>
      Do not hesitate to consult the Graylog community if your questions are not answered in the{' '}
      <DocumentationLink page={DocsHelper.PAGES.WELCOME} text="documentation" />.
    </p>

    <ul>
      <li>
        <Icon name="group" />&nbsp;
        <a href="https://www.graylog.org/community-support/" target="_blank" rel="noopener noreferrer">Community support</a>
      </li>
      <li>
        <Icon name={{ prefix: 'fab', iconName: 'github-alt' }} />&nbsp;
        <a href="https://github.com/Graylog2/graylog2-server/issues" target="_blank" rel="noopener noreferrer">Issue tracker</a>
      </li>
      <li>
        <Icon name="heart" />&nbsp;
        <a href="https://www.graylog.org/professional-support" target="_blank" rel="noopener noreferrer">Professional support</a>
      </li>
    </ul>
  </span>
);

const EmptyResultWidget = () => (
  <div className={styles.spinnerContainer}>
    <Icon name="times" size="3x" className={styles.iconMargin} />
    <div>
      <strong>
        Your search returned no results, try changing the used time range or the search query.{' '}
      </strong>

      <br />
      Take a look at the{' '}
      <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE} text="documentation" />{' '}
      if you need help with the search syntax or the time range selector.
      Or <PortaledPopover popover={popover} title="Need help?">click here</PortaledPopover> if you are stuck!
    </div>
  </div>
);

EmptyResultWidget.propTypes = {};

export default EmptyResultWidget;
