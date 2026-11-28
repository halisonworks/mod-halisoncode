# Halison Code: veiledning for spillere

English: [guide-en.md](guide-en.md)

Halison Code er en mod for Minecraft Java Edition. Du får en liten robot, Kintro, i verdenen din.
Du programmerer den i nettleseren med blokker, JavaScript eller Python.
Hver blokk du kjører, får roboten til å gjøre én ting i spillet.

Til og med versjon 0.0.5 het moden Codeling. Roboter, tingene deres, innstillinger og prosjekter i editoren derfra blir ikke med over: tøm robotene først, og kopier JavaScript-koden i programmene dine til en tekstfil (gamle prosjektfiler kan ikke importeres).

Halison Code er ikke laget av Microsoft, Mojang eller MakeCode-teamet. Se [Ansvarsfraskrivelse](#ansvarsfraskrivelse).

## Dette trenger du

- Minecraft Java Edition, versjon 1.21.1 til 1.21.11 eller 26.1 til 26.3, på en datamaskin (Windows, macOS eller Linux).
- En modlaster: Fabric Loader 0.20.1 eller nyere med Fabric API for Minecraft-versjonen din, eller den nyeste NeoForge for Minecraft-versjonen din (26.3.0.102 eller nyere for 26.3, 21.1.265 eller nyere for 1.21.1).
- Halison Code-filen for modlasteren og Minecraft-versjonen din (en `.jar`-fil). Navnet sier hvilken det er:

  | Minecraft | Fabric | NeoForge |
  |---|---|---|
  | 26.3 | `halisoncode-fabric-26.3-...jar` | `halisoncode-neoforge-26.3-...jar` |
  | 26.2 | `halisoncode-fabric-26.2-...jar` | `halisoncode-neoforge-26.2-...jar` |
  | 26.1 til 26.1.2 | `halisoncode-fabric-26.1.2-...jar` | `halisoncode-neoforge-26.1.2-...jar` |
  | 1.21.11 | `halisoncode-fabric-1.21.11-...jar` | `halisoncode-neoforge-1.21.11-...jar` |
  | 1.21.9 til 1.21.10 | `halisoncode-fabric-1.21.10-...jar` | `halisoncode-neoforge-1.21.10-...jar` |
  | 1.21.6 til 1.21.8 | `halisoncode-fabric-1.21.8-...jar` | `halisoncode-neoforge-1.21.8-...jar` |
  | 1.21.5 | `halisoncode-fabric-1.21.5-...jar` | `halisoncode-neoforge-1.21.5-...jar` |
  | 1.21.4 | `halisoncode-fabric-1.21.4-...jar` | `halisoncode-neoforge-1.21.4-...jar` |
  | 1.21.2 til 1.21.3 | `halisoncode-fabric-1.21.3-...jar` | `halisoncode-neoforge-1.21.3-...jar` |
  | 1.21.1 | `halisoncode-fabric-1.21.1-...jar` | `halisoncode-neoforge-1.21.1-...jar` |
- En nettleser (Chrome, Edge eller Firefox).

En forelder kan gjøre installasjonen. Den tar omtrent ti minutter. Stegene under sier 26.3; på en annen versjon velger du den i installasjonsprogrammene og bruker filene for den.

På eldre Minecraft-versjoner viser redigereren også blokker og gjenstander som kom senere (for eksempel blek eik). Et program som bruker en av dem, får feilen "minecraft:... is not in this Minecraft version". Alt annet virker likt.

## Installere

1. Lukk Minecraft.
2. Gå til https://fabricmc.net/use/installer/ og last ned Fabric-installasjonsprogrammet.
3. Kjør det. Velg "Client", Minecraft-versjon 26.3, og trykk "Install".
4. Åpne Minecraft Launcher én gang og start den nye profilen "fabric-loader-26.3". Lukk spillet igjen. Da lages mappa `mods`.
5. Last ned Fabric API for 26.3 fra https://modrinth.com/mod/fabric-api.
6. Legg `.jar`-filen til Fabric API og `.jar`-filen til Halison Code i mappa `mods`:
   - Windows: trykk Win+R, skriv `%appdata%\.minecraft\mods` og trykk Enter.
   - macOS: `~/Library/Application Support/minecraft/mods`
   - Linux: `~/.minecraft/mods`
7. Start Minecraft med profilen "fabric-loader-26.3".

### Installere på NeoForge

Bruk disse stegene i stedet hvis du spiller med NeoForge. NeoForge trenger ikke Fabric API.

1. Lukk Minecraft.
2. Gå til https://neoforged.net/ og last ned NeoForge-installasjonsprogrammet for Minecraft 26.3.
3. Kjør det. Velg "Install client" og trykk "Proceed".
4. Åpne Minecraft Launcher én gang og start den nye NeoForge-profilen. Lukk spillet igjen. Da lages mappa `mods`.
5. Legg filen `halisoncode-neoforge-26.3-...jar` i mappa `mods` (samme mapper som i steg 6 over).
6. Start Minecraft med NeoForge-profilen.

## Åpne editoren

1. Åpne en verden (enspiller, eller en server som har Halison Code, se [Flerspiller](#flerspiller)).
2. Trykk T for å åpne chatten. Skriv `/halisoncode` og trykk Enter.
3. Nettleseren åpner Halison Code-editoren. Lenken virker bare på denne maskinen, og bare til du lukker Minecraft.
4. Mens et program kjører, fortsetter spillet når du klikker i nettleseren. Åpner du pausemenyen (Escape), går spillet på pause og roboten venter: loggen under simulatoren sier "Minecraft is paused. Go back to the game and close the menu." Lukk menyen, så fortsetter roboten.

Editoren kan vise norsk. Robotblokkene har norske navn. Noen få andre tekster er fortsatt på engelsk.

## Lag roboten

Skriv `/robot spawn` i chatten. Roboten dukker opp ved føttene dine og ser samme vei som deg.
Du har én robot. Skriver du `/robot spawn` igjen, flyttes den til deg.
Hvis verdenen tillater flere, kan du også ha roboter med navn: se [Flere roboter](#flere-roboter).

## Ditt første program

Med blokker:

1. Åpne kategorien "Robot" (grønn) i editoren.
2. Dra "robot flytt fremover med 1" ut på arbeidsflaten. Endre 1 til 3.
3. Dra "robot snu venstre" under den. Endre venstre til høyre.
4. Trykk på Kjør-knappen (play) under simulatoren til venstre.
5. Se i spillet. Roboten går 3 blokker fremover og snur seg mot høyre.

Det samme programmet i JavaScript (fanen JavaScript øverst):

```
agent.move(SixDirection.Forward, 3)
agent.turn(TurnDirection.Right)
```

I Python har navnene understrek: `agent.move_to(...)`, `agent.find_nearest(...)`.

Hver kommando venter til roboten er ferdig. Så starter den neste.
For å stoppe et program trykker du på stoppknappen i editoren, eller skriver `/robot stop` i spillet.

## Eksempler

Startsiden i editoren (den første siden du ser) har raden "Eksempler" under "Mine prosjekter": åtte små programmer,
for eksempel en trapp, en tunnel, et rom med gjerde, en vaktpatrulje og fakler i mørket. Klikk på ett for å lese hva det lærer deg,
og trykk så på "Åpne eksempel" under Blokker, Python eller JavaScript. Da får du et nytt prosjekt med programmet i:
trykk på Kjør-knappen for å prøve det, og endre det som du vil. Selve eksempelet blir som det var.

## Vent og gjenta

"pause (ms)" i kategorien Løkker venter før neste kommando: 1000 ms er ett sekund. "gjenta for alltid" kjører koden
inni seg om og om igjen, så lenge programmet kjører. Denne roboten holder vakt og går 3 blokker fram og tilbake
hvert andre sekund:

```
loops.forever(function () {
    agent.move(SixDirection.Forward, 3)
    agent.turn(TurnDirection.Left)
    agent.turn(TurnDirection.Left)
    loops.pause(2000)
})
```

En pause holder bare igjen koden den står i. Kode i "kjør samtidig" og i andre "gjenta for alltid"-blokker fortsetter.
Et program med "gjenta for alltid" slutter aldri av seg selv: trykk på stoppknappen i editoren, eller skriv
`/robot stop` i spillet. Begge stopper det med en gang, også midt i en pause.

## Lag din egen funksjon

En funksjon er en bit av programmet med et navn du velger selv. Den kjører ikke bare fordi du har laget den: programmet
må også kjøre (kalle) den.

1. Åpne "Avansert" og så "Funksjoner", og trykk på "Lag en funksjon ...". Skriv et navn, for eksempel `buildWall`, og
   trykk på "Ferdig".
2. Dra robotblokker inn i "funksjon buildWall".
3. Åpne "Funksjoner" igjen og dra ut "kjør buildWall": inn i "ved start" (dra den ut av Løkker først) for å kjøre den når du
   trykker på Kjør, eller inn i "når chatkommando" (fra Spiller) for å kjøre den når du skriver i chatten i
   spillet.
4. Trykk på Kjør-knappen.

Denne funksjonen har et tall, `length` ("Ny parameter", "Tall"). Trykk på Kjør én gang. Skriv så `wall 5` i
chatten i spillet, og igjen så ofte du vil, med det tallet du vil:

```
function buildWall(length: number) {
    agent.move(SixDirection.Forward, length)
    agent.turn(TurnDirection.Left)
}
player.onChat("wall", function (num1, num2, num3) {
    buildWall(num1)
})
```

`wall` er nøkkelordet i chatten, ikke navnet på funksjonen: kallet inni "når chatkommando" kobler dem sammen. Programmet
må kjøre, med fanen åpen i nettleseren. Et stoppet program kan ikke startes fra spillet. Mer om chatkommandoer:
[Reager på chat og død](#reager-på-chat-og-død).

## Gå gjennom et program steg for steg

Knappen med insektet under simulatoren slår på feilsøkingsmodus. Programmet starter og stopper før sin første
kommando.

- Klikk på rundingen på en blokk (eller i feltet til venstre for et linjenummer i JavaScript og Python) for å sette et
  stoppunkt. Programmet stopper der hver gang det kommer dit.
- I Blokker kjører "Step" én blokk og stopper igjen (knappen har ikke norsk tekst). I JavaScript og Python kjører
  "Ta neste steg" én linje (et funksjonskall i ett steg), og "Steg inn" går inn i funksjonen.
- Pilen (Fortsett) kjører videre til neste stoppunkt, eller til slutten.
- Mens programmet står stille, er blokken eller linjen som kommer etterpå markert, og "Variabler" viser verdien til
  hver variabel.
- Skilpaddeknappen (Sakte film) kjører programmet sakte og markerer hver blokk mens den kjører, uten å stoppe.

En robotkommando blir alltid ferdig før programmet stopper. Roboten står stille mens programmet står stille, og det
gjør også "gjenta for alltid" og "kjør samtidig". For å stoppe programmet trykker du på "Avslutt feilsøking",
eller skriver `/robot stop` i spillet. Når du slår på feilsøkingsmodus eller Sakte film, starter programmet fra
begynnelsen, så roboten gjør kommandoene sine på nytt fra der den står.

## Feil, og å sjekke sitt eget program

Når spillet avviser en robotkommando (for eksempel en flytting på mer enn 64 blokker), kaster kommandoen en feil inn i
programmet ditt. Hvis ingenting fanger den, stopper programmet, robotloggen viser kommandoen og feilen, og en
"Robot error"-boks sier hva som gikk galt. I feilsøkingsmodus markerer editoren også blokken eller linjen den kom fra.

I JavaScript og Python kan programmet fange feilen og fortsette, for eksempel ved å gå hjem i stedet:

```
try {
    agent.move(SixDirection.Forward, 100)
} catch (e) {
    let error: string = e
    console.log("klarte ikke: " + error)
    agent.goHome()
}
```

```python
try:
    agent.move(SixDirection.FORWARD, 100)
except:
    print("klarte ikke å flytte")
    agent.go_home()
```

I JavaScript er feilen en tekst: koden, et kolon og meldingen, som `OUT_OF_RANGE: blocks must be 1 to 64`. Legg den
først i en `string`-variabel, som over, for å bruke den som tekst (for eksempel `error.indexOf("OUT_OF_RANGE") == 0`).
I Python skriver du bare `except:`. Python i editoren kan ikke gi feilen et navn (`except Exception as e` kompilerer
ikke), men robotloggen viser den. Blokker har ingen "prøv"-blokk, så å fange feil er for JavaScript og Python.

Feilkodene et program kan fange:

| Kode | Hvorfor |
|---|---|
| `BAD_ARGS` | Kommandoen eller en verdi er ugyldig, for eksempel en ting som ikke finnes i denne Minecraft-versjonen. |
| `NO_PERMISSION` | Du har ikke lov her, for eksempel "robot gi" uten kreativ modus, robotkameraet når serveren har slått det av, eller noe serveren ikke lar deg gjøre (meldingen sier hvilken rettighet det er). |
| `RATE_LIMITED` | Roboten har for mange kommandoer som venter. |
| `PROTECTED` | Satt av til beskyttede områder. I dag svarer spillet false i stedet (handlingen gjorde ingenting). |
| `UNLOADED` | Roboten eller blokken er i en del av verden som ikke er lastet, eller roboten er ikke laget. |
| `OUT_OF_RANGE` | Et tall er for stort eller for lite, for eksempel en flytting på mer enn 64 blokker eller en plass over 27. |
| `NO_ITEMS` | Roboten gikk tom for blokker mens den bygde (fyll, plasser, linje, former, labyrint eller byggmester), for eksempel `NO_ITEMS: I ran out of oak planks after 90 blocks.` Blokkene den satt, blir stående. Gi roboten flere og bygg videre. |

Disse avslutter programmet og kan ikke fanges: stoppknappen, `/robot stop`, "Avslutt feilsøking", tapt kontakt
med spillet, `STOPPED` (spillet stoppet roboten) og `VERSION` (editoren er eldre enn spillet; last den inn på nytt).

Programmet kan også sjekke sitt eget arbeid. "sjekk ... ellers stopp med" i kategorien Logikk stopper programmet med
meldingen din når betingelsen er usann, og "stopp med feil" stopper det med en gang. I JavaScript er de
`control.assert(betingelse, melding)` (blokken skriver den som `control.check`) og `control.fail(melding)`, og
`throw "melding"` gjør det samme. I Python virker `assert betingelse, "melding"` og `raise ValueError("melding")`.
Programmet stopper, og en "Program error"-boks viser meldingen. Feilsøkingsmodus markerer blokken eller linjen den kom
fra. En `try`/`catch` (eller `try`/`except`) rundt dem fanger dem som en robotfeil. Eksempelet "Rom og gjerde" sjekker
hvor glasslinjen ender.

## Alle robotkommandoene

Retningene gjelder fra robotens side: fremover, bakover, venstre, høyre, opp, ned.
Plassene er robotens 27 inventarplasser, nummerert fra 1 til 27.

### Bevegelse

| Blokk | JavaScript | Hva den gjør |
|---|---|---|
| robot flytt (retning) med (antall) | `agent.move(SixDirection.Forward, 3)` | Går 1 til 64 blokker. Stopper før hvis noe står i veien. |
| robot snu (venstre/høyre) | `agent.turn(TurnDirection.Left)` | Snur 90 grader. |
| robot tyngdekraft (på/av) | `agent.setGravity(true)` | På: roboten faller og går som en spiller. Av (slik den starter): den flyr. |
| robot gå til (posisjon) | `agent.moveTo(pos(0, 0, 5))` | Finner en vei og går dit. Svarer sann eller usann. Åpner tredører underveis. |
| robot sett hjem | `agent.setHome()` | Husker hvor roboten står, som hjemmet sitt. Også `/robot sethome`. |
| robot gå hjem | `agent.goHome()` | Går hjem som "gå til". Usann hvis den ikke har noe hjem eller ingen vei. |
| robot gå til robot (navn) | `agent.goToRobot(robots.get("Miner"))` | Går som "gå til" til en ledig blokk ved siden av roboten din Miner og snur seg mot den. Svarer sann eller usann. Se "Flere roboter". |
| robot følg sti (av blokk) | `agent.followPath()` | Går langs stien den står på, eller en sti av blokken du velger (`agent.followPath(Block.Gravel)`), også ett trinn opp eller ned, til stien slutter eller deler seg. En robot rett ved siden av stien går opp på den; står den lenger unna, svarer den slutt og sier "I am not on the path". Høyst 256 blokker. Svarer hvorfor den stoppet: slutt, veikryss, sperret eller grense (`PathStop.Junction`). I et veikryss stopper den vendt samme vei som den kom: snu den og følg stien igjen. |
| robot følg sti av (blokk) hold (venstre/høyre) | `agent.followPath(Block.OakPlanks, TurnDirection.Left)` | Holder én hånd på veggen, slik du kan gjøre i en ekte labyrint. I hvert veikryss tar den veien lengst til venstre (eller høyre). I en blindvei med vegg foran snur den og går tilbake. Den stopper der stien slutter uten vegg foran (veien ut: slutt), tilbake der den startet (slutt), eller etter 256 blokker (grense: følg stien igjen for å gå videre). Trykk + to ganger på blokken for å velge side. |

### Bygging

| Blokk | JavaScript | Hva den gjør |
|---|---|---|
| robot plasser (retning) | `agent.place(SixDirection.Down)` | Setter ut blokken fra valgt plass. |
| robot ødelegg (retning) | `agent.destroy(SixDirection.Forward)` | Knuser blokken. Det som faller ut, havner i roboten. |
| robot hjelp (plasser ved flytting) (på/av) | `agent.setAssist(AgentAssist.PlaceOnMove, true)` | Legger en blokk bak seg for hvert steg. |
| robot bruk (retning) | `agent.interact(SixDirection.Forward)` | Åpner dører, luker og grinder, trykker på knapper, slår spaker. |

### Undersøke

| Blokk | JavaScript | Hva den gjør |
|---|---|---|
| robot oppdag (blokk/rødstein) (retning) | `agent.detect(AgentDetection.Block, SixDirection.Forward)` | Sann hvis en blokk står i veien. |
| robot undersøk (blokk/data) (retning) | `agent.inspect(AgentInspection.Block, SixDirection.Down)` | Nummeret til blokken. |
| robot er åpen (retning) | `agent.isOpen(SixDirection.Forward)` | Sann hvis en dør, luke, grind eller tønne er åpen. |
| robot oppdag (fiendtlig skapning/dyr/spiller/gjenstand) (retning) | `agent.detectEntity(EntityKind.Mob, SixDirection.Forward)` | Sann hvis en slik er i blokken ved siden av roboten. |
| robot tell i nærheten (type) innen (radius) | `agent.countNearby(EntityKind.Animal, 4)` | Hvor mange som er nær roboten, høyst 8 blokker unna. |
| robot redstonestyrke (retning) | `agent.redstonePower(SixDirection.Forward)` | Rødsteinstyrke fra 0 til 15. |
| når roboten får redstonesignal | `agent.onRedstonePowered(function () { })` | Kjører koden sin hver gang blokken roboten står i, får et redstonesignal, for eksempel når du slår på en spak ved siden av den. Se "Reager på blokker, redstone og dagen" under. |
| robot posisjon | `agent.getPosition()` | Hvor roboten er. |
| robot retning | `agent.getOrientation()` | Hvilken vei den ser, i grader: sør 0, vest 90, nord -180, øst -90. |
| robot finn nærmeste (blokk) innen (radius) | `agent.findNearest(Block.OakLog, 16)` | Den nærmeste blokken av den typen, eller ingenting. |
| robot finn nærmeste (kategori) innen (radius) | `agent.findNearestCategory(BlockCategory.Logs, 16)` | Det samme for stammer, løv, malm, planker, ull eller blomster. |

### Sensorer

Radaren ser høyst 16 blokker rundt roboten. Usynlige spillere og spillere i tilskuermodus blir aldri funnet.

| Blokk | JavaScript | Hva den gjør |
|---|---|---|
| robot radar (type) innen (radius) | `agent.radar(EntityKind.Mob, 8)` | En liste over hvor de er, nærmeste først. Tom når det ikke er noen. Høyst de 64 nærmeste. |
| robot nærmeste (type) innen (radius) | `agent.nearestEntity(EntityKind.Player, 8)` | Hvor den nærmeste er, eller ingenting. |
| robot oppdag væske (retning) | `agent.detectLiquid(SixDirection.Down)` | Vann, lava eller ingen (`Liquid.Lava`). |
| robot er trygt (retning) | `agent.isSafe(SixDirection.Forward)` | Usann hvis blokken har lava i seg eller under seg, et fall på mer enn 3 blokker under seg, eller en fiendtlig skapning i seg eller rett ved siden av. En vegg er trygg, for roboten kan ikke gå dit. |
| robot lysnivå | `agent.lightLevel()` | Hvor lyst det er der roboten står: 0 (mørkt) til 15 (dagslys, eller en fakkel rett der). Lyset fra himmelen er svakere om natten og når det regner. Fiendtlige skapninger kan dukke opp der det er 0. |
| robot biom | `agent.biome()` | Biomet der roboten står, som en kort tekst: `"plains"`, `"desert"`. Biomer fra modder har med navnet på modden: `"mymod:crystal_cave"`. |
| robot temperatur | `agent.temperature()` | Hvor varmt biomet er: snøslette 0, slette 0,8, ørken 2. Under 0,15 snør det i stedet for å regne. |

### Måling

Roboten blir stående og ser bare. En posisjon eller et hjørne du gir, må være innenfor 32 blokker fra roboten, ellers stopper programmet.

| Blokk | JavaScript | Hva den gjør |
|---|---|---|
| robot mål fritt (retning) opptil (maks) | `agent.measure(SixDirection.Forward, 32)` | Hvor mange frie blokker det er før noe er i veien. |
| robot mål vegg (retning) langs (langs) opptil (maks) | `agent.measureRun(SixDirection.Forward, SixDirection.Up, 32)` | Hvor høy eller bred veggen ved siden av er. |
| robot mål åpning (akse) opptil (maks) | `agent.measureGap(Axis.X, 32)` | Hvor bred dalen eller gangen er der roboten står. |
| robot bakkehøyde ved (posisjon) | `agent.groundHeight(pos(0, 0, 5))` | Hvor høyt bakken er der. Løv teller ikke. |
| robot høydeprofil (retning) lengde (lengde) | `agent.heightProfile(SixDirection.Forward, 16)` | En liste med bakkehøyder langs en linje. |
| robot er området fritt fra (hjørne) til (hjørne) | `agent.isAreaFree(pos(1, 0, 1), pos(5, 4, 5))` | Sann hvis ingenting er i boksen (gress og snø går fint, en fakkel gjør ikke det). Høyst 32 blokker hver vei. |
| robot tell blokker fra (hjørne) til (hjørne) | `agent.countSolid(pos(1, 0, 1), pos(5, 4, 5))` | Hvor mange blokker som er i boksen: alt unntatt luft, gress, snø og vann, så fakler, blomster og skilt telles også. |
| robot finn ytterkanter fremover opptil (maks) | `agent.findBounds(32)` | De to ytre hjørnene av bygget foran, eller ingenting. |

### Inventar

| Blokk | JavaScript | Hva den gjør |
|---|---|---|
| robot velg plass (plass) | `agent.setSlot(1)` | Velger plassen den bygger fra. |
| robot antall i plass (plass) | `agent.getItemCount(1)` | Hvor mange ting som ligger der. |
| robot ledig i plass (plass) | `agent.getItemSpace(1)` | Hvor mange flere som får plass. |
| robot ledige plasser | `agent.freeSlots()` | Hvor mange av de 27 plassene som er tomme. |
| robot er inventaret fullt | `agent.isInventoryFull()` | Sann hvis ingen plass er tom. En stabel som ikke er full, kan fortsatt ta flere av samme ting. |
| robot gi (ting) antall (antall) i plass (plass) | `agent.setItem(Item.Stone, 64, 1)` | Gir roboten ting. Bare i kreativ modus, med spilleregelen `halisoncode:free_items`, eller når serveren lar deg. |
| robot samle alt | `agent.collectAll()` | Plukker opp ting rett ved siden av seg. En ting som nettopp ble sluppet, kan plukkes opp etter et øyeblikk (et halvt sekund for det roboten selv slapp), og aldri en som er holdt av til en annen spiller. |
| robot slipp (retning) fra plass (plass) antall (antall) | `agent.drop(SixDirection.Back, 1, 10)` | Slipper noen ting. |
| robot slipp alt (retning) | `agent.dropAll(SixDirection.Back)` | Slipper alt. |
| robot overfør antall (antall) fra plass (fra) til plass (til) | `agent.transfer(10, 1, 2)` | Flytter ting mellom sine egne plasser. |

### Beholdere

Kister, tønner, trakter, ovner og lignende blokker.

| Blokk | JavaScript | Hva den gjør |
|---|---|---|
| robot legg i beholder (retning) fra plass (plass) | `agent.storeItems(SixDirection.Forward, 0)` | Legger ting i beholderen. Plass 0 betyr alle. |
| robot ta fra beholder (retning) fra plass (plass) antall (antall) | `agent.takeItems(SixDirection.Forward, 1, 64)` | Tar fra én plass i beholderen. |
| robot ta fra (retning) (ting) antall (antall) | `agent.takeItem(SixDirection.Forward, Item.Coal, 16)` | Tar én type ting. |
| robot tell i beholder (retning) av (ting) | `agent.containerCount(SixDirection.Forward, Item.Coal)` | Hvor mange av tingen som ligger der. |
| robot beholderstørrelse (retning) | `agent.containerSize(SixDirection.Forward)` | Antall plasser, 0 hvis det ikke er en beholder. |
| robot sorter beholder (retning) | `agent.sortContainer(SixDirection.Forward)` | Rydder i kista. |

### Lage ting

| Blokk | JavaScript | Hva den gjør |
|---|---|---|
| robot lag (ting) antall (antall) | `agent.craft(Item.OakPlanks, 4)` | Lager ting av sine egne ting. Store oppskrifter trenger et arbeidsbord ved siden av roboten. |
| robot kan lage (ting) antall (antall) | `agent.canCraft(Item.Stick, 4)` | Sann hvis den kan lage det nå. |
| robot steinkutter lag (ting) antall (antall) | `agent.stonecut(Item.StoneBricks, 4)` | Bruker en steinkutter ved siden av roboten. |

### Tilbakemelding

| Blokk | JavaScript | Hva den gjør |
|---|---|---|
| robot si (tekst) | `agent.say("Hei")` | Viser tekst over roboten i 5 sekunder. Høyst 80 tegn. |
| robot kamera (modus) | `agent.camera(CameraMode.Follow)` | Setter robotkameraet til øyne, følg eller av (se Robotkamera under). Hvis serveren ikke tillater det, stopper programmet med en feil. |

### Spiller (kategorien Spiller)

Disse blokkene handler om deg, spilleren som kjører programmet. De virker uten robot. Ingen andres navn eller
posisjon kan leses, og ingen andre kan flyttes.

| Blokk | JavaScript | Hva den gjør |
|---|---|---|
| si i chatten (melding) | `player.say("Hei!")` | Viser meldingen i chatten din. Bare du ser den (i MakeCode ser alle den). Ren tekst, høyst 256 tegn. |
| spiller posisjon i verden | `player.position()` | Der du står: blokken føttene dine er i, som en posisjon i verden. |
| spiller retning | `player.getOrientation()` | Hvilken vei du ser, i grader: sør 0, vest 90, nord -180, øst -90. |
| spiller navn | `player.name()` | Spillernavnet ditt. |
| teleporter til (posisjon) | `player.teleport(pos(0, 10, 0))` | Flytter deg dit, i dimensjonen du er i. Bare i en enspillerverden (som verten) eller for en operatør på serveren; for alle andre nekter spillet med `NO_PERMISSION`. Utenfor verdensgrensen eller byggehøyden nekter det med `OUT_OF_RANGE`. |
| når chatkommando (nøkkelord) | `player.onChat("jump", function (num1, num2, num3) { })` | Kjører koden sin når du skriver en chattelinje som begynner med nøkkelordet, for eksempel `jump 10`. Se "Reager på chat og død" under. |
| når spilleren dør | `player.onDied(function () { })` | Kjører koden sin når du dør. |
| kjør chatkommando (tekst) | `player.runChatCommand("jump 10")` | Kjører din egen "når chatkommando"-kode for det nøkkelordet, som om du hadde skrevet det. Den sender ingenting til chatten eller spillet. |

Spillmodus betyr ikke noe for disse blokkene: i tilskuer- eller eventyrmodus kan du fortsatt bruke alle (teleporter
krever fortsatt enspiller, operatør eller rettigheten).

#### Reager på chat og død

"når chatkommando" og "når spilleren dør" lar programmet ditt vente på deg. Dette programmet teleporterer deg opp når
du skriver `jump 10` i chatten:

```
player.onChat("jump", function (num1, num2, num3) {
    player.teleport(pos(0, num1, 0))
})
```

- Nøkkelordet er ett ord: bokstaver, sifre, `-` og `_`, høyst 32 tegn, og store bokstaver teller (`Jump` er ikke
  `jump`). Nøkkelordet `*` får hver chattelinje du skriver. Et program kan lytte etter høyst 16 nøkkelord.
- Ordene etter nøkkelordet blir num1, num2 og num3: hele tall. Et ord som ikke er et tall, eller et som mangler,
  blir 0. `jump 10 abc` gir 10, 0 og 0.
- En linje som begynner med nøkkelordet ditt, vises ikke i chatten; den går bare til programmet ditt. Alle andre
  linjer sendes som vanlig. Linjer fra andre spillere når aldri programmet ditt, og programmet ditt ser aldri ordene
  i en linje, bare nøkkelordet og tallene.
- Har serveren en chatteutvidelse som tar chattelinjene først, skriver du `/halisoncode chat jump 10` i stedet.
- Et program med "når chatkommando" eller "når spilleren dør" fortsetter å kjøre etter siste linje, så det kan
  reagere. Trykk på stoppknappen i editoren, eller skriv `/robot stop`, for å avslutte det. Da vises chattelinjene
  dine som vanlig igjen.
- Høyst 16 chatkommandoer venter på programmet ditt; flere på en gang blir forkastet.

#### Reager på blokker, redstone og dagen

Tre blokker til lar programmet ditt vente på at noe skjer. Dette programmet sier fra hver gang du graver opp
diamantmalm, hver gang roboten får et signal, og hver morgen:

```
blocks.onBlockBroken(Block.DiamondOre, function () {
    player.say("Diamanter!")
})
agent.onRedstonePowered(function () {
    agent.move(SixDirection.Forward, 1)
})
gameplay.onDayStart(function () {
    player.say("God morgen")
})
```

- "når (blokk) blir ødelagt" kjører når du selv ødelegger en slik blokk. Blokker robotene dine ødelegger, og blokker
  andre spillere ødelegger, teller ikke. Et program kan lytte etter høyst 16 slags blokker.
- "når roboten får redstonesignal" kjører når blokken standardroboten din står i, går fra intet redstonesignal til et
  signal: for eksempel en spak, en knapp eller en redstoneblokk ved siden av roboten. Den kjører én gang per nytt
  signal, ikke igjen mens signalet varer. En robot som alt har signal når programmet starter, teller ikke. Roboter
  med navn har ikke denne blokken.
- "når en ny dag begynner" kjører når tiden på døgnet går rundt fra natt til morgen i verdenen din. Å sove gjennom
  natten teller, og det gjør også `/time set` til et tidligere tidspunkt på døgnet (`/time set day` om kvelden).
  `/time add` med et helt døgn gjør det ikke.
- Også disse programmene fortsetter å kjøre etter siste linje, til du trykker på stopp eller skriver `/robot stop`.
  Høyst 16 hendelser venter på programmet ditt; flere på en gang blir forkastet.

### Tid og vær (kategorien Vær og tid)

Disse blokkene leser verdenen du er i. De virker uten robot.

| Blokk | JavaScript | Hva den gjør |
|---|---|---|
| tid på døgnet | `gameplay.timeOfDay()` | Tiden i spill-tikk, 0 til 23999: 0 er soloppgang, 6000 middag, 12000 solnedgang, 18000 midnatt. Nether og End har tiden i Oververdenen. |
| regner det | `gameplay.isRaining()` | Sann når det regner eller tordner i dimensjonen din (snø teller også). Aldri i Nether eller End. Den sier ikke om regnet når ett bestemt sted: under et tak svarer den fortsatt sann. |
| når en ny dag begynner | `gameplay.onDayStart(function () { })` | Kjører koden sin når en ny dag begynner (tid 0, soloppgang). Se "Reager på blokker, redstone og dagen" over. |

Dette programmet jobber bare om dagen og stopper når natten kommer:

```
while (gameplay.timeOfDay() < 12000) {
    agent.move(SixDirection.Forward, 1)
}
```

### Signaler (kategorien Signaler)

Et signal er et navn, som `qr-forest` eller `lamp-red`. En annen mod på serveren kan sende et til deg, for eksempel
når du skanner en QR-kode ved en post i et museum eller når et sjekkpunkt, og programmet ditt kan sende et til de
andre modene, for eksempel for å slå på en lampe. En operatør kan sende et med `/halisoncode signal` (se
"Chattekommandoer" under), slik at en kommandoblokk eller et kart kan sende signaler uten noen annen mod.

| Blokk | JavaScript | Hva den gjør |
|---|---|---|
| når signal (navn) kommer | `signals.onReceived("qr-forest", function (value) { })` | Kjører koden sin når et signal med det navnet kommer til deg. `value` er tallet det har med, 0 når det ikke har noe. |
| send signal (navn) | `signals.send("lamp-red")` | Sender navnet til de andre modene på serveren. Robotloggen viser sann når en mod tok det, ellers usann og hvorfor. |

```
signals.onReceived("qr-forest", function (value) {
    agent.move(SixDirection.Forward, value)
})
signals.send("lamp-red")
```

- Et navn er ett ord av bokstaver, sifre, `-` og `_`, 1 til 32 tegn. Store og små bokstaver teller: `Lamp` og `lamp`
  er to navn. Et program kan lytte etter høyst 16 navn.
- Et signal kommer bare til ditt eget program, bare mens det kjører og har en "når signal"-blokk for det navnet.
  Signaler deler de 16 ventende hendelsene med chat, død, blokker, redstone og dagen.
- "send signal" sender bare navnet, aldri noe annet fra programmet ditt. Det går til de andre modene på serveren,
  aldri til en annen spillers program og aldri tilbake til ditt eget. Samme navn går høyst én gang i sekundet; et
  nytt før det svarer usann. På en server uten en mod som lytter, svarer det alltid usann.
- Et program med en "når signal"-blokk fortsetter å kjøre etter siste linje, til du trykker på stopp eller skriver
  `/robot stop`.

### Posisjoner (kategorien Posisjoner)

| Blokk | JavaScript | Hva den gjør |
|---|---|---|
| ~x ~y ~z | `pos(0, 0, 5)` | Et sted målt fra føttene dine. |
| ^x ^y ^z | `posLocal(0, 0, 5)` | Et sted målt fra der du ser (z er fremover). |
| verden x y z | `world(100, 64, 200)` | Et nøyaktig sted i verdenen. |
| velg tilfeldig posisjon fra (a) til (b) | `positions.random(pos(-5, 0, -5), pos(5, 0, 5))` | Et tilfeldig sted i boksen. |
| (posisjon) x verdi | `p.getValue(Axis.X)` | Ett av tallene i en posisjon. |
| (posisjon) i verden | `p.toWorld()` | Gjør `~` eller `^` om til et nøyaktig sted. |
| (posisjon) + (annen) | `p.add(other)` | Legger sammen to posisjoner. |
| (posisjon) = (annen) | `p.equals(other)` | Sann hvis de er like. |
| (posisjon) som tekst | `p.toString()` | Posisjonen som tekst. |
| (posisjon) avstand til (annen) | `p.distanceTo(other)` | Hvor mange blokker det er mellom dem i rett linje, med desimaler. En `~`- eller `^`-posisjon mot en annen type gjøres først om til et nøyaktig sted. |
| (posisjon) - (annen) | `p.subtract(other)` | Hvor langt det er fra den andre til denne, som en posisjon av samme type. |
| (posisjon) ganger (tall) | `p.scale(2)` | Hvert tall ganget opp, rundet av til hele blokker. |
| sorter (liste) etter avstand fra (posisjon) | `positions.sortByDistance(list, p)` | En ny liste med de samme posisjonene, nærmeste først. |
| nærmeste i (liste) til (posisjon) | `positions.nearest(list, p)` | Den nærmeste posisjonen i lista, eller `null` når lista er tom. |

### Bygge stort (kategoriene Blokker, Former og Bygger)

| Blokk | JavaScript | Hva den gjør |
|---|---|---|
| fyll med (blokk) fra (a) til (b) (hvordan) | `blocks.fill(Block.Stone, pos(1, 0, 1), pos(5, 3, 5), FillOperation.Hollow)` | Fyller boksen. Hul gir vegger med luft inni, omriss bare veggene, behold fyller bare luft. Luft tømmer boksen. |
| plasser (blokk) ved (posisjon) | `blocks.place(Block.Glass, pos(0, 2, 0))` | Plasserer én blokk. |
| når (blokk) blir ødelagt | `blocks.onBlockBroken(Block.DiamondOre, function () { })` | Kjører koden sin når du selv ødelegger en slik blokk. Se "Reager på blokker, redstone og dagen" over. |
| linje av (blokk) fra (a) til (b) | `shapes.line(Block.OakPlanks, pos(0, 0, 1), pos(5, 3, 1))` | En rett linje med blokker. |
| sirkel av (blokk) sentrum (s) radius (r) rundt (akse) (hvordan) | `shapes.circle(Block.Stone, pos(0, 0, 8), 5, Axis.Y, ShapeOperation.Outline)` | En sirkel; rundt Y ligger den flatt på bakken. |
| kule av (blokk) sentrum (s) radius (r) (hvordan) | `shapes.sphere(Block.Glass, pos(0, 6, 0), 4, ShapeOperation.Hollow)` | En kule. Hul gir en boble med luft inni. |
| labyrint ved (hjørne) bredde (b) lengde (l) vegger av (blokk) gulv av (blokk) (frø) | `shapes.maze(pos(2, 0, 2), 15, 15, Block.StoneBricks, Block.OakPlanks, 42)` | En labyrint med én vei mellom to steder, vegger 2 blokker høye, inngang på nordsiden og utgang på sørsiden. Hjørnet er i fothøyde, som hjørnet i en fill: veggene står i det laget, og gulvet erstatter bakken én blokk under. Bredde og lengde er 5 til 32; et partall bygger én blokk mindre. Samme frø gir alltid samme labyrint, så venner kan kappes gjennom labyrint 42; uten frø (trykk + på blokken for å legge til et) blir den ny hver gang. Gå gjennom den med `agent.followPath(Block.OakPlanks)`. |
| bygger flytt (retning) med (steg) | `builder.move(SixDirection.Forward, 4)` | Flytter en usynlig bygger. Den starter ved føttene dine og ser mot sør. |
| bygger snu, bygger vend mot, bygger teleporter til | `builder.turn(TurnDirection.Left)` | Snur eller flytter byggeren. |
| bygger sett merke | `builder.mark()` | Husker hvor byggeren er. |
| bygger linje fra merke med (blokk), bygger fyll fra merke med (blokk) (hvordan) | `builder.fill(Block.Stone, FillOperation.Hollow)` | Bygger fra merket til byggeren. |
| bygger plasser (blokk), bygger posisjon | `builder.place(Block.Torch)` | Plasserer en blokk der byggeren er, eller sier hvor den er. |

Roboten bygger alt dette, så den må være innenfor 32 blokker fra hver del. I overlevelse bruker den blokkene i inventaret sitt og stopper når de er brukt opp. Da sier den hvorfor, over hodet og i loggen under simulatoren (for eksempel "Jeg har ingen Stein i inventaret."; over roboten på spillets språk, i loggen på editorens), og kallet feiler med `NO_ITEMS`, som JavaScript og Python kan fange (se [Feil](#feil-og-å-sjekke-sitt-eget-program)); i kreativ modus, eller med spilleregelen `halisoncode:free_items`, trenger den ingen. Blokker som står i veien, blir ødelagt og havner i roboten.

Kategorien Konsoll har "si (tekst)" (`console.log("tekst")`). Den skriver i loggen under simulatoren.

### Løs en labyrint

Bygg en labyrint foran deg:

```
shapes.maze(pos(2, 0, 2), 15, 15, Block.StoneBricks, Block.OakPlanks, 42)
```

Gå inn i åpningen i nordveggen (inngangen), se sørover inn i labyrinten og skriv `/robot spawn`. Kjør så:

```
while (agent.followPath(Block.OakPlanks, TurnDirection.Left) == PathStop.Limit) {
}
agent.say("Jeg er ute!")
```

Roboten holder venstre hånd på veggen til den går ut av utgangen på sørsiden. I en stor labyrint er turen lengre enn 256 blokker, så løkka følger stien igjen hver gang den stopper ved grensen. Prøv `TurnDirection.Right`: hvilken vei er kortest i labyrint 42?

## Flere roboter

En verden kan la hver spiller ha mer enn én robot. Spillregelen `halisoncode:max_robots` sier hvor mange (1 til 16, og
roboten uten navn teller som én). Den er 1 fra start, så alt virker med én robot som før. En operatør skrur den opp,
for eksempel `/gamerule halisoncode:max_robots 4`. Skrur man den ned igjen, forsvinner ingen roboter; det kommer bare
ikke flere.

- `/robot spawn Miner` lager en robot som heter Miner, ved føttene dine. Et navn begynner med en bokstav og har høyst
  16 bokstaver, sifre, `_` eller `-`, også æ, ø og å (Bærer). Robotene dine har ulike navn (Miner og miner er samme
  navn).
- Den første roboten din, den uten navn, er fortsatt standardroboten din. I programmer heter den `agent`, så gamle
  programmer og lagrede verdener virker som før.
- Hver robot har sitt eget inventar, sin egen plass, sitt eget hjem, sin egen tyngdekraft og hjelp, og lagres med
  verdenen.
- `/robot list` viser robotene dine og hvor de er. `/robot remove Miner` fjerner en: alt den bærer, havner i
  inventaret ditt, og det som ikke får plass, faller ned ved føttene dine. Roboten må være lastet inn (nær deg).
  `/robot remove agent` fjerner standardroboten og stopper programmet ditt. Neste `/robot spawn` gir den tilbake med
  samme navn, utseende og farge.
- Chattekommandoene for én robot tar navnet til slutt: `/robot move forward 3 Miner`, `/robot come Miner`,
  `/robot inventory Miner`, `/robot camera Miner`. Uten navn gjelder de standardroboten din. I kommandoene heter den
  `agent`, og også det du kaller den: `/robot skin mint agent`. Trykk Tab for å se navnene på robotene dine.
- `/robot stop` stopper alle robotene og programmet ditt. `/robot stop Miner` stopper bare det Miner holder på med.

I et program er `robots.get("Miner")` roboten som heter Miner. Den kan alt `agent` kan:

```
let miner = robots.get("Miner")
control.runInParallel(function () {
    miner.move(SixDirection.Forward, 5)
    miner.destroy(SixDirection.Down)
})
agent.move(SixDirection.Back, 5)
```

Hver robot gjør én ting om gangen, og hver robot har sin egen kø av kommandoer. "kjør samtidig" lar koden inni den
kjøre samtidig med resten, så Miner og standardroboten flytter seg sammen. Uten den venter programmet på hver
kommando før den neste, som alltid.

I Blokker har kategorien Roboter "robot (navn)", "kjør samtidig" og de mest brukte robotblokkene: flytt, snu, gå til,
gå til robot, posisjon, plasser, ødelegg, samle alt og si. Legg "robot (navn)" i en variabel og bruk variabelen i de blokkene. De andre
robotkallene (for eksempel `miner.findNearest(Block.CoalOre, 16)`) finnes i JavaScript og Python. Robotblokkene for
`agent` er som før og gjelder standardroboten din.

### En funksjon for hver robot

Gi hver robot sin egen jobb med én funksjon per robot. Den første linjen i funksjonen velger roboten, og resten bruker
den variabelen:

```
function mine(length: number) {
    let miner = robots.get("Miner")
    for (let i = 0; i < length; i++) {
        miner.destroy(SixDirection.Forward)
        miner.move(SixDirection.Forward, 1)
    }
}
function build(length: number) {
    let builder = robots.get("Builder")
    for (let i = 0; i < length; i++) {
        builder.move(SixDirection.Forward, 1)
        builder.place(SixDirection.Back)
    }
}
player.onChat("mine", function (num1, num2, num3) {
    mine(num1)
})
player.onChat("build", function (num1, num2, num3) {
    build(num1)
})
player.onChat("both", function (num1, num2, num3) {
    control.runInParallel(function () {
        mine(num1)
    })
    build(num1)
})
```

1. La verdenen ha tre roboter (`/gamerule halisoncode:max_robots 3`, standardroboten din teller som én). Så
   `/robot spawn Miner` og `/robot spawn Builder`.
2. Legg blokker i Builder: høyreklikk på den, eller `/robot give stone 64 1 Builder` i kreativ modus. Den bygger med
   plassen den har valgt (`/robot slot 1 Builder`).
3. Lim inn programmet i JavaScript, gå tilbake til Blokker og trykk på Kjør.
4. Skriv `mine 5`, `build 5` eller `both 5` i chatten i spillet. Miner graver 5 blokker framover, og Builder går
   5 blokker og setter en blokk bak seg for hver. Med `both` jobber de samtidig.

Vil du lage det med blokker, se [Lag din egen funksjon](#lag-din-egen-funksjon). Legg "robot (navn)" i en variabel først i
funksjonen, og bruk den variabelen i blokkene fra Roboter.

### Gå til en annen robot

"gå til robot" sender en av robotene dine til en annen, det første steget i alt robotene gjør sammen:

```
let miner = robots.get("Miner")
agent.goToRobot(miner)
miner.goToRobot(robots.get(""))
```

Roboten går (eller flyr) på samme måte som "gå til" og stopper på en ledig blokk ved siden av den andre roboten,
snudd mot den. Ender den rett over eller under, beholder den retningen den hadde. Den svarer sann når den kom fram,
og usann når det ikke finnes noen vei, ingen ledig blokk ved siden av den andre roboten, eller den andre roboten
flyttet seg mens den gikk (spør da bare på nytt). En robot som skal gå til seg selv, svarer sann med en gang; en som
alt står ved siden av den andre, snur seg bare. `robots.get("")` er standardroboten din, `agent`, så den siste linjen
sender Miner til `agent`.

Bare dine egne roboter kan være målet: en annen spillers robot blir aldri funnet, selv med samme navn. Disse stopper
programmet med en feil som du kan fange (se "Feil, og å sjekke sitt eget program"):

- `BAD_ARGS`: du har ingen robot med det navnet. Meldingen viser robotene du har.
- `UNLOADED`: den andre roboten er ikke lastet (langt unna alle spillere).
- `OUT_OF_RANGE`: den andre roboten er i en annen dimensjon, eller så langt unna at veisøket gir opp.

"robot (navn) posisjon" gir hvor en robot er, til posisjonsblokkene: avstanden til den, `player.teleport` dit
(bare enspiller eller operatør), eller bygging ved siden av den.

### Navneskilt

En robot med navn viser det over hodet. Gi den nytt navn med `/robot rename Miner Digger`, eller med et navneskilt
fra en ambolt (høyreklikk på roboten med det). Bare eieren og operatører kan gi en robot nytt navn.
`/robot rename agent Bob` gir standardroboten din et navn. Da tar kommandoene både `agent` og `Bob`. I programmer heter
den fortsatt `agent`.

Ser du på en robot på nært hold, ser du også hvem som eier den. `/robot nametag off` skjuler navnene på alle robotene
dine, `/robot nametag on` viser dem igjen; legg til et navn for å endre bare én robot. Mens en robot snakker
(`agent.say`), tar snakkeboblen plassen til navneskiltet.

## Chattekommandoer

Alle kommandoer og spilleregler står også i [én liste](../commands.md) (på engelsk).

Et robotnavn til slutt i en kommando (vist som `[navn]`) velger en av robotene dine. Uten navn gjelder kommandoen
standardroboten din.

| Kommando | Hva den gjør |
|---|---|
| `/halisoncode` | Åpner editoren i nettleseren. |
| `/halisoncode chat <tekst>` | Sender en chattelinje til "når chatkommando"-koden i ditt eget program, slik det å skrive den i chatten ville gjort. Virker på servere der chatteutvidelser tar chattelinjene først. Sier fra når ingen av programmene dine lytter. |
| `/halisoncode signal <spillere> <navn> [tall]` | Sender et signal til programmene til spillerne du nevner (for eksempel `@a` eller et spillernavn), med tallet (0 uten tall). Bare for operatører, og for kommandoblokker. Den sier hvor mange programmer som fikk det, og nevner spillerne det ikke nådde. |
| `/halisoncode log on [minutter]` og `/halisoncode log off` | Utvidet logging i ditt eget spill i 15 minutter (eller 1 til 60). Se «Meld fra om et problem». |
| `/halisoncode report` | Skriver det en feilmelding trenger til `logs/halisoncode-report.txt` i mappen til spillet. |
| `/halisoncode server log on [minutter]`, `/halisoncode server log off` og `/halisoncode server report` | Det samme for serveren. Bare for operatører og serverkonsollen. |
| `/robot spawn [navn]` | Lager roboten din, eller flytter den til deg. Stopper et program som kjører. En ny robot med navn trenger plass under `halisoncode:max_robots`. |
| `/robot list` | Robotene dine og hvor de er. |
| `/robot remove <navn>` | Fjerner en robot. Tingene den har, får du. `agent` er standardroboten din. Fjerner du den, stopper også programmet ditt. |
| `/robot rename <navn> <nytt navn>` | Gir en robot nytt navn. `agent` er standardroboten din, og `/robot rename <nytt navn>` gir også den nytt navn. |
| `/robot nametag <on\|off> [navn]` | Viser eller skjuler navnet over robotene dine (uten navn: alle). |
| `/robot stop [navn]` | Uten navn: stopper alle robotene og programmet ditt. Med navn: stopper det den roboten holder på med. |
| `/robot status [navn]` | Hvor roboten er og hvilken vei den ser. |
| `/robot move <retning> <blokker> [navn]` | Går 1 til 64 blokker. `<retning>` er forward, back, left, right, up eller down. |
| `/robot turn <left\|right> [navn]` | Snur. |
| `/robot place <retning> [navn]` | Setter ut en blokk. |
| `/robot destroy <retning> [navn]` | Knuser en blokk. |
| `/robot inspect <retning> [navn]` | Viser nummeret til blokken. |
| `/robot detect <retning> [navn]` | Sann hvis en blokk står i veien. |
| `/robot slot <1-27> [navn]` | Velger en plass. |
| `/robot give <blokk> <antall> [plass] [navn]` | Gir ting, for eksempel `/robot give oak_planks 64`. Bare i kreativ modus, med spilleregelen `halisoncode:free_items`, eller når serveren lar deg. |
| `/robot inventory [navn]` | Åpner robotens inventar hvis du står nær. Ellers lister den tingene. |
| `/robot collect [navn]` | Plukker opp ting ved siden av seg. |
| `/robot drop <retning> [navn]` | Slipper alt. |
| `/robot assist <true\|false> [navn]` | Plasser ved flytting. |
| `/robot gravity <true\|false> [navn]` | Tyngdekraft på eller av. |
| `/robot sethome [navn]` | Husker hvor roboten står, som hjemmet sitt. |
| `/robot come [navn]` | Roboten går til deg. Finnes det ingen vei, hopper den til deg. Ikke fra en annen dimensjon: bruk `/robot spawn`. |
| `/robot camera [navn]` | Se med robotens øyne. Kjør den igjen, `/robot camera off` eller kameratasten tar deg tilbake til dine egne. Escape åpner pausemenyen som vanlig og lar kameraet stå på. |
| `/robot camera follow [navn]` | Følg roboten bakfra. Kjør den igjen, `/robot camera off` eller følgetasten tar deg tilbake til dine egne øyne. Escape åpner pausemenyen som vanlig og lar kameraet stå på. |
| `/robot stop all` | Stopper alle roboter. Bare for operatører. |

Kommandoene skrives på engelsk, også når editoren er på norsk.
Du kan også høyreklikke på roboten for å åpne inventaret.

### Robotkamera

`/robot camera` viser verden gjennom robotens øyne. Du kan også gi kameraet en tast: under Alternativer, Kontroller, Tastebindinger finner du "Robot camera" under Halison Code (den har ingen tast fra start). Samme tast, `/robot camera` eller `/robot camera off` tar deg tilbake. Escape åpner pausemenyen (så du kan bytte til editoren) og lar kameraet stå på.

Mens du ser gjennom roboten, står kroppen din stille der den er. Øverst til venstre står "Robot camera" og navnet på roboten. Kameraet virker bare når roboten er i samme dimensjon og så nær at spillet ditt har lastet den delen av verdenen. Det slutter av seg selv når roboten fjernes, kommer for langt unna eller bytter dimensjon.

`/robot camera follow` setter i stedet kameraet bak og over roboten, så du kan se på en lang byggejobb eller rute. Kameraet snur seg med roboten. Beveg musa for å se rundt roboten, og bruk musehjulet for å komme nærmere eller lenger unna (2 til 12 blokker). Står en vegg i veien, flytter kameraet seg foran den. "Robot camera: follow" i Tastebindinger gir det en egen tast. De samme reglene gjelder som for øyne-visningen.

## Robotens utseende

Roboten er én blokk høy og har tre egne skins. Bruk `/robot skin workshop`, `/robot skin industrial` eller `/robot skin mint`. Legg til et robotnavn etter skinet for å endre den roboten, for eksempel `/robot skin mint Miner` eller `/robot skin mint agent` (standardroboten din). Navn og skins består når du lagrer og når roboten bytter dimensjon.

Gåing og svinger til venstre og høyre animeres av seg selv. Bruk `/robot animation look_left`, `look_right`, `look_up`, `look_down` eller `wave` for en kort bevegelse. Legg til et robotnavn til slutt når det trengs. `idle` avbryter en bevegelse. Lampen er bare en visuell detalj. Lys i en bestemt retning er eget arbeid.

Bruk `/robot color 3366FF` for en egen hovedfarge på panelene, eller `/robot color reset` for å gå tilbake til fargene i det valgte skinet. Et robotnavn kan stå etter fargen. Metall, øyne, lampe og ledd beholder sine opprinnelige farger.

## Grenser

Grensene holder spillet raskt for alle.

- Én robot per spiller, med mindre spillregelen `halisoncode:max_robots` tillater flere (høyst 16).
- `flytt` går høyst 64 blokker per kommando. Hver blokk tar 4 spilltikk (0,2 sekunder).
- `finn nærmeste` og måleblokkene ser høyst 32 blokker ut fra roboten.
- Fyll og former bygger høyst 32 blokker ut fra roboten, høyst 32768 blokker om gangen, 256 blokker per tikk.
- `gå til` gir opp når veien er veldig lang eller ikke finnes, og svarer usann. `gå til robot` svarer
  `OUT_OF_RANGE` når veien er så lang.
- Roboten virker bare i deler av verdenen som er lastet inn (nær spillere).
- Antall er høyst 64. Robotplassene er 1 til 27. Kisteplassene er 1 til 54.
- `si` viser høyst 80 tegn, og spillerens `si i chatten` høyst 256.
- Spillet kjører høyst én spillerblokk per tikk (20 i sekundet) for deg; flere enn 16 som venter, nektes med `RATE_LIMITED`.
- Et program lytter etter høyst 16 nøkkelord for chatkommandoer, hvert på høyst 32 tegn, høyst 16 slags ødelagte blokker og høyst 16 signalnavn. Høyst 16 hendelser venter på det.
- "send signal" sender samme navn høyst én gang i sekundet.
- Tyngdekraften er av fra start, så roboten svever. Slå den på med "robot tyngdekraft" eller `/robot gravity true`.
- Programmet kjører i nettleseren din. Lukker du fanen eller forlater verdenen, stopper programmet. Etter 10 sekunder uten kontakt stopper spillet roboten.

## Flerspiller

Kjører du en server, les [veiledningen for serveradministratorer](../server-admin.md) (på engelsk).

- Serveren trenger Halison Code for sin modlaster (Fabric Loader og Fabric API, eller NeoForge). Alle som blir med på serveren, trenger samme modlaster og Halison Code også.
- Bruk samme Halison Code-versjon på serveren og hos alle spillerne. Å blande versjoner er ikke støttet og ikke testet.
- Bare du kan programmere roboten din. Ingen andre kan sende den kommandoer. Det gjelder roboter med navn også: et
  navn velger alltid bare en av dine egne roboter.
- `/gamerule halisoncode:max_robots <1-16>` bestemmer hvor mange roboter hver spiller kan ha (1, standard, er én robot
  uten navn).
- Bare du og operatører på serveren kan åpne inventaret til roboten din. Andre får beskjeden "This is not your robot" over hurtiglinjen.
- I overlevelse samler roboten sine egne ressurser. "robot gi" og `/robot give` virker bare i kreativ modus, eller når verdenen har `/gamerule halisoncode:free_items true`. Ellers legger du ting i roboten gjennom inventarskjermen dens.
- Operatører kan stoppe alle roboter med `/robot stop all`.
- `config/halisoncode-server.json` på serveren sier hvor mye roboter får gjøre hvert tikk (i spillet, ikke i programmet). Filen
  blir laget første gang en verden eller server med Halison Code kjører, og leses når den starter. Spillerne deler likt, uansett
  hvor mange roboter de har; på en travel server kan en robot vente litt før den fortsetter.
- Med en rettighetsmod, som LuckPerms, bestemmer serveren for hver spiller: `halisoncode.robot` (bruke en robot; alle fra start),
  `halisoncode.build` (plasser, fyll, former og labyrint; alle), `halisoncode.free_items` (ting og bygging gratis; kreativ modus
  eller spilleregelen) og `halisoncode.teleport` (spillerens teleport; operatører og verten i en enspillerverden). For eksempel
  `/lp user <spiller> permission set halisoncode.build false`. Uten `halisoncode.robot` er `/robot` en ukjent kommando.
- `player.say` i et program går bare til spilleren som kjører det, og `player.teleport` virker på en server bare for operatører, eller spillere serveren lar gjøre det.
- Et program hører bare chatkommandoene, døden og de ødelagte blokkene til sin egen spiller, redstonesignalet til sin
  egen robot og dagen i sin egen spillers verden, aldri noe om en annen spiller. En chattelinje holdes bare tilbake
  fra chatten mens programmet ditt kjører og lytter etter det første ordet i den.
- Et signal når bare programmet til spilleren det ble sendt til. "send signal" i et program går bare til de andre
  modene på serveren, aldri til en annen spiller, og har bare med navnet.
- `/gamerule halisoncode:pause_on_lost_focus false` gjør at spillet til den som kjører verdenen (enspiller, eller åpnet for
  LAN), ikke går på pause når vinduet mister fokus. Escape pauser fortsatt. `true`, som er standard, følger spillerens
  eget valg med F3+P.
- Spillregelen `halisoncode:robot_camera` bestemmer hvem som kan bruke robotkameraet: `owner` (standard: hver spiller gjennom sin egen robot), `ops` (bare operatører) eller `off` (ingen). For eksempel `/gamerule halisoncode:robot_camera off`. På NeoForge 26.3 viser skjermen Edit Game Rules regelen som et tekstfelt: skriv `off`, `owner` eller `ops` (på NeoForge 1.21.1 er den ikke med på skjermen; bruk kommandoen). Serveren sjekker dette, og kameraet slutter med en gang når regelen tar det bort.
- Roboten knuser og bruker ting som om den var deg. Hvis serveren beskytter et område, kan heller ikke roboten endre det.

## Feilsøking

Feilmeldingene er på engelsk.

| Problem | Hva du gjør |
|---|---|
| "Editor link expired" | Minecraft er startet på nytt siden du åpnet fanen. Skriv `/halisoncode` igjen. |
| "Not in a world" | Åpne en verden først, og trykk Kjør igjen. |
| "This server does not have Halison Code installed" | Serveren mangler moden. Spør den som eier serveren, eller spill i enspiller. |
| Roboten gjør ingenting mens du er i nettleseren, og loggen sier "Minecraft is paused" | Pausemenyen er åpen i spillet. Gå tilbake til spillet og lukk den; roboten fortsetter av seg selv. |
| Spillet går på pause når du klikker i nettleseren, før du trykker Kjør | Trykk F3+P i spillet (Fn+F3+P på noen bærbare maskiner). Da går ikke spillet på pause når du forlater vinduet. Trykk F3+P igjen for å slå det tilbake. Eller slå det av for denne verdenen med `/gamerule halisoncode:pause_on_lost_focus false` (operatører, så juks må være på). |
| "Kintro is not loaded. Go near it, or use /robot spawn to bring it to you" | Skriv `/robot spawn`, eller gå nærmere roboten. |
| "robot Miner is not loaded (or not spawned: /robot spawn Miner, see /robot list)" | Sjekk navnet med `/robot list`. Gå nærmere roboten, eller skriv `/robot spawn Miner`. |
| "This world allows one robot per player" | Roboter med navn trenger spillregelen `halisoncode:max_robots` over 1. Spør en operatør. |
| "The editor cannot start, because port 25580 is in use" | Et annet program bruker porten, ofte en annen Minecraft med Halison Code. Lukk det og skriv `/halisoncode` på nytt. Eller legg `-Dhalisoncode.port=25581` til Java-argumentene i launcheren, og start Minecraft på nytt. Editoren på en annen port har sine egne prosjekter. Slik finner du programmet på Windows: skriv `netstat -ano \| findstr 25580` i et kommandovindu. Det siste tallet er PID-en i Oppgavebehandling (Detaljer). På macOS og Linux skriver du `lsof -i :25580`. |
| `/halisoncode` er en ukjent kommando | Ditt eget spill har ikke Halison Code. Installer samme versjon som serveren, for samme modlaster og Minecraft-versjon. |
| Serveren avviser deg når du blir med, og nevner Halison Code | Ditt eget spill har ikke Halison Code. Installer det som i raden over. |
| En brannmur eller et antivirusprogram spør om Minecraft eller Java | Halison Code trenger det ikke. Editoren kan bare nås fra din egen datamaskin, så åpne aldri port 25580 i en brannmur eller ruter. Minecraft selv kan spørre når du åpner en verden for andre. |
| Programmene dine er borte | De lagres i én nettleser på én datamaskin. Åpne editoren i nettleseren du brukte før. Se [Spørsmål og svar](#spørsmål-og-svar). |
| Blokkene ser gamle ut etter en oppdatering | Trykk Ctrl+F5 i nettleseren. |
| "Program stopped" | `/robot stop`, `/robot spawn`, en lukket fane eller at du forlot verdenen stoppet det. Trykk Kjør igjen. |

## Spørsmål og svar

**Hvor kjører koden min?** I din egen nettleser, på din egen datamaskin. Spillet får bare kommandoene, for eksempel «flytt
fremover». På en server sjekker serveren hver kommando og flytter roboten. Programmet ditt sendes aldri til serveren.

**Hvem kan styre roboten min?** Bare du kan programmere den. Operatører på serveren kan åpne inventaret og stoppe alle
roboter. Se [Flerspiller](#flerspiller).

**Hvor lagres programmene mine?** I nettleseren din, på datamaskinen din, og ikke i verdenen. En annen nettleser, en annen
datamaskin eller et privat vindu har dem ikke. Sletter du nettleserens data for editoren, slettes de. Vil du ha en
kopi, trykker du på lagre-knappen ved siden av prosjektnavnet. Da får du en `.mkcd`-fil. «Hent inn» på editorens
hjemskjerm åpner den igjen.

**Virker det uten nett?** Ja, når Halison Code er installert. Editoren følger med moden og trenger ikke internett. Bare noen
lenker, for eksempel til vilkår og personvern, går til internett. En server på internett trenger nett som vanlig.

## Meld fra om et problem

1. Skriv `/halisoncode log on`. Da skriver Halison Code mer i spillets logg i 15 minutter. Det slår seg av selv.
2. Gjør det som gikk galt en gang til.
3. Skriv `/halisoncode report` og klikk «[copy the report]». Rapporten ligger også i `logs/halisoncode-report.txt`.
4. Les gjennom den. Halison Code utelater spillernavn, robotnavn, chat og det roboter sier der den kjenner dem. Fjern resten
   du ikke vil at andre skal se.
5. Lim den inn i [en feilmelding](https://github.com/halisonworks/mod-halisoncode/issues/new/choose). Det du skriver der, er offentlig.
   Er du barn, be en voksen om hjelp.

På en server andre eier, gjør eieren det samme med `/halisoncode server log on` og `/halisoncode server report`. Halison Code
sender aldri noe av seg selv.

## Ansvarsfraskrivelse

Halison Code er et fanprosjekt. Det har ingen tilknytning til og er ikke godkjent av Microsoft, Mojang eller Microsoft MakeCode-teamet.
Minecraft er et varemerke som tilhører Mojang Synergies AB (Microsoft).
Editoren er bygd med pxt, Microsofts verktøysett for MakeCode med åpen kildekode, som har MIT-lisens.
Halison Code har selv MIT-lisens. Detaljer og lisenstekster: [THIRD-PARTY-NOTICES.md](../../THIRD-PARTY-NOTICES.md) og [LICENSE](../../LICENSE).
