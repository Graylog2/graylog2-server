import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';

import 'font-awesome/css/font-awesome.css';

/**
 * Component that renders an icon or glyph.
 * Uses Font Awesome 4.7 : https://fontawesome.com/v4.7.0/icons/
 *
 * No need to pass `fa` or `fa-` prefixes, just the name of the icon
 */

const Icon = React.forwardRef(({
  className,
  flip,
  fixedWidth,
  inverse,
  name,
  pulse,
  rotate,
  size,
  spin,
  ...props
}, ref) => {
  const cleanIconName = name.replace(/^fa-/, ''); // remove "fa-" prefix if it exists

  const iconClasses = classnames('fa',
    `fa-${cleanIconName}`,
    className,
    {
      [`fa-flip-${flip}`]: !!flip,
      [`fa-rotate-${rotate}`]: !!rotate,
      [`fa-${size}`]: !!size,
      'fa-fw': !!fixedWidth,
      'fa-inverse': !!inverse,
      'fa-pulse': !!pulse,
      'fa-spin': !!spin,
    });

  return (
    <i className={iconClasses} {...props} ref={ref} />
  );
});

Icon.propTypes = {
  /** Pass through any custom or Font Awesome specific classes */
  className: PropTypes.string,
  /** Flip icon output */
  flip: PropTypes.oneOf(['horizontal', 'vertical']),
  /** Use when different Icon widths throw off alignment. */
  fixedWidth: PropTypes.bool,
  /** Can be used as an alternative Icon color. */
  inverse: PropTypes.bool,
  /** Name of Font Awesome 4.7 Icon without `fa-` prefix */
  name: PropTypes.string.isRequired,
  /** Have Icon rotate with 8 steps */
  pulse: PropTypes.bool,
  /** Rotate icon output */
  rotate: PropTypes.oneOf(['90', '180', '270']),
  /** Increase Icon sizes relative to their container */
  size: PropTypes.oneOf(['lg', '2x', '3x', '4x', '5x']),
  /** Have any Icon to rotate */
  spin: PropTypes.bool,
};

Icon.defaultProps = {
  className: undefined,
  flip: undefined,
  fixedWidth: false,
  inverse: false,
  pulse: false,
  rotate: undefined,
  size: undefined,
  spin: false,
};

export default Icon;
