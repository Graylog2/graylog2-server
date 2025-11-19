type SortableItemLike = { id: string };

const getSortableItemId = (element: Element): string | undefined => {
  const childWithId = element.querySelector?.('[data-sortable-id]');

  return childWithId?.getAttribute('data-sortable-id') ?? undefined;
};

const createRect = (index: number, height: number, width: number): DOMRect => {
  const top = index * height;

  return DOMRect.fromRect({
    x: 0,
    y: top,
    width,
    height,
  });
};

const useSortableItemRectsMock = <Item extends SortableItemLike>(
  items: Array<Item>,
  {
    height = 10,
    width = 100,
  }: {
    height?: number;
    width?: number;
  } = {},
) => {
  let spy: jest.SpyInstance | undefined;

  beforeEach(() => {
    const originalGetBoundingClientRect = Element.prototype.getBoundingClientRect;

    spy = jest.spyOn(Element.prototype, 'getBoundingClientRect').mockImplementation(function mockRect(this: Element) {
      const sortableId = getSortableItemId(this);
      const index = sortableId ? items.findIndex((item) => item.id === sortableId) : -1;

      if (index >= 0) {
        return createRect(index, height, width);
      }

      return originalGetBoundingClientRect.call(this);
    });
  });

  afterEach(() => {
    spy?.mockRestore();
  });
};

export default useSortableItemRectsMock;
