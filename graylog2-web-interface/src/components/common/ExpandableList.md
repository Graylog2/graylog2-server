```js
import createReactClass from 'create-react-class';
import { ExpandableListItem } from 'components/common';

const ExpandableListExample = createReactClass({

  render() {
    return (
      <ExpandableList>
        <ExpandableListItem selectable expanded header="Wheel of Time Character">
          <ExpandableList>
            <ExpandableListItem selectable header="Andor">
              <ExpandableList>
                <ExpandableListItem selectable expandable={false} header="Elayne" />
                <ExpandableListItem selectable expandable={false} header="Morgase" />
              </ExpandableList>
            </ExpandableListItem>
            <ExpandableListItem selectable header="Edmonds Field">
              <ExpandableList>
                <ExpandableListItem selectable expandable={false} header="Rand" />
                <ExpandableListItem selectable expandable={false} header="Perrin" />
                <ExpandableListItem selectable expandable={false} header="Egwene" />
                <ExpandableListItem checked expandable={false} header="Mat" />
                <ExpandableListItem expandable={false} selectable header="Nynaeve" />
              </ExpandableList>
            </ExpandableListItem>
          </ExpandableList>
        </ExpandableListItem>
      </ExpandableList>
    );
  },
});

<ExpandableListExample />

```
