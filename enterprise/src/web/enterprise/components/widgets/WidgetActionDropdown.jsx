import React from 'react';
import PropTypes from 'prop-types';
import { MenuItem } from 'react-bootstrap';
import { Overlay } from 'react-overlays';

/**
 * This implements a custom toggle for a dropdown menu.
 * See: "Custom Dropdown Components" in react-bootstrap documentation.
 */
const WidgetActionToggle = ({ children, onClick }) => {
  const handleClick = (e) => {
    e.preventDefault();
    onClick(e);
  };

  return (
    <span onClick={handleClick} role="presentation">
      {children}
    </span>
  );
};

WidgetActionToggle.propTypes = {
  children: PropTypes.node.isRequired,
  onClick: PropTypes.func,
};

WidgetActionToggle.defaultProps = {
  onClick: () => {},
};

class WidgetActionDropdown extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      show: false,
    };
  }

  _onToggle = () => this.setState(({ show }) => ({ show: !show }));

  render() {
    const { children, container, element } = this.props;
    const { show } = this.state;
    const displayMenu = show ? { display: 'block' } : { display: 'none' };
    const listStyle = {
      minWidth: 'max-content',
      color: '#666666',
    };
    return (
      <span>
        <WidgetActionToggle bsRole="toggle" onClick={this._onToggle}>
          <span ref={(elem) => { this.target = elem; }}>{element}</span>
        </WidgetActionToggle>
        <Overlay show={show}
                 container={container}
                 placement="bottom"
                 shouldUpdatePosition
                 rootClose
                 onHide={this._onToggle}
                 target={() => this.target}>
          <ul className="dropdown-menu" style={Object.assign({}, listStyle, displayMenu)}>
            <MenuItem header>Actions</MenuItem>
            {children}
          </ul>
        </Overlay>
      </span>
    );
  }
}


WidgetActionDropdown.propTypes = {
  children: PropTypes.node.isRequired,
  container: PropTypes.oneOfType([PropTypes.node, PropTypes.func]),
  element: PropTypes.node.isRequired,
};

WidgetActionDropdown.defaultProps = {
  container: undefined,
};

export default WidgetActionDropdown;
