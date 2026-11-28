// The Halison Code website (GitHub Pages): https://halisonworks.github.io/mod-halisoncode/
// Pages written for the site are in src/content/docs; the guide, the server pages and the examples are copied in from
// docs/ and target/docs by scripts/sync.mjs, so they have one source.
import { defineConfig } from "astro/config";
import starlight from "@astrojs/starlight";

const repo = "https://github.com/halisonworks/mod-halisoncode";

export default defineConfig({
    site: "https://halisonworks.github.io",
    base: "/mod-halisoncode",
    integrations: [
        starlight({
            title: "Halison Code",
            description: "Program a robot in Minecraft with blocks, JavaScript or Python.",
            logo: { src: "./src/assets/logo.svg", alt: "Halison Code" },
            favicon: "/favicon.svg",
            customCss: ["./src/styles/theme.css"],
            defaultLocale: "root",
            locales: {
                root: { label: "English", lang: "en" },
                nb: { label: "Norsk bokmål", lang: "nb" },
            },
            social: [{ icon: "github", label: "GitHub", href: repo }],
            editLink: { baseUrl: `${repo}/edit/develop/site/` },
            sidebar: [
                { label: "Get started", translations: { nb: "Kom i gang" }, slug: "get-started" },
                { label: "Player guide", translations: { nb: "Veiledning" }, slug: "guide" },
                { label: "Inspiration", translations: { nb: "Inspirasjon" }, slug: "inspiration" },
                { label: "For teachers", translations: { nb: "For lærere" }, slug: "teachers" },
                {
                    label: "Servers",
                    translations: { nb: "Servere" },
                    items: [
                        { label: "Server guide", translations: { nb: "Serverveiledning" }, slug: "servers" },
                        { label: "Commands and game rules", translations: { nb: "Kommandoer og spilleregler" }, slug: "commands" },
                    ],
                },
                { label: "Download", translations: { nb: "Last ned" }, link: `${repo}/releases`, attrs: { target: "_blank" } },
            ],
        }),
    ],
});
