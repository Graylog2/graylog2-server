// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { Button, OverlayTrigger, Popover } from 'components/graylog';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';

import DecoratorStyles from './decoratorStyles.css';

const PopoverHelp = () => {
  const popoverHelp = (
    <Popover id="decorators-help" className={DecoratorStyles.helpPopover}>
      <p className="description">
        Decorators can modify messages shown in the search results on the fly. These changes are not stored, but only
        shown in the search results. Decorator config is stored <strong>per stream</strong>.
      </p>
      <p className="description">
        Use drag and drop to modify the order in which decorators are processed.
      </p>
      <p>
        Read more about message decorators in the <DocumentationLink page={DocsHelper.PAGES.DECORATORS} text="documentation" />.
      </p>
    </Popover>
  );
  return (
    <div className={DecoratorStyles.helpLinkContainer}>
      <OverlayTrigger trigger="click" rootClose placement="right" overlay={popoverHelp}>
        <Button bsStyle="link" className={DecoratorStyles.helpLink}>What are message decorators?</Button>
      </OverlayTrigger>
    </div>
  );
};

PopoverHelp.propTypes = {};

export default PopoverHelp;
