import classNames from 'classnames';
import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const StyledLi = styled.li(({ theme }) => `
  && {
    > a {
      color: ${theme.utils.readableColor(theme.colors.global.contentBackground)};
      background-color: ${theme.colors.global.contentBackground};
      border-color: ${theme.colors.gray[80]};

      &:hover,
      &:focus {
        color: ${theme.utils.readableColor(theme.colors.gray[90])};
        background-color: ${theme.colors.gray[90]};
        border-color: ${theme.colors.gray[80]};
      }
    }

    &.active > a {
      &,
      &:hover,
      &:focus {
        color: ${theme.utils.readableColor(theme.colors.gray[90])};
        background-color: ${theme.colors.gray[90]};
        border-color: ${theme.colors.gray[80]};
      }
    }

    &.disabled {
      > a,
      > a:hover,
      > a:focus {
        color: ${theme.colors.gray[40]};
        background-color: ${theme.colors.gray[80]};
        border-color: ${theme.colors.gray[80]};
      }
    }
  }
`);

const PaginationButton = ({
  active,
  children,
  className,
  disabled,
  eventKey,
  onClick,
  ...props
}) => {
  return (
    <StyledLi className={classNames(className, { active, disabled })}>
      {/* eslint-disable-next-line jsx-a11y/anchor-is-valid */}
      <a {...props}
         disabled={disabled}
         href="#"
         onClick={(event) => {
           onClick(eventKey, event);
         }}>
        {children}
      </a>
    </StyledLi>
  );
};

PaginationButton.propTypes = {
  active: PropTypes.bool,
  children: PropTypes.node,
  className: PropTypes.string,
  disabled: PropTypes.bool,
  eventKey: PropTypes.number,
  onClick: PropTypes.func,
};

PaginationButton.defaultProps = {
  active: false,
  children: undefined,
  className: undefined,
  disabled: false,
  eventKey: 0,
  onClick: () => {},
};

export default PaginationButton;
