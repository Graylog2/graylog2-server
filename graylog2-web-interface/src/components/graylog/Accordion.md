## Uncontrolled

```js
import AccordionItem from './AccordionItem';

const AccordionExample = () => (
  <Accordion id="accordion-test">
    <AccordionItem name="Example A">
      <h4>A as in Apple</h4>

      <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit. Amet aperiam autem, cum facere facilis illum
        incidunt
        non nulla pariatur quia, sunt temporibus voluptas? Adipisci consectetur ex molestiae nulla perferendis ut?
      </p>
      <p>Accusantium facilis in iste libero molestiae saepe temporibus! Accusantium adipisci amet consequatur culpa
        debitis, dolores est illum incidunt maxime nam necessitatibus non quam quas quis quod reiciendis reprehenderit
        tempore vitae.
      </p>
      <p>A alias atque commodi consectetur corporis distinctio dolorem eius ex explicabo facere harum illum nam
        nesciunt
        nihil nisi odit officia possimus, provident quam quis ratione repellendus sint sit soluta veniam?
      </p>
      <p>Assumenda, et, rerum! Ad animi aut cupiditate delectus possimus ratione suscipit veritatis voluptatibus.
        Accusamus alias culpa labore nostrum odio perferendis quia, repudiandae sapiente. Animi corporis culpa fugiat
        iusto sunt. Dolorem.
      </p>
      <p>Amet architecto culpa deleniti facilis, nemo non officia, porro provident quam tempore temporibus vero. Alias
        deserunt esse nihil numquam quaerat repudiandae, soluta. Consectetur ea excepturi facere incidunt natus,
        nesciunt. Doloribus!
      </p>
    </AccordionItem>

    <AccordionItem name="Example 2">
      <h4>Two as in Teeth</h4>

      <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit. Amet aperiam autem, cum facere facilis illum
        incidunt
        non nulla pariatur quia, sunt temporibus voluptas? Adipisci consectetur ex molestiae nulla perferendis ut?
      </p>
      <p>Accusantium facilis in iste libero molestiae saepe temporibus! Accusantium adipisci amet consequatur culpa
        debitis, dolores est illum incidunt maxime nam necessitatibus non quam quas quis quod reiciendis reprehenderit
        tempore vitae.
      </p>
      <p>A alias atque commodi consectetur corporis distinctio dolorem eius ex explicabo facere harum illum nam
        nesciunt
        nihil nisi odit officia possimus, provident quam quis ratione repellendus sint sit soluta veniam?
      </p>
      <p>Assumenda, et, rerum! Ad animi aut cupiditate delectus possimus ratione suscipit veritatis voluptatibus.
        Accusamus alias culpa labore nostrum odio perferendis quia, repudiandae sapiente. Animi corporis culpa fugiat
        iusto sunt. Dolorem.
      </p>
      <p>Amet architecto culpa deleniti facilis, nemo non officia, porro provident quam tempore temporibus vero. Alias
        deserunt esse nihil numquam quaerat repudiandae, soluta. Consectetur ea excepturi facere incidunt natus,
        nesciunt. Doloribus!
      </p>
    </AccordionItem>

    <AccordionItem name="Example III">
      <h4>Three as in Dimensions</h4>

      <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit. Amet aperiam autem, cum facere facilis illum
        incidunt
        non nulla pariatur quia, sunt temporibus voluptas? Adipisci consectetur ex molestiae nulla perferendis ut?
      </p>
      <p>Accusantium facilis in iste libero molestiae saepe temporibus! Accusantium adipisci amet consequatur culpa
        debitis, dolores est illum incidunt maxime nam necessitatibus non quam quas quis quod reiciendis reprehenderit
        tempore vitae.
      </p>
      <p>A alias atque commodi consectetur corporis distinctio dolorem eius ex explicabo facere harum illum nam
        nesciunt
        nihil nisi odit officia possimus, provident quam quis ratione repellendus sint sit soluta veniam?
      </p>
      <p>Assumenda, et, rerum! Ad animi aut cupiditate delectus possimus ratione suscipit veritatis voluptatibus.
        Accusamus alias culpa labore nostrum odio perferendis quia, repudiandae sapiente. Animi corporis culpa fugiat
        iusto sunt. Dolorem.
      </p>
      <p>Amet architecto culpa deleniti facilis, nemo non officia, porro provident quam tempore temporibus vero. Alias
        deserunt esse nihil numquam quaerat repudiandae, soluta. Consectetur ea excepturi facere incidunt natus,
        nesciunt. Doloribus!
      </p>
    </AccordionItem>
  </Accordion>
);

<AccordionExample />
```

## Controlled

```js
import AccordionItem from './AccordionItem';

const AccordionExample = () => {
  const [activeKey, setActiveKey] = React.useState('Example 2');
  
  return (
    <Accordion id="accordion-test" activeKey={activeKey} onSelect={setActiveKey}>
      <AccordionItem name="Example A">
        <h4>A as in Apple</h4>
  
        <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit. Amet aperiam autem, cum facere facilis illum 
         incidunt
          non nulla pariatur quia, sunt temporibus voluptas? Adipisci consectetur ex molestiae nulla perferendis ut?
        </p>
        <p>Accusantium facilis in iste libero molestiae saepe temporibus! Accusantium adipisci amet consequatur culpa
          debitis, dolores est illum incidunt maxime nam necessitatibus non quam quas quis quod reiciendis reprehenderit
          tempore vitae.
        </p>
        <p>A alias atque commodi consectetur corporis distinctio dolorem eius ex explicabo facere harum illum nam 
         nesciunt
          nihil nisi odit officia possimus, provident quam quis ratione repellendus sint sit soluta veniam?
        </p>
        <p>Assumenda, et, rerum! Ad animi aut cupiditate delectus possimus ratione suscipit veritatis voluptatibus.
          Accusamus alias culpa labore nostrum odio perferendis quia, repudiandae sapiente. Animi corporis culpa fugiat
          iusto sunt. Dolorem.
        </p>
        <p>Amet architecto culpa deleniti facilis, nemo non officia, porro provident quam tempore temporibus vero. Alias
          deserunt esse nihil numquam quaerat repudiandae, soluta. Consectetur ea excepturi facere incidunt natus,
          nesciunt. Doloribus!
        </p>
      </AccordionItem>
  
      <AccordionItem name="Example 2">
        <h4>Two as in Teeth</h4>
  
        <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit. Amet aperiam autem, cum facere facilis illum 
         incidunt
          non nulla pariatur quia, sunt temporibus voluptas? Adipisci consectetur ex molestiae nulla perferendis ut?
        </p>
        <p>Accusantium facilis in iste libero molestiae saepe temporibus! Accusantium adipisci amet consequatur culpa
          debitis, dolores est illum incidunt maxime nam necessitatibus non quam quas quis quod reiciendis reprehenderit
          tempore vitae.
        </p>
        <p>A alias atque commodi consectetur corporis distinctio dolorem eius ex explicabo facere harum illum nam 
         nesciunt
          nihil nisi odit officia possimus, provident quam quis ratione repellendus sint sit soluta veniam?
        </p>
        <p>Assumenda, et, rerum! Ad animi aut cupiditate delectus possimus ratione suscipit veritatis voluptatibus.
          Accusamus alias culpa labore nostrum odio perferendis quia, repudiandae sapiente. Animi corporis culpa fugiat
          iusto sunt. Dolorem.
        </p>
        <p>Amet architecto culpa deleniti facilis, nemo non officia, porro provident quam tempore temporibus vero. Alias
          deserunt esse nihil numquam quaerat repudiandae, soluta. Consectetur ea excepturi facere incidunt natus,
          nesciunt. Doloribus!
        </p>
      </AccordionItem>
  
      <AccordionItem name="Example III">
        <h4>Three as in Dimensions</h4>
  
        <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit. Amet aperiam autem, cum facere facilis illum 
         incidunt
          non nulla pariatur quia, sunt temporibus voluptas? Adipisci consectetur ex molestiae nulla perferendis ut?
        </p>
        <p>Accusantium facilis in iste libero molestiae saepe temporibus! Accusantium adipisci amet consequatur culpa
          debitis, dolores est illum incidunt maxime nam necessitatibus non quam quas quis quod reiciendis reprehenderit
          tempore vitae.
        </p>
        <p>A alias atque commodi consectetur corporis distinctio dolorem eius ex explicabo facere harum illum nam 
         nesciunt
          nihil nisi odit officia possimus, provident quam quis ratione repellendus sint sit soluta veniam?
        </p>
        <p>Assumenda, et, rerum! Ad animi aut cupiditate delectus possimus ratione suscipit veritatis voluptatibus.
          Accusamus alias culpa labore nostrum odio perferendis quia, repudiandae sapiente. Animi corporis culpa fugiat
          iusto sunt. Dolorem.
        </p>
        <p>Amet architecto culpa deleniti facilis, nemo non officia, porro provident quam tempore temporibus vero. Alias
          deserunt esse nihil numquam quaerat repudiandae, soluta. Consectetur ea excepturi facere incidunt natus,
          nesciunt. Doloribus!
        </p>
      </AccordionItem>
    </Accordion>
  );
};

<AccordionExample />
```
