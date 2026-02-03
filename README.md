Tradutor Universal (Hytale) + API de Tradução
============================================

Projeto dividido em dois componentes:

- **Mod/Plugin para Hytale** (`Tradutor/`): intercepta o chat do servidor e envia para tradução por jogador via WebSocket.
- **API de Tradução** (`API/`): servidor FastAPI que recebe mensagens e usa OpenAI para devolver traduções no formato esperado.

Visão geral
-----------

O plugin cancela o chat original e:

1. Envia a mensagem original **somente para o remetente** (para ele ver instantaneamente).
2. Envia um payload via WebSocket para a API com a lista de jogadores e seus idiomas.
3. Recebe a resposta com as traduções e entrega **mensagens traduzidas para o idioma selecionado de cada jogador individualmente**.

Se a API/WS não estiver configurada ou indisponível, o plugin **não retransmite o chat** para os outros jogadores (apenas o remetente verá a mensagem).

Estrutura do repositório
------------------------

```
.
├─ API/
│  ├─ tradutor.py
│  ├─ requirements.txt
│  └─ .env.example
├─ Tradutor/
│  ├─ manifest.json
│  ├─ META-INF/
│  ├─ sources.txt
│  └─ com/jogandobem/... (código-fonte Java)
└─ README.md
```

Requisitos
----------

Plugin (Hytale):
- Servidor Hytale com suporte a plugins Java.
- Java 21 (as classes são compiladas com major version 65).

Discord (opcional):
- Dependências da JDA no classpath **ou** no próprio `.jar` (fat jar).

Auto-detecção por IP (opcional):
- Token do ipinfo.io (campo `ipinfo_token`).

API (FastAPI):
- Python 3.x com `pip`.
- Dependências em `API/requirements.txt`.
- Chave da OpenAI para traduções reais (opcional; sem chave usa fallback).

Instalação e configuração da API
--------------------------------

1) Entre na pasta da API e instale dependências:

```
pip install -r requirements.txt
```

2) Crie o arquivo `.env` com base em `API/.env.example`:

```
MODELO=gpt-5-nano # ou um de sua escolha. o gpt-5-nano tem se saído muito bem (qualidade x velocidade)
OPENAI_API_KEY=sua_chave_aqui
```

Variáveis de ambiente suportadas (em `tradutor.py`):
- `MODELO`: modelo da OpenAI usado nas traduções (padrão: `gpt-5-nano`).
- `OPENAI_API_KEY`: chave da OpenAI (se vazio, a API devolve o texto original).
- `LOG_LEVEL`: nível de log (padrão: `DEBUG`).

3) Execute a API:

```
python tradutor.py
```

Isso sobe o servidor em `127.0.0.1:5521` com WebSocket em `/ws`.

Se quiser expor para outras máquinas, use uvicorn:

```
uvicorn tradutor:app --host 0.0.0.0 --port 5521 --log-level info
```

Instalação do plugin
--------------------

O plugin usa manifesto de mod Hytale em `Tradutor/manifest.json`:

- Nome: `TradutorUniversal`
- Versão: `1.0.1`
- Classe principal: `com.jogandobem.TradutorUniversal`

Copie o plugin compilado para o local de mods do seu servidor Hytale conforme o fluxo de instalação que você já utiliza.

Configuração do plugin
----------------------

Na primeira execução, o plugin cria `translator_config.json` na pasta de dados do plugin. Valores padrão (do código):

```
{
  "api_host": "http://127.0.0.1:5521",
  "api_key": "",
  "ws_url": "ws://127.0.0.1:5521/ws",
  "server_id": "server-1",
  "server_secret": "",
  "default_language": "auto",
  "warn_on_join": true,
  "warn_message": "Servidor com traducao automatica. Use /l <codigo> para escolher o idioma.",
  "ipinfo_token": "",
  "api_timeout_ms": 60000,
  "ws_reconnect_seconds": 3,
  "pending_ttl_seconds": 30
}
```

Campos e função:
- `api_host`: base HTTP (usado apenas pela classe `TranslationService`, atualmente não utilizada pelo fluxo do plugin).
- `api_key`: enviado no request HTTP (não usado no WebSocket atual).
- `ws_url`: URL WebSocket da API (obrigatório para o fluxo principal).
- `server_id`: identificador do servidor (enviado no hello e no payload).
- `server_secret`: enviado no hello; a API atual **não valida**.
- `default_language`: idioma padrão quando o jogador não escolhe nenhum (ex.: `auto`, `pt`, `en`).
- `warn_on_join`: envia aviso de tradução ao entrar.
- `warn_message`: texto do aviso.
- `ipinfo_token`: token do ipinfo.io para auto-detecção por IP.
- `api_timeout_ms`: timeout de HTTP (caso use `TranslationService`).
- `ws_reconnect_seconds`: intervalo de reconexão do WebSocket.
- `pending_ttl_seconds`: tempo máximo aguardando resposta de tradução por mensagem.

O plugin também cria:
- `languages.json` (idioma e IP por jogador).
- `messages.json` (textos/idiomas do plugin).
- `discord.json` (configuração do Discord).

Auto-detecção por IP (ipinfo.io)
--------------------------------

- Só roda se `ipinfo_token` estiver configurado.
- Só roda **para jogadores sem entrada** em `languages.json`.
- Se não conseguir resolver o IP do jogador, **não consulta** o ipinfo e registra log de erro.
- Para forçar nova detecção: use `/l auto` ou remova a entrada do jogador em `languages.json`.

Discord (opcional)
------------------

O plugin cria `discord.json` na pasta de dados do plugin. Exemplo mínimo:

```
{
  "botToken": "SEU_TOKEN_AQUI",
  "channelsIds": {
    "pt-BR": "id_canal_discord",
    "en-US": "outro_id_canal_discord"
  },
  "gameToDiscordFormat": "**[{player}]**: {message}",
  "discordToGameFormat": "[DISCORD] {user}: {message}",
  "serverEventsEnabled": true,
  "playerJoinLeaveEnabled": false,
  "webhookUrl": "",
  "useWebhookForChat": false,
  "useWebhookForEvents": false,
  "statusEnabled": true,
  "statusUpdateIntervalSeconds": 60
}
```

Notas:
- `channelsIds` mapeia **idioma → canal**. Se não houver canal para o idioma, ele não envia.
- Você pode usar **bot** (via `botToken`) ou **webhook** (`webhookUrl`, `useWebhookForChat`, `useWebhookForEvents`).
- Para Discord funcionar, o `.jar` precisa conter as dependências da JDA (fat jar) **ou** a JDA deve estar no classpath do plugin.

Comandos
--------

- `/l` | Mostra idioma atual e instruções.
- `/l <codigo_idioma>` | Define o idioma do jogador. Exemplo: `/l` `pt-BR`
- `/l auto` ou `/l default` ou `/l padrao` | Remove idioma personalizado e volta ao padrão.
- `/treload` | Recarrega o `translator_config.json` e a lista de idiomas.

Idiomas suportados pela OpenAI (códigos + variantes)
----------------------------------------------------

- Português: `pt-BR`, `pt-PT`, `pt-AO`, `pt-MZ`
- Inglês: `en-US`, `en-GB`, `en-CA`, `en-AU`, `en-IN`, `en-NZ`, `en-IE`, `en-ZA`
- Espanhol: `es-ES`, `es-MX`, `es-AR`, `es-CO`, `es-CL`, `es-PE`, `es-US`
- Francês: `fr-FR`, `fr-CA`, `fr-BE`, `fr-CH`, `fr-LU`
- Alemão: `de-DE`, `de-AT`, `de-CH`
- Italiano: `it-IT`, `it-CH`
- Holandês: `nl-NL`, `nl-BE`
- Sueco: `sv-SE`
- Norueguês (Bokmål/Nynorsk): `nb-NO`, `nn-NO`
- Dinamarquês: `da-DK`
- Finlandês: `fi-FI`
- Islandês: `is-IS`
- Irlandês: `ga-IE`
- Galês: `cy-GB`
- Polonês: `pl-PL`
- Tcheco: `cs-CZ`
- Eslovaco: `sk-SK`
- Húngaro: `hu-HU`
- Romeno: `ro-RO`, `ro-MD`
- Búlgaro: `bg-BG`
- Grego: `el-GR`, `el-CY`
- Russo: `ru-RU`
- Ucraniano: `uk-UA`
- Sérvio (cirílico/latino): `sr-RS`, `sr-Cyrl-RS`, `sr-Latn-RS`
- Croata: `hr-HR`
- Bósnio: `bs-BA`
- Esloveno: `sl-SI`
- Albanês: `sq-AL`, `sq-XK`
- Macedônio: `mk-MK`
- Lituano: `lt-LT`
- Letão: `lv-LV`
- Estoniano: `et-EE`
- Turco: `tr-TR`
- Árabe (geral + variantes): `ar`, `ar-SA`, `ar-EG`, `ar-MA`, `ar-AE`, `ar-DZ`
- Hebraico: `he-IL`
- Persa (Farsi/Dari): `fa-IR`, `fa-AF`
- Curdo (mais comuns): `ku-TR`, `ckb-IQ` (Sorani)
- Hindi: `hi-IN`
- Urdu: `ur-PK`, `ur-IN`
- Bengali: `bn-BD`, `bn-IN`
- Punjabi: `pa-IN`, `pa-PK`
- Tâmil: `ta-IN`, `ta-LK`, `ta-SG`, `ta-MY`
- Telugu: `te-IN`
- Marathi: `mr-IN`
- Gujarati: `gu-IN`
- Kannada: `kn-IN`
- Malayalam: `ml-IN`
- Sinhala: `si-LK`
- Nepali: `ne-NP`
- Chinês (mandarim – por região): `zh-CN`, `zh-TW`, `zh-HK`, `zh-SG`
  - (escritas úteis): `zh-Hans`, `zh-Hant`, `zh-Hans-CN`, `zh-Hant-TW`
- Cantonês (comum em HK): `yue-HK`, `yue-Hant-HK`
- Japonês: `ja-JP`
- Coreano: `ko-KR`
- Vietnamita: `vi-VN`
- Tailandês: `th-TH`
- Indonésio: `id-ID`
- Malaio: `ms-MY`, `ms-BN`, `ms-SG`
- Filipino/Tagalog: `fil-PH`, `tl-PH`
- Suaíli: `sw-KE`, `sw-TZ`
- Africâner: `af-ZA`
- Zulu: `zu-ZA`
- Xhosa: `xh-ZA`
- Amárico: `am-ET`
- Somali: `so-SO`, `so-KE`
- Iorubá: `yo-NG`
- Igbo: `ig-NG`
- Hauçá: `ha-NG`
- Shona: `sn-ZW`
- Latim: `la` (às vezes aparece como `la-VA`)
- Esperanto: `eo`

Fluxo de tradução
-----------------

1) Jogador envia mensagem no chat.
2) Plugin cancela o evento e envia a mensagem original apenas para o remetente.
3) Plugin gera `message_id`, monta a lista de jogadores online com seus idiomas e envia via WS.
4) API chama OpenAI e devolve `translations`.
5) Plugin envia a mensagem traduzida para cada jogador (exceto o remetente).

Protocolo WebSocket
-------------------

Handshake (cliente -> API):

```
{
  "type": "hello",
  "server_id": "server-1",
  "server_secret": "",
  "plugin": "TradutorUniversal",
  "version": "1.0.1"
}
```

ACK (API -> cliente):

```
{
  "type": "hello_ack",
  "server_id": "server-1",
  "ok": true
}
```

Payload de chat (cliente -> API):

```
{
  "type": "chat",
  "server_id": "server-1",
  "message_id": "1700000000-abcdef",
  "texto_original": "Olá!",
  "idioma_original": "pt",
  "jogador": "JogadorA",
  "jogador_uuid": "uuid-aqui",
  "jogadores_online": [
    { "jogador": "JogadorA", "idioma": "pt" },
    { "jogador": "PlayerB",  "idioma": "en" }
  ]
}
```

Resposta de tradução (API -> cliente):

```
{
  "type": "translations",
  "server_id": "server-1",
  "message_id": "1700000000-abcdef",
  "traducao": [
    { "jogador": "JogadorA", "texto_traduzido": "Olá!" },
    { "jogador": "PlayerB",  "texto_traduzido": "Hello!" }
  ]
}
```

Notas importantes:
- O plugin usa `message_id` para correlacionar respostas.
- A API atual **não valida** `server_secret`.
- Se uma tradução vier vazia para algum jogador, a API faz fallback para o texto original.

Detalhes da API (FastAPI)
-------------------------

Endpoint WebSocket: `ws://<host>:5521/ws`

Principais comportamentos (em `API/tradutor.py`):
- Monta prompt com lista de jogadores e idiomas.
- Exige JSON no formato: `{"traducao":[{"jogador":"Nome","texto_traduzido":"Mensagem"}]}`
- Para jogadores no mesmo idioma do remetente, retorna texto original exatamente igual.
- Se a OpenAI falhar ou retornar JSON inválido, faz fallback para texto original.

Compilação (Windows / PowerShell)
---------------------------------

1) Compile as classes (precisa do `HytaleServer.jar` e da JDA):

```
$src = Get-ChildItem -Recurse -Filter *.java -Path Tradutor\com | ForEach-Object { $_.FullName }
javac -cp "..\HytaleServer.jar;..\DiscordLink.jar" -d "Tradutor\build\classes" $src
```

2) Empacote o plugin (manifesto no root):

```
jar cfm "..\TradutorUniversal.jar" "Tradutor\META-INF\MANIFEST.MF" -C "Tradutor\build\classes" . -C "Tradutor" manifest.json
```

3) (Opcional) Fat jar com Discord embutido:
- Extraia um jar que já tenha JDA e mescle o conteúdo:

```
mkdir _jda_tmp
cd _jda_tmp
jar xf ..\..\DiscordLink.jar
cd ..
jar cfm "..\TradutorUniversal.jar" "Tradutor\META-INF\MANIFEST.MF" -C "Tradutor\build\classes" . -C "Tradutor" manifest.json -C "_jda_tmp" com -C "_jda_tmp" gnu -C "_jda_tmp" google -C "_jda_tmp" javax -C "_jda_tmp" kotlin -C "_jda_tmp" net/dv8tion -C "_jda_tmp" okhttp3 -C "_jda_tmp" okio -C "_jda_tmp" org -C "_jda_tmp" META-INF/services -C "_jda_tmp" META-INF/versions
```

Limites e comportamento de fallback
-----------------------------------

- Se `OPENAI_API_KEY` não estiver configurada, a API **retorna o texto original** para todos.
- Se a API estiver offline ou o WS não conectar, os outros jogadores **não recebem** o chat.
- O plugin re-tenta conexão WS a cada `ws_reconnect_seconds`.
- Mensagens pendentes expiram em `pending_ttl_seconds`.

Arquivos importantes
--------------------

Plugin:
- `Tradutor/com/jogandobem/TradutorUniversal.java` - bootstrap do plugin.
- `Tradutor/com/jogandobem/listeners/ChatListener.java` - intercepta chat e envia WS.
- `Tradutor/com/jogandobem/TranslationSocketClient.java` - cliente WebSocket.
- `Tradutor/com/jogandobem/LanguageStore.java` - idiomas por jogador.
- `Tradutor/com/jogandobem/TranslationConfig.java` - configuração.
- `Tradutor/com/jogandobem/discord/` - integração com Discord.

API:
- `API/tradutor.py` - servidor FastAPI e integração OpenAI.
- `API/requirements.txt` - dependências.

Segurança e privacidade
-----------------------

- O chat dos jogadores e seus nomes/idiomas são enviados para a API.
- Não há autenticação forte no WS (apenas `server_id` e `server_secret`, mas a API atual não valida).
- Se você expor a API publicamente, considere adicionar autenticação e TLS.

Problemas comuns
----------------

- **Plugin não carrega (Failed to load manifest file)**  
  Garanta que `manifest.json` está na raiz do `.jar`.

- **Discord não funciona (NoClassDefFoundError JDABuilder)**  
  A JDA não está no classpath. Use fat jar ou inclua JDA no classpath do plugin.

- **Auto-detecção por IP não funciona**  
  Verifique `ipinfo_token`, se o jogador já tem entrada em `languages.json`, e os logs `ChatTranslation ...`.

- **Outros jogadores não recebem mensagens**  
  Verifique `ws_url` e se a API está rodando. O plugin cancela o chat original.

- **Sem traduções (tudo original)**  
  Verifique `OPENAI_API_KEY` e o modelo configurado em `MODELO`.

- **WS conecta, mas sem traduções**  
  Veja os logs da API (`LOG_LEVEL=DEBUG`) e do servidor Hytale.
