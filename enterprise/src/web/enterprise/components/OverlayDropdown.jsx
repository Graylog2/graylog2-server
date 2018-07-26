import React from 'react';
import PropTypes from 'prop-types';

import { Overlay } from 'react-overlays';

import styles from './OverlayDropdown.css';

export default class OverlayDropdown extends React.Component {
  static propTypes = {
    children: PropTypes.node.isRequired,
    menuContainer: PropTypes.object,
    placement: PropTypes.string,
    toggle: PropTypes.oneOfType([PropTypes.string, PropTypes.node]).isRequired,
  };

  static defaultProps = {
    menuContainer: document.body,
    placement: 'bottom',
  };

  render() {
    const { children, menuContainer, onToggle, show, toggle } = this.props;
    const displayMenu = show ? { display: 'block' } : { display: 'none' };
    const listStyle = {
      paddingLeft: '5px',
      paddingRight: '5px',
      minWidth: 'max-content',
      color: '#666666',
    };

    return (
      <span>
        <span onClick={onToggle}
              ref={(elem) => { this.target = elem; }}
              role="presentation"
              className={styles.dropdowntoggle}>
          {toggle}<span className="caret" />
        </span>
        <Overlay show={show}
                 container={menuContainer}
                 containerPadding={10}
                 placement={this.props.placement}
                 shouldUpdatePosition
                 rootClose
                 onHide={onToggle}
                 target={() => this.target}>
          <ul className="dropdown-menu" style={Object.assign({}, listStyle, displayMenu)}>
            {children}
          </ul>
        </Overlay>
      </span>
    );
  }
};
