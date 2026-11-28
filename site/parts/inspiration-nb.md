---
title: Inspirasjon
description: Åtte små programmer fra startsiden i editoren å prøve og endre, med en idé til hvert.
---

Dette er eksemplene på startsiden i editoren. For å åpne et av dem skriver du `/halisoncode` i spillet, klikker på det
under **Eksempler** og trykker **Åpne eksempel**. Du får et eget prosjekt: trykk Kjør, og endre det som du vil.

:::note[Før du starter]
De fleste eksemplene gir roboten blokker med `agent.setItem`. Det krever kreativ modus eller spilleregelen
`halisoncode:free_items`. I overlevelse høyreklikker du på roboten og legger blokkene i den selv, og tar bort
linjene med `agent.setItem`. **To roboter** trenger også en robot som heter Miner: `/robot spawn Miner`.
:::

## Eksemplene

<!-- examples -->

## Endre det

- **Steinsti**: gjør den til en firkantet spiral som vokser for hver runde. Bytt ut 2 med en variabel som øker med 1.
- **Trapp**: bygg den ned i bakken i stedet, eller gjør hvert trinn to blokker bredt.
- **Tunnel**: gjør den lengre, og sett en fakkel for hver fjerde blokk med `agent.place`.
- **Søyle på en posisjon**: bygg en søyle der du står. Bruk `player.position()` i stedet for `world(0, 64, 3)`.
- **Rom og gjerde**: lag en dør i rommet. Bruk `blocks.fill` med luft for åpningen.
- **To roboter**: la Miner grave mens standardroboten din bygger, og la hver av dem si fra når den er ferdig.
- **Vaktpatrulje**: la vakten snu til venstre én gang i stedet for to, så den går i en firkant.
- **Fakler i mørket**: gå videre til det er en vegg foran, med en `mens`-løkke og `agent.detect`.

Flere ideer: [veiledningen](../guide/) viser løkker, funksjoner, former, labyrinter, chattekommandoer og flere roboter.
