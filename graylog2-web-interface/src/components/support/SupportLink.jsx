import PropTypes from 'prop-types';
import React from 'react';
import Icon from 'components/common/Icon';

const SupportLink = ({ small, children }) => {
  return (
    <table className="description-tooltips" style={{ marginBottom: '10px' }}>
      <tbody>
        <tr>
          <td style={{ width: '40px' }}>
            <span className={`fa-stack ${!small && 'fa-lg'}`}>
              <Icon name="circle" className="fa-stack-2x" />
              <Icon name="lightbulb-o" className="fa-stack-1x" inverse />
            </span>
          </td>
          <td>
            <strong>
              {children}
            </strong>
          </td>
        </tr>
      </tbody>
    </table>
  );
};

SupportLink.propTypes = {
  small: PropTypes.bool,
  children: PropTypes.node.isRequired,
};

SupportLink.defaultProps = {
  small: false,
};

export default SupportLink;
