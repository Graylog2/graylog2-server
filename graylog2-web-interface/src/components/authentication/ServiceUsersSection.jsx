// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

type Props = {
  children: string,
};

const ServiceUsersSection = ({ children }: Props) => (<>{children}</>);

ServiceUsersSection.propTypes = {
  children: PropTypes.string,
};

ServiceUsersSection.defaultProps = {
  children: 'Hello World!',
};

export default ServiceUsersSection;
