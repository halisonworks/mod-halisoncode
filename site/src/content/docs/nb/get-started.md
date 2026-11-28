---
title: Kom i gang
description: Installer Halison Code, lag roboten og kjør ditt første program.
---

Det tar omtrent et kvarter første gang. En forelder kan gjøre installeringen.

## 1. Dette trenger du

- Minecraft Java Edition, versjon 1.21.1 til 1.21.11 eller 26.1 til 26.3, på en datamaskin med Windows, macOS eller Linux.
- En modlaster: [Fabric](https://fabricmc.net/use/installer/) med [Fabric API](https://modrinth.com/mod/fabric-api),
  eller [NeoForge](https://neoforged.net/).
- Halison Code-fila (`.jar`) for modlasteren og Minecraft-versjonen din, fra
  [utgivelsene](https://github.com/halisonworks/mod-halisoncode/releases). Navnet sier hvilken det er, for eksempel
  `halisoncode-fabric-26.3-...jar`.
- En nettleser: Chrome, Edge eller Firefox.

## 2. Installer

1. Installer Fabric (eller NeoForge) for Minecraft-versjonen din, og start den nye profilen én gang så `mods`-mappa blir laget.
2. Legg Halison Code-fila i `mods`-mappa, og for Fabric også Fabric API-fila.
3. Start Minecraft med den nye profilen.

[Veiledningen](../guide/#installere) har hvert steg, med mappa for Windows, macOS og Linux.

## 3. Lag roboten og åpne editoren

1. Åpne en verden. Trykk T for å åpne chatten.
2. Skriv `/robot spawn` og trykk Enter. Kintro dukker opp ved føttene dine.
3. Skriv `/halisoncode` og trykk Enter. Editoren åpnes i nettleseren. Lenken virker bare på denne maskinen, og bare til
   du lukker Minecraft.

## 4. Ditt første program

1. Trykk **Nytt prosjekt** i editoren, og åpne den grønne kategorien **Robot**.
2. Dra **robot flytt fremover med 1** ut på arbeidsflaten, og endre 1 til 3.
3. Dra **robot snu venstre** under den, og endre venstre til høyre.
4. Trykk **Kjør** (avspillingsknappen under simulatoren). Se i spillet: roboten går 3 blokker og snur.

Det samme programmet i JavaScript:

```js
agent.move(SixDirection.Forward, 3)
agent.turn(TurnDirection.Right)
```

Hver kommando venter til roboten er ferdig, så starter den neste. For å stoppe et program trykker du stoppknappen i
editoren eller skriver `/robot stop` i spillet.

## Hva nå?

- Prøv programmene på siden [Inspirasjon](../inspiration/), og endre dem.
- Les om løkker, funksjoner og å gå gjennom et program steg for steg i [veiledningen](../guide/).
- Virker ikke noe? Se [Feilsøking](../guide/#feilsøking).
