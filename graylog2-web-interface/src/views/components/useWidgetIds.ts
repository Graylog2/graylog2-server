import useAppSelector from 'stores/useAppSelector';

const useWidgetIds = () => useAppSelector((state) => state?.view?.view?.state?.map((viewState) => viewState.widgets.map((widget) => widget.id).toList()).toMap());

export default useWidgetIds;
