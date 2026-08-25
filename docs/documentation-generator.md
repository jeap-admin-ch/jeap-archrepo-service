# Documentation generator

The generator renders the architecture model into a page tree in **Confluence**, refreshed by a scheduled job. It
is what makes the imported inventory readable for people who never call the API.

## What it generates

`DocumentationGenerator.generate(model)` walks the model and writes, per system, a system page with three index
pages under it - each holding the leaf pages of one kind:

```
root page
└── <Name> (System)
    ├── Komponenten (<Name>)
    │   └── <component name>
    ├── Events (<Name>)
    │   └── <event message type name>
    └── Commands (<Name>)
        └── <command message type name>
```

The three index pages carry no content of their own: they render a Confluence `children` macro that lists what is
below them.

The pages are rendered from Thymeleaf templates in `jeap-archrepo-docgen`
(`src/main/resources/template/documentation/`): `index.html`, `system.html`, `system-component.html`, `event.html`
and `command.html`, plus shared fragments under `fragment/`. All five start with the `generated-infoblock`
fragment, a Confluence info box stating that the page was generated automatically.

## Diagrams

Two different mechanisms, for two different kinds of diagram.

| Diagram                                                                                 | Rendered by                                                              | Ends up as                                                        |
| --------------------------------------------------------------------------------------- | ------------------------------------------------------------------------ | ----------------------------------------------------------------- |
| System and component context views, and the ER diagram of a component's database schema | `PlantUmlRenderer`, into PlantUML source                                 | A Confluence `plantuml` macro in the page - Confluence renders it |
| The reaction graphs imported from the reaction observer                                 | `GraphvizRenderer`, which pipes DOT into an external `dot -Tpng` process | A PNG attached to the page                                        |

So the Confluence instance needs a PlantUML macro available, and the container the instance runs in needs the
Graphviz `dot` binary on its `PATH`. See [Operations](operations.md).

Graph attachments are only regenerated when they changed: each `SystemGraph`, `ComponentGraph` and `MessageGraph`
carries a `fingerprint` from the reaction observer and the `lastPublishedFingerprint` of the last export, and the
attachment services compare the two before doing any work.

## Orphan cleanup

After the last page is written, the generator walks the whole tree below the root page and deletes every page it
did not just generate. A system that left the model therefore takes its pages with it - and any page a person
created under the root page is removed too. **The root page's subtree belongs to the generator.**

## When it runs

The `generateDocumentation()` job runs on `archrepo.documentation-generator.update-schedule`, guarded by ShedLock
so only one instance of a cluster generates. It can also be triggered with
`POST /api/jobs` and `{ "type": "GENERATE_DOC" }` - see [API](api.md).

The job **reads** the model; it never changes it. Generating and importing are therefore independent, and a failed
generation leaves the inventory intact.

## Confluence access

All Confluence I/O goes through the `ConfluenceAdapter` interface, which has two implementations:

| Implementation          | Selected when                                                             | Behaviour                                                    |
| ----------------------- | ------------------------------------------------------------------------- | ------------------------------------------------------------ |
| `ConfluenceAdapterImpl` | default                                                                   | Writes to Confluence through the Confluence publisher client |
| `ConfluenceAdapterMock` | `archrepo.documentation-generator.confluence.mock-confluence-client=true` | Writes nothing. Used by **all** tests                        |

Because the mock is what the test suite runs against, the real client path is not covered by the build and wants a
smoke test on a staging instance before a release.

### The root page is configured by id

The publisher client's Data Center variant looks a page up by title **within an ancestor**, so a root page cannot
be found by title alone. The root is therefore configured as
`archrepo.documentation-generator.confluence.root-page-id` - an id, not a name.

## Related

- [Architecture](architecture.md) - the model that is rendered
- [Data model](data-model.md) - the graph entities and their fingerprints
- [Configuration](configuration.md) - the generator's properties
- [Operations](operations.md) - what the runtime needs, and how to see the job is running
- [API](api.md) - triggering a generation run
