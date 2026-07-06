# Craft Level — Wiki

Mod de progressão RPG para Minecraft 1.20.1 (Forge). Jogadores ganham **LP (Life Points)** ao matar mobs, sobem de **level** e escalam stats, dano e cura via JSON. Mobs recebem level dinâmico e stats proporcionais.

---

## Índice

1. [Visão geral](#visão-geral)
2. [Como o mod funciona](#como-o-mod-funciona)
3. [Arquivos de configuração](#arquivos-de-configuração)
4. [Configuração do cliente (`client.json`)](#configuração-do-cliente-clientjson)
5. [Configuração geral (`config.json`)](#configuração-geral-configjson)
6. [Wiring de stats (`player_stats`, `mob_stats`, `mob_overrides`)](#wiring-de-stats-player_stats-mob_stats-mob_overrides)
7. [Níveis de mobs (`mob_levels.json`)](#níveis-de-mobs-mob_levelsjson)
8. [Escaladores (Scalers)](#escaladores-scalers)
9. [Modos de scaling (`scaling_mode`)](#modos-de-scaling-scaling_mode)
10. [Modos de aplicação (`apply_mode`)](#modos-de-aplicação-apply_mode)
11. [`include_modifiers`](#include_modifiers)
12. [Combate: hurt, hurt_self e heal](#combate-hurt-hurt_self-e-heal)
13. [Comandos admin](#comandos-admin)
14. [Editor in-game](#editor-in-game)
15. [Debug](#debug)

---

## Visão geral

| Conceito | Descrição |
|----------|-----------|
| **Level (player)** | Progressão do jogador (1 → `lvl_max`). Persiste no save. |
| **LP** | Pontos ganhos ao matar mobs. Quando LP ≥ LP para próximo level, sobe de level. |
| **Level (mob)** | Nível numérico por mob, calculado no spawn/entrada no mundo. |
| **Escalador** | Fórmula reutilizável (`health`, `mob_health`, `melee_power`, etc.). |
| **Wiring** | Mapa que liga attributes / tipos de dano / cura → nome do escalador. |

Configs ficam em `config/craftlevel/` (servidor e singleplayer). O cliente tem `client.json` só para HUD.

---

## Como o mod funciona

### Progressão do jogador

1. Jogador começa em **level 1** com **0 LP**.
2. Ao matar um mob (morto por jogador), ganha LP = `max(1, maxHealth do mob)`.
3. Se `LP ≥ LP para próximo level` e `level < lvl_max`, consome LP e sobe de level.
4. **LP para próximo level** usa o bloco `lp_scaling` de `config.json` (ver [LP scaling](#lp_scaling-progressão)).

Stats de attributes (vida, etc.) são reaplicados em login, respawn, level up, troca de equipamento (se configurado) e `/craftlevel reload`.

### Level dos mobs

1. Quando um mob entra no mundo, o mod verifica a capability `mob_level`.
2. Se **level ≤ 0**: calcula level com base no **jogador mais próximo** (128 blocos), aplica variação aleatória e limites de `mob_levels.json`.
3. Se **level > 0** (chunk reload, mob já inicializado): só reaplica stats.

```
level_base = level do player mais próximo (ou 1 se ninguém perto)
level = clamp(level_base + random(-variação, +variação), lvl_min, lvl_max)
```

Isso cobre spawn natural, summons, bosses de mods (ex.: Dead King acordando) e reload de chunk.

### Dois caminhos de scaling

| Caminho | Quando | Onde |
|---------|--------|------|
| **Attributes** | Login, spawn, level up, equipamento, reload | `StatRefreshService` → modifiers permanentes em attributes vanilla |
| **Eventos de combate** | Cada hit / cura | `EventScalingService` → `LivingHurtEvent`, `LivingHealEvent` |

Attributes usam `attribute` no wiring. Dano e cura usam `hurt`, `hurt_self` e `heal`.

### HUD (cliente)

- Círculo de level + barra de LP do jogador.
- Barras opcionais de vida e fome customizadas.
- Barra de HP + level acima dos mobs (cor verde/branco/vermelho vs level do player).
- Números flutuantes de dano ao acertar (servidor → cliente).

### Armadura (mixin)

Players e mobs perdem **1 ponto de durabilidade por peça** ao tomar dano que desgasta armadura (em vez do vanilla: dano ÷ 4).

---

## Arquivos de configuração

| Arquivo | Escopo | Descrição |
|---------|--------|-----------|
| `config.json` | Servidor | Debug, LP, escaladores globais |
| `client.json` | Cliente | Posição/escala do HUD |
| `player_stats.json` | Servidor | Wiring dos jogadores |
| `mob_stats.json` | Servidor | Wiring padrão de todos os mobs |
| `mob_overrides.json` | Servidor | Wiring por entity type (substitui `mob_stats`) |
| `mob_levels.json` | Servidor | `lvl_min` / `lvl_max` por mob |

Recarregar sem reiniciar: `/craftlevel reload` (OP 2).

---

## Configuração do cliente (`client.json`)

Arquivo: `config/craftlevel/client.json`  
Editável in-game em **Mods → Craft Level → Config → Client**.

### Estrutura

```json
{
  "hud": {
    "lp_bar": { ... },
    "level_circle": { ... },
    "health_bar": { ... },
    "hunger_bar": { ... }
  }
}
```

### Campos comuns (`lp_bar`, `level_circle`)

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `anchor` | string | Ancora na tela (ver tabela abaixo) |
| `x` | int | Offset X em pixels a partir da ancora |
| `y` | int | Offset Y em pixels a partir da ancora |
| `scale` | float | Escala do elemento gráfico |
| `font_scale` | float | Escala do texto |

### `health_bar` / `hunger_bar` (campos extras)

| Campo | Descrição |
|-------|-----------|
| `enabled` | `true` / `false` — usa barra custom ou vanilla |
| `armor_margin` | (health) Distância da barra de armadura vanilla |
| `breath_margin` | (hunger) Distância da barra de fôlego vanilla |

### Valores de `anchor`

`top_left`, `top_center`, `top_right`, `center_left`, `center_right`, `bottom_left`, `bottom_center`, `bottom_right`

Positivo em `x`/`y` move para direita/baixo a partir da ancora (conforme implementação do layout).

---

## Configuração geral (`config.json`)

### `debug`

```json
"debug": true
```

Ativa logs detalhados no console (`DebugLog`) para spawn, scaling, hurt/heal, reload, etc.

### `lp_scaling` (progressão)

```json
"lp_scaling": {
  "mob_level_variation": 10,
  "lvl_max": 200,
  "base_value": 200.0,
  "scaling": 1.06,
  "scaling_mode": "power"
}
```

| Campo | Descrição |
|-------|-----------|
| `mob_level_variation` | Variação aleatória ±N no level de mobs spawnados |
| `lvl_max` | Level máximo do jogador (e teto do comando `/craftlevel level set`) |
| `base_value` | Base da fórmula de LP necessário por level |
| `scaling` | Fator da fórmula (depende do `scaling_mode`) |
| `scaling_mode` | `power`, `linear` ou `product` |

**LP para subir do level L** = valor escalado de `lp_scaling` no level L (inteiro truncado).  
Exemplo com defaults (`base=200`, `scaling=1.06`, `power`):

| Level | LP necessário |
|-------|----------------|
| 1 | 200 |
| 2 | 212 |
| 10 | ~358 |
| 50 | ~3687 |

### `scaling` (escaladores globais)

Bloco de escaladores reutilizáveis referenciados pelo wiring:

```json
"scaling": {
  "health": {
    "base_value": 1.0,
    "scaling": 1.045,
    "scaling_mode": "power",
    "apply_mode": "multiply",
    "include_modifiers": true
  },
  "mob_health": {
    "base_value": 1.0,
    "scaling": 1.05,
    "scaling_mode": "power",
    "apply_mode": "multiply",
    "include_modifiers": false
  }
}
```

Cada escalador aceita os campos da tabela abaixo (ver [Escaladores](#escaladores-scalers)).

Escaladores default incluídos: `spell_power`, `mob_health`, `flat`, `health`, `melee_power`, `none`.

---

## Wiring de stats (`player_stats`, `mob_stats`, `mob_overrides`)

Wiring define **o que** escalar, não **como** (isso fica em `scaling`).

### Estrutura

```json
{
  "attribute": {
    "minecraft:generic.max_health": "health"
  },
  "hurt": {
    "minecraft:player_attack": "melee_power",
    "*irons_spellbooks*": "spell_power",
    "default": "health"
  },
  "hurt_self": {
    "default": "health"
  },
  "heal": {
    "default": "health"
  }
}
```

### Seções

| Seção | Chave | Valor | Efeito |
|-------|-------|-------|--------|
| `attribute` | ID do attribute (ex. `minecraft:generic.max_health`) | Nome do escalador | Modifier permanente no attribute |
| `hurt` | ID do damage type ou wildcard | Nome do escalador | Multiplica dano **causado** pela entidade |
| `hurt_self` | ID do damage type, wildcard ou `default` | Nome do escalador | Multiplica dano **recebido** (regras abaixo) |
| `heal` | Chave (geralmente `default`) | Nome do escalador | Multiplica cura recebida |

### `player_stats.json`

Wiring aplicado a **todos os jogadores**.

### `mob_stats.json`

Wiring **padrão** de mobs quando não há override.

### `mob_overrides.json`

Wiring **por entity type** — substitui completamente `mob_stats` para aquele mob:

```json
{
  "irons_spellbooks:dead_king": {
    "attribute": { "minecraft:generic.max_health": "mob_health" },
    "hurt": { "default": "mob_health" },
    "hurt_self": {
      "default": "mob_health",
      "attributeslib:current_hp_damage": "none"
    },
    "heal": { "default": "mob_health" }
  }
}
```

Útil para bosses com regras especiais (ex.: anular `% HP` do AttributesLib com escalador `none`).

### Wildcards em `hurt` / `hurt_self`

Chaves com `*` fazem match se o damage type **contém** o texto entre asteriscos:

```json
"*irons_spellbooks*": "spell_power"
```

Combina `irons_spellbooks:fireball`, etc. Em empate, vence o wildcard **mais longo**.

### Chave `default`

Usada quando nenhuma chave exata ou wildcard combina. Em `hurt_self`, `default` tem papel especial (ver [Combate](#combate-hurt-hurt_self-e-heal)).

---

## Níveis de mobs (`mob_levels.json`)

Define limites por entity type:

```json
{
  "minecraft:zombie": {
    "lvl_min": 1,
    "lvl_max": 200
  },
  "irons_spellbooks:dead_king": {
    "lvl_min": 100,
    "lvl_max": 200
  }
}
```

| Campo | Default se omitido |
|-------|---------------------|
| `lvl_min` | 1 |
| `lvl_max` | 200 |

O level calculado é sempre clampado entre `lvl_min` e `lvl_max`.  
Mobs passivos (allay, vaca, etc.) podem ficar fixos em `lvl_min = lvl_max = 1`.

---

## Escaladores (Scalers)

Todo escalador em `config.json → scaling.{nome}`:

| Campo | Tipo | Default | Descrição |
|-------|------|---------|-----------|
| `base_value` | number | 1.0 | Valor base da fórmula |
| `scaling` | number | 1.0 | Fator/multiplicador da fórmula |
| `scaling_mode` | string | `power` | Como level entra na fórmula |
| `apply_mode` | string | `multiply` | Como o resultado é aplicado |
| `include_modifiers` | bool | `false` | Só para **attributes** — ver seção dedicada |

**Level usado na fórmula:** sempre `level - 1` (mínimo 0). Level 1 → fator 0 → resultado = base (em power/product).

Escalador especial **`none`**: `base_value=0`, `apply_mode=overwrite` — zera/neutraliza o efeito wired.

---

## Modos de scaling (`scaling_mode`)

Define **como calcular o valor escalado** a partir de `base_value`, `scaling` e level.

Seja `n = max(0, level - 1)`.

### `power` (padrão)

```
valor = base_value × scaling^n
```

Crescimento exponencial. Bom para vida, dano e LP.

| level | base=1, scaling=1.045 | base=200, scaling=1.06 (LP) |
|-------|------------------------|-----------------------------|
| 1 | 1.000 | 200 |
| 10 | 1.485 | 358 |
| 50 | 5.840 | 3687 |

### `linear`

```
valor = base_value + scaling × n
```

Crescimento constante por level.

| level | base=1, scaling=0.5 |
|-------|---------------------|
| 1 | 1.0 |
| 10 | 5.5 |
| 50 | 25.5 |

### `product`

```
valor = base_value × scaling × n
```

Linear multiplicado por `base_value`. Em level 1 (`n=0`) o resultado é **0**.

| level | base=1, scaling=0.1 |
|-------|---------------------|
| 1 | 0 |
| 10 | 0.9 |
| 50 | 4.9 |

Útil quando o efeito só deve existir a partir do level 2+.

---

## Modos de aplicação (`apply_mode`)

Define **como o valor escalado combina** com o valor atual. Comportamento difere entre **attributes** e **eventos** (hurt/heal).

### Em eventos (`hurt`, `hurt_self`, `heal`)

Fórmula direta sobre o `amount` do evento:

| Modo | Fórmula | Exemplo (amount=10, escalado=1.5) |
|------|---------|-----------------------------------|
| `multiply` | `amount × escalado` | 15 |
| `add` | `amount + escalado` | 11.5 |
| `overwrite` | `escalado` (ignora amount) | 1.5 |

### Em attributes (`attribute` wiring)

O mod calcula um **AttributeModifier** vanilla:

#### Com `include_modifiers: false` (padrão)

Referência = **base value** do attribute (sem equipamento).

| Modo | Efeito na base |
|------|----------------|
| `multiply` | Nova base = `base × escalado` |
| `add` | Nova base = `base + escalado` |
| `overwrite` | Nova base = `escalado` |

Operação do modifier: `ADDITION`.

#### Com `include_modifiers: true`

Referência = **valor final** do attribute (base + armadura, buffs, etc.).

| Modo | Efeito no total |
|------|-----------------|
| `multiply` | Total final × escalado |
| `add` | Total final + escalado |
| `overwrite` | Total = escalado |

Operação do modifier: `MULTIPLY_TOTAL` (ou `ADDITION` se referência = 0).

Após alterar `minecraft:generic.max_health`, o mod ajusta HP atual (cura cheia ou clamp, conforme contexto).

---

## `include_modifiers`

Só afeta wiring de **`attribute`**.

| Valor | Comportamento |
|-------|---------------|
| `false` | Escalador muda só a **base** do attribute. Equipamento soma depois. |
| `true` | Escalador age sobre o **total** (inclui bônus de itens). |

Se algum attribute wired usar `include_modifiers: true`, troca de equipamento (mão/armadura) dispara refresh automático do player.

---

## Combate: hurt, hurt_self e heal

### Roteamento do dano (`LivingHurtEvent`)

Ordem de decisão:

```
1. hurt_self tem match EXPLÍCITO (tipo ou wildcard)?
   → Escala com level do ALVO (mesmo com atacante externo)

2. Sem atacante (ambiental, fogo, void, etc.)?
   → hurt_self.default com level do ALVO

3. Atacante externo (atacante ≠ alvo)?
   → hurt com level do ATACANTE

4. Self-damage sem entrada explícita em hurt_self?
   → Sem scaling CraftLevel (dano vanilla)
```

### `hurt` — dano causado

- **Player:** wiring de `player_stats.json`
- **Mob:** `mob_overrides` ou `mob_stats`
- Usa level de quem **causa** o dano
- Resolve: chave exata → wildcard → `default`

Damage types comuns (referência):

| ID | Uso típico |
|----|------------|
| `minecraft:player_attack` | Soco/espada |
| `minecraft:arrow` | Flecha |
| `minecraft:trident` | Tridente |
| `minecraft:magic` / `indirect_magic` | Magia direta/indireta |
| `minecraft:mob_attack` | Ataque de mob |

IDs reais vêm do registro `DamageType` (`namespace:path`).

### `hurt_self` — dano recebido

| Situação | Chave usada |
|----------|-------------|
| Entrada explícita (tipo ou `*wildcard*`) | Essa entrada |
| Dano ambiental / sem atacante | `default` |
| Self-damage (ex.: flecha própria) sem entrada explícita | Nenhum — sem scaling |

### `heal` — cura recebida

Usa level de quem **recebe** a cura. Chave padrão: `default`.

---

## Comandos admin

Requer OP nível 2.

| Comando | Descrição |
|---------|-----------|
| `/craftlevel reload` | Recarrega JSONs e reaplica stats em players/mobs carregados |
| `/craftlevel level set <alvo> <level>` | Define level de player ou mob (máx. `lvl_max`) |
| `/craftlevel lp set <player> <amount>` | Define LP do jogador |

---

## Editor in-game

**Mods → Craft Level → Config** (botão na lista de mods Forge).

| Botão | Tela |
|-------|------|
| Config | LP, debug, escaladores |
| Client | HUD (posição, escala, anchors) |
| Player Stats / Mob Stats | Wiring attribute/hurt/hurt_self/heal |
| Mob Overrides | Lista de mobs + editor de wiring |
| Mob Levels | Tabela lvl_min / lvl_max |

Alterações em arquivos de servidor exigem `/craftlevel reload` ou reinício para aplicar em entidades já carregadas (reload reaplica automaticamente).

---

## Debug

Com `"debug": true` em `config.json`, o log mostra:

- Spawn/join de mobs com level calculado
- Resolução de escaladores em hurt/heal
- Modifiers aplicados em attributes
- Reload e comandos admin

---

## Fluxo resumido (diagrama)

```
                    config.json
                         │
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
    lp_scaling      scaling{}      mob_level_variation
         │               │               │
         ▼               ▼               ▼
   LP por level    Escaladores      Level dos mobs
                         │
              player_stats / mob_stats / mob_overrides
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
      StatRefreshService      EventScalingService
      (attributes)            (hurt / hurt_self / heal)
              │                     │
              ▼                     ▼
         Player / Mob           Combate
```

---

## Licença

Ver `LICENSE.txt` e metadados em `META-INF/mods.toml`. Mod por **grgs**.
