# The Halison Code website

The site on GitHub Pages, https://halisonworks.github.io/mod-halisoncode/, made with
[Astro Starlight](https://starlight.astro.build/), in English and Norwegian.

```
cd site
npm ci
npm run dev      # http://localhost:4321/mod-halisoncode/
npm run build    # into site/dist
```

## Where the texts are

| Page | Source |
|---|---|
| Home, Get started, For teachers | `src/content/docs/` (English), `src/content/docs/nb/` (Norwegian) |
| Inspiration | `parts/inspiration-*.md`; the examples come from `target/docs/examples` and the editor's cards |
| Player guide | `docs/player/guide-en.md`, `docs/player/guide-nb.md` |
| Server guide, Commands and game rules | `docs/server-admin.md`, `docs/commands.md` (English only; the Norwegian site shows them in English) |

`scripts/sync.mjs` copies the pages from `docs/` and `target/docs` into the site before each build and rewrites their
links, so each text has one source. Edit those files where they are, not the copies (they are in `.gitignore`).

`.github/workflows/pages.yml` builds the site for a pull request that touches it, and publishes it from `main`, so the
guide on the site matches the released mod.
