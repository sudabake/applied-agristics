---
navigation:
  parent: index.md
  title: ME Biological Cultivator
  position: 60
  icon: appliedagristics:me_bio_cultivator
categories:
  - agr-machines
item_ids:
  - appliedagristics:me_bio_cultivator
  - appliedagristics:bio_sampler
  - appliedagristics:bio_seed
  - appliedagristics:culture_nutrient
  - appliedagristics:empty_bio_seed
---

# ME Biological Cultivator

<BlockImage id="appliedagristics:me_bio_cultivator" scale="5" />

This machine turns a mob sample into a biological seed you can plant.

## Inputs and output

| Supply one of each | Receive |
|---|---|
| Sampled <ItemLink id="appliedagristics:bio_sampler" />, <ItemLink id="appliedagristics:culture_nutrient" />, and <ItemLink id="appliedagristics:empty_bio_seed" /> | <ItemLink id="appliedagristics:bio_seed" /> of the sampled species |

Right-click a mob with the Biological Swab to take the sample. The Nutrient Reagent recipe returns the empty bucket.

## Automate the machine

From JEI, transfer the chosen species' recipe into an AE processing pattern. Use a Pattern Provider to submit the order. Results from ME crafting orders return to the network.

With manual inputs, enable automatic output from the side settings to send seeds to adjacent inventories. The base cycle is 10 seconds and the machine accepts four <ItemLink id="ae2:speed_card" /> upgrades.
