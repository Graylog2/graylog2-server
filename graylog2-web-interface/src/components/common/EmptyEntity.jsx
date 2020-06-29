import React from 'react';
import PropTypes from 'prop-types';

import style from './EmptyEntity.css';

/**
 * Component used to represent an empty entity in Graylog. This component allows us to display some larger
 * text to the user explaining what that entity is and a link to create a new one.
 */
class EmptyEntity extends React.Component {
  static propTypes = {
    /** Text or node to be rendered as title. */
    title: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.node,
    ]),
    /**
     * Any other content the component should display below the title. This may include a description and button
     * or link to easily create a new entity.
     */
    children: PropTypes.node.isRequired,
  };

  static defaultProps = {
    title: 'Looks like there is nothing here, yet!',
  };

  render() {
    const { children, title } = this.props;

    return (
      <div className={style.component}>
        <h3>{title}</h3>
        {children}
      </div>
    );
  }
}

export default EmptyEntity;
