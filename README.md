# BlueMap-Towny

> *[BlueMap](https://github.com/BlueMap-Minecraft/BlueMap) addon for showing your [Towny](https://github.com/TownyAdvanced/Towny) towns on your beautiful map*


[![GitHub Total Downloads](https://img.shields.io/github/downloads/Chicken/BlueMap-Towny/total?label=Downloads&color=success "Click here to download the plugin")](https://github.com/Chicken/BlueMap-Towny/releases/latest)

Configuration, placeholders and main features heavily inspired by Dynmap-Towny.  
Has support for holes in claims and claims outside main town.  
Does not have support for external plugins hooking into placeholders.

<details>
<summary><span style="font-size:1.5em;">Images</span></summary>

![flat view of a town](.github/images/1.png)

![perspective view of a town](.github/images/2.png)

</details>

## Installation

Put the plugin jar file inside your plugins folder and have both Towny and BlueMap installed.

## Config

```yml
# BlueMap-Towny configuration
# https://github.com/Chicken/BlueMap-Towny#config

# Seconds between checks for marker updates
update-interval: 30
# Set by /n set mapcolor
dynamic-nation-colors: true
# Set by /t set mapcolor
dynamic-town-colors: true
# HTML for town popup, placeholders documented in README
popup: '<span style="font-size: 120%">%name% (%nation%)</span><br><span>Mayor <strong>%mayor%</strong></span><br><span>Residents <strong>%residents%</strong></span><br><span>Bank <strong>%bank%</strong></span>'

style:
  # Y-level to put markers at
  y-level: 62
  # Town border settings
  border-color: '#FF0000'
  border-opacity: 0.8
  border-width: 3
  # Town fill settings
  fill-color: '#FF0000'
  fill-opacity: 0.35
  # Path to icons on web or a link
  # Town home
  home-icon-enabled: false
  home-icon: assets/house.png
  # Nation capital
  capital-icon-enabled: false
  capital-icon: assets/king.png
```

### Popup placeholders

| Placeholder              | Content                          |
|--------------------------|----------------------------------|
| `%name%`                 | Town name                        |
| `%mayor%`                | Town mayor                       |
| `%residents%`            | Town residents                   |
| `%assistants%`           | Town assistants                  |
| `%residentdisplaynames%` | Town residents but display names |
| `%residentcount%`        | Number of town residents         |
| `%founded%`              | Town founding date               |
| `%board%`                | Town board text                  |
| `%trusted%`              | Town trusted residents           |
| `%tax%`                  | Town tax                         |
| `%bank%`                 | Town bank balance                |
| `%nation%`               | Nation of the town               |
| `%nationstatus%`         | Capital/Member of ...            |
| `%public%`               | Town publicity status            |
| `%peaceful%`             | Is the town peaceful             |
| `%flags%`                | Flags of the town (pvp, mobs...) |

## Building

`./gradlew clean build`

Output in `build/libs/`