// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { Popover, OverlayTrigger } from 'components/graylog';
import Icon from 'components/common/Icon';

type Props = {
  children: React.Node,
  id?: string,
  title: string,
  className: string,
  pullRight: boolean,
};

const HoverForHelp = ({ children, className, title, id, pullRight }: Props) => (
  <OverlayTrigger trigger={['hover', 'focus']}
                  placement="bottom"
                  overlay={(
                    <Popover title={title} id={id}>
                      {children}
                    </Popover>
                  )}>
    <Icon className={`${className} ${pullRight ? 'pull-right' : ''}`} name="question-circle" />
  </OverlayTrigger>
);

HoverForHelp.propTypes = {
  children: PropTypes.any.isRequired,
  className: PropTypes.string,
  title: PropTypes.string.isRequired,
  id: PropTypes.string,
  pullRight: PropTypes.bool,
};

HoverForHelp.defaultProps = {
  id: 'help-popover',
  className: '',
  pullRight: true,
};

export default HoverForHelp;
