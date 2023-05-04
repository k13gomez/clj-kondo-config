# custom clj-kondo configuration

These clj-kondo hooks and settings help working with clara-rules and some custom def/defn macros

## Installation
Method of installation is to copy or merge the config.edn for clj-kondo and copy the provided hooks

### Clone clj-kondo configuration

```bash
$ mkdir -p ~/.config/clj-kondo
$ git clone https://github.com/k13gomez/clj-kondo-config.git ~/.config/clj-kondo
```

### Update clj-kondo configuration

```bash
$ mkdir -p ~/.config/clj-kondo/hooks
$ curl -o ~/.config/clj-kondo/config.edn https://raw.githubusercontent.com/k13gomez/clj-kondo-config/main/config.edn
$ curl -o ~/.config/clj-kondo/hooks/clara_rules.clj https://raw.githubusercontent.com/k13gomez/clj-kondo-config/main/hooks/clara_rules.clj
$ curl -o ~/.config/clj-kondo/hooks/gateless_rules.clj https://raw.githubusercontent.com/k13gomez/clj-kondo-config/main/hooks/gateless_rules.clj
```
