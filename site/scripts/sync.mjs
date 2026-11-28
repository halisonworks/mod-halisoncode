// Copies the pages that have their source elsewhere in the repo into the site, so each text is written once:
// the player guides (docs/player), the server pages (docs/server-admin.md, docs/commands.md) and the examples of the
// editor's home screen (target/docs). Run by `npm run build` and `npm run dev`; the copies are in .gitignore.
import { copyFileSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const site = join(dirname(fileURLToPath(import.meta.url)), "..");
const repo = join(site, "..");
const docs = join(site, "src", "content", "docs");
const base = "/mod-halisoncode";
const github = "https://github.com/halisonworks/mod-halisoncode";

const read = path => readFileSync(join(repo, path), "utf8").replace(/\r\n/g, "\n");
const write = (path, text) => {
    mkdirSync(dirname(join(docs, path)), { recursive: true });
    writeFileSync(join(docs, path), text);
};

// Where a link in the repo's Markdown goes on the site. Files that are not pages go to GitHub.
const pages = {
    "player/guide-en.md": `${base}/guide/`,
    "player/guide-nb.md": `${base}/nb/guide/`,
    "server-admin.md": `${base}/servers/`,
    "commands.md": `${base}/commands/`,
};

function links(text, from) {
    return text.replace(/\]\(([^)\s#]+\.md|[^)\s#:]*(?:LICENSE|NOTICES\.md))(#[^)]*)?\)/g, (all, target, hash = "") => {
        const path = new URL(target, `file:///repo/${from}`).pathname.slice("/repo/".length);
        const page = pages[path.replace(/^docs\//, "")];
        return `](${page ? page + hash : `${github}/blob/develop/${path}${hash}`})`;
    });
}

// A repo page as a site page: the first heading becomes the title, and the language links at the top go (the site has
// its own language picker).
function page(path, { title, description, drop = [] }) {
    let text = read(path);
    const heading = text.match(/^# (.+)\n/);
    text = text.slice(heading.index + heading[0].length);
    for (const line of drop) text = text.replace(line, "");
    text = links(text, path).replace(/^\n+/, "");
    const source = `${github}/blob/develop/${path}`;
    return `---\ntitle: ${JSON.stringify(title ?? heading[1])}\ndescription: ${JSON.stringify(description)}\neditUrl: ${source.replace("/blob/", "/edit/")}\n---\n\n${text}`;
}

write("guide.md", page("docs/player/guide-en.md", {
    title: "Player guide",
    description: "Everything about Halison Code for players: install, the editor, every robot command, and help.",
    drop: ["Norsk: [guide-nb.md](guide-nb.md)\n"],
}));
write("nb/guide.md", page("docs/player/guide-nb.md", {
    title: "Veiledning for spillere",
    description: "Alt om Halison Code for spillere: installering, editoren, alle robotkommandoene og hjelp.",
    drop: ["English: [guide-en.md](guide-en.md)\n"],
}));
write("servers.md", page("docs/server-admin.md", {
    title: "Server guide",
    description: "Run a Minecraft server or a class world with Halison Code: install, game rules, permissions and protection.",
}));
write("commands.md", page("docs/commands.md", {
    description: "Every /robot and /halisoncode command and every game rule of Halison Code.",
}));

// Inspiration: the editor's examples, in the order of its home screen, with their pictures and programs.
mkdirSync(join(site, "public", "examples"), { recursive: true });
for (const file of readdirSync(join(repo, "target/docs/static/examples"))) {
    copyFileSync(join(repo, "target/docs/static/examples", file), join(site, "public", "examples", file));
}
mkdirSync(join(site, "src", "assets"), { recursive: true });
copyFileSync(join(repo, "target/docs/static/logo.svg"), join(site, "src", "assets", "logo.svg"));
copyFileSync(join(repo, "target/docs/static/logo.svg"), join(site, "public", "favicon.svg"));

function cards(path) {
    return JSON.parse(read(path).match(/```codecard\n([\s\S]*?)\n```/)[1]);
}

function inspiration(lang, cardsPath) {
    const parts = read(`site/parts/inspiration-${lang}.md`).split("<!-- examples -->");
    const examples = cards(cardsPath).map(card => {
        const name = card.url.split("/").pop();
        const program = read(`target/docs/examples/${name}.md`).match(/```blocks\n([\s\S]*?)\n```/)[1];
        return `### ${card.name}\n\n<img src="${base}${card.imageUrl.replace("/static", "")}" alt="" width="160">\n\n${card.description}\n\n\`\`\`js\n${program}\n\`\`\`\n`;
    }).join("\n");
    return parts[0] + examples + (parts[1] ?? "");
}
write("inspiration.md", inspiration("en", "target/docs/examples.md"));
write("nb/inspiration.md", inspiration("nb", "target/docs/_locales/nb/examples.md"));
console.log(`sync: guide, servers, commands and ${cards("target/docs/examples.md").length} examples copied`);
