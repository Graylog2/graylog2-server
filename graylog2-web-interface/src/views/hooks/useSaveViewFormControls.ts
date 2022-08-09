import usePluginEntities from 'views/logic/usePluginEntities';

const useSaveViewFormControls = () => {
  const pluggableSaveViewControlFns = usePluginEntities('views.components.saveViewForm');

  return pluggableSaveViewControlFns.map((controlFn) => controlFn()).filter((control) => !!control);
};

export default useSaveViewFormControls;
