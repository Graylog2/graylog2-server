Simple usage. You can use `ExpandableCheckboxListItem` to have checkboxes in the list.

```tsx
import { ExpandableListItem, ExpandableCheckboxListItem } from 'components/common';

const ExpandableListExample = () => (
  <ExpandableList>
    <ExpandableListItem header="Wheel of Time Character" value="character">
      <ExpandableList>
        <ExpandableCheckboxListItem header="Andor" value="andor">
          <ExpandableList>
            <ExpandableListItem header="Elayne" value="elayne">The content</ExpandableListItem>
            <ExpandableListItem expandable={false} header="Morgase" value="morgase" />
          </ExpandableList>
        </ExpandableCheckboxListItem>
        <ExpandableListItem header="Edmonds Field" value="edmonds"> 
          <ExpandableList>
            <ExpandableListItem header="Rand" value="rand" expandable={false}/>
            <ExpandableListItem header="Perrin" value="perrin" expandable={false}/>
            <ExpandableListItem header="Egwene" value="egwene">
              The content
            </ExpandableListItem>
            <ExpandableListItem  header="Mat" value="mat" expandable={false} />
            <ExpandableListItem header="Nynaeve" value="nynaeve" expandable={false} />
          </ExpandableList>
        </ExpandableListItem>
      </ExpandableList>
    </ExpandableListItem>
  </ExpandableList>
);

<ExpandableListExample />
```

Control which sections are expanded by default.

```tsx
import { useState } from 'react';
import { ExpandableListItem } from 'components/common';

const ExpandableListExample = () => {
  const [expandedSection, setExpandedSection] = useState(['fred', 'morgase']);
  return (
    <ExpandableList value={expandedSection} onChange={(newExpandedSections) => setExpandedSection(newExpandedSections)}>
      <ExpandableListItem header="Elayne" value="elayne">The content</ExpandableListItem>
      <ExpandableListItem header="Fred" value="fred">The content</ExpandableListItem>
      <ExpandableListItem header="Morgase" value="morgase">The content</ExpandableListItem>
    </ExpandableList>
  );
};

<ExpandableListExample />;
```
