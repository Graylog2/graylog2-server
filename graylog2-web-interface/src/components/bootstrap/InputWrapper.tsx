import React from 'react';

type InputWrapperProps = {
  className?: string;
  children: React.ReactNode;
};

const InputWrapper = ({
  children,
  className
}: InputWrapperProps) => (className
  ? <div className={className}>{children}</div>
  : <span>{children}</span>);

export default InputWrapper;
