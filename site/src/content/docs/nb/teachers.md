---
title: For lærere
description: Bruk Halison Code med en klasse eller en kodeklubb. Hva hver elev trenger, hvordan du setter opp en felles verden, og ideer til timer.
---

Med Halison Code programmerer barn i et spill mange av dem allerede spiller. De ser hver blokk de kjører skje i
verdenen, og en feil viser seg med en gang: roboten går inn i en vegg, eller tårnet får et hull.

## Dette trenger hver elev

- En datamaskin med Minecraft Java Edition (hver elev trenger sin egen Minecraft-konto), modlasteren og Halison
  Code-fila. Se [Kom i gang](../get-started/).
- En nettleser. Editoren kjører på elevens egen maskin og snakker bare med elevens eget spill: det er ingen kontoer,
  og prosjektene lagres i den nettleseren.

Elever kan dele en maskin. Roboten hører til den som er logget inn i Minecraft, men prosjektene ligger i nettleseren:
gi hver elev sin egen nettleserprofil, ellers ser de hverandres prosjekter.

## Én verden for hele klassen

Hver elev kan spille i sin egen verden. For å bygge sammen setter du opp en server med Halison Code:

1. Sett opp en Fabric- eller NeoForge-server med samme Halison Code-versjon som elevenes spill.
   [Serverveiledningen](../servers/) har stegene.
2. Bestem hva elevene kan gjøre med spilleregler (se [Kommandoer og spilleregler](../commands/)):
   - `halisoncode:free_items`: roboter får blokker gratis, så elevene kan bygge uten å samle først.
   - `halisoncode:max_robots`: hvor mange roboter hver elev kan ha (1 til 16).
   - `halisoncode:robot_camera`: hvem som kan se gjennom en robot (off, owner, ops).
3. Hver robot hører til én elev. Ingen andre kan programmere den eller ta tingene dens. En lærer som er operatør, kan
   stoppe alle robotene med `/robot stop all`.
4. Roboter følger spawn-beskyttelse, spillmoduser og claim-moder, som om de var eieren. I eventyrmodus setter og
   ødelegger de ingenting.

## Ideer til timer

Hver idé bygger på den forrige. Start med blokker; elever som er klare, kan bytte det samme prosjektet til JavaScript
eller Python.

| Time | Idé | Koding |
|---|---|---|
| 1 | Gå med roboten til et tre og tilbake | kommandoer etter hverandre |
| 2 | En steinsti, en trapp | gjenta-løkker |
| 3 | En tunnel: grav når det er en blokk i veien, ellers gå | mens-løkker og hvis |
| 4 | Fakler i mørket | sensorer (lysnivå) og betingelser |
| 5 | Et rom med dør, et gjerde rundt et jorde | egne funksjoner |
| 6 | To roboter som bygger samtidig | flere roboter, arbeid i parallell |
| 7 | En vakt som patruljerer for alltid, og en chattekommando som sender den hjem | for alltid, hendelser |

Programmene på siden [Inspirasjon](../inspiration/) passer til time 2 til 7, og hvert av dem har en idé til å endre det.

## Greit å vite

- Editoren viser hver kommando i robotloggen, og roboten sier over hodet hvorfor den stoppet (tom for blokker, ikke på
  stien). Feilsøkeren kan kjøre et program én blokk om gangen.
- Editoren, veiledningene og meldingene i spillet finnes på engelsk og norsk.
- Halison Code er en forhåndsversjon. Oppdater serveren og alle elevenes spill til samme versjon samtidig.
- Problemer og ideer: [meld dem på GitHub](https://github.com/halisonworks/mod-halisoncode/issues/new/choose).
