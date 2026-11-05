# Ukrainian glossary

Terms used in the Ukrainian translations (`uk/*.json`), so the whole interface uses the same words. Based on
Microsoft's Ukrainian terminology where it fits a log management product, and on the decisions made for Polish
(proper Ukrainian terms instead of English words with Ukrainian endings).

## Style

- Address the user politely with "ви" (lower case), as modern Ukrainian software does: "Ви можете…" only at the
  start of a sentence. Buttons and menu items use the infinitive ("Створити", "Видалити", "Зберегти").
- Use Ukrainian typography: quotes «…», the apostrophe ’ (U+2019) in words like "пам’ять", "об’єкт", an ellipsis
  "..." where the English has one, a colon where the English has one.
- Never use Russian words or Russian spelling (e.g. "настройки", "поиск", "удалить"). Use standard modern Ukrainian.
- Keep product and feature names in English: Graylog, Enterprise, Data Lake, Data Node, Security (as a product
  name, e.g. "{{productName}} Security"), Sidecar, Collector, Forwarder, Fleet, Pipeline, Illuminate, Sigma,
  Watchlist, MITRE ATT&CK, Investigation, Asset, Detection Chain, Data Tiering, Input Profile, Content Hub,
  Warm Tier, OpenSearch, Elasticsearch, AWS and other vendor and product names. Fixed names like "Default Stream"
  stay English too.
- Translate only the value. Keep every `{{placeholder}}`, `<tag>` / `</tag>` and `<0>`-style Trans markers
  exactly as they are, plus technical identifiers in `backticks`, field names, query syntax and URLs.
- Plural keys: `_one` = 1, 21, 31 … ("1 повідомлення"), `_few` = 2-4, 22-24 … , `_many` = 0, 5-20, 25-30 … ,
  `_other` = fractions. The helper script gives the English plural text as hint for `_few/_many/_other`.
- An English feature name inside a Ukrainian sentence is not declined: "у Pipeline", "для Sidecar". If that reads
  badly, add a noun: "у правилі Pipeline".

## Terms

| English | Ukrainian |
| --- | --- |
| Stream | потік (Default Stream stays English) |
| Input | вхід (pl. входи) |
| Output | вихід (pl. виходи) |
| Dashboard | панель моніторингу (pl. панелі моніторингу) |
| Widget | віджет |
| Extractor | екстрактор |
| Lookup Table | таблиця підстановки |
| Index Set | набір індексів |
| Index | індекс |
| Node | вузол |
| Cluster | кластер |
| Content Pack | пакет вмісту |
| Event | подія |
| Event Definition | визначення події |
| Alert | оповіщення |
| Notification | сповіщення |
| Report | звіт |
| Search (noun / verb) | пошук / шукати |
| Query | запит |
| Message | повідомлення |
| Field | поле |
| Source | джерело |
| Timestamp | позначка часу |
| Time range | часовий діапазон |
| All time | увесь час |
| Throughput | пропускна здатність |
| Shard / Replica | шард / репліка |
| Journal | журнал |
| Buffer | буфер |
| Processing | обробка |
| Rule / Stream rule | правило / правило потоку |
| Archive | архів |
| Audit log | журнал аудиту |
| Retention | зберігання |
| Anomaly | аномалія |
| Risk score | оцінка ризику |
| User / Role / Team | користувач / роль / команда |
| Permission | дозвіл |
| Token | токен |
| Certificate | сертифікат |
| Authentication | автентифікація |
| Configuration | конфігурація |
| Settings | налаштування |
| Preferences | параметри |
| License | ліцензія |
| Entity, item (in lists and tables) | елемент |
| Column | стовпець |
| Filter (noun / verb) | фільтр / фільтрувати |
| Bulk actions | групові дії |
| Preview | попередній перегляд |
| Refresh | оновити |
| Create / Delete / Edit / Save / Cancel | створити / видалити / редагувати / зберегти / скасувати |
| Default | за замовчуванням |
| Preset (e.g. time range) | шаблон |
| Aggregation / Histogram | агрегація / гістограма |
| Slice / Slice by | зріз / розбити за |
| Backup | резервна копія |
| Backend (e.g. authentication backend) | бекенд |
| Spool directory | каталог спулінгу |
