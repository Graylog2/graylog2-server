import React from 'react';

type Option = { value: string; label: string };

type RenderOptionsType = (options: Option[], label: string, loading?: boolean) => React.ReactElement;

function Options({ value, label }: Option): React.ReactElement {
  return (
    <option value={value} key={value}>
      {label}
    </option>
  );
}

const renderOptions: RenderOptionsType = (options = [], label = 'Choose One', loading = false) => {
  if (loading) {
    return Options({ value: '', label: 'Loading...' });
  }

  return (
    <>
      <option value="">{label}</option>
      {options.map((option) => Options({ value: option.value, label: option.label }))}
    </>
  );
};

export default Options;

export { renderOptions };
