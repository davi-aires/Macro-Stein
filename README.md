# Macros para Stein GFX

Adaptação do **Macro / Keybind Mod 0.11.4** para Minecraft 1.8.9 com **Stein Loader 0.1.27**, **Stein Bridge 0.1.21** e **Stein GFX ativado**.

- Teclado e mouse integrados às filas de eventos do launcher novo.
- Fontes, cores, editor, seleção de texto e ícones renderizados com GFX.
- Cursor e botão de parar mantêm a inversão de cores original.
- A tela de Controles volta a mostrar a combinação de teclas, o vínculo de override e o botão **Macro Settings...**, que abre a tela de atalhos.
- O interpretador, os scripts e os recursos do mod original são preservados.
- A versão 1.0.1 corrige o crash ao desenhar ícones nos seletores de itens (`$$i`), incluindo prompts usados por macros de venda.

Este repositório contém **a adaptação e seu aplicador**, não o código completo do Macros. O pacote pronto é distribuído nas releases com autorização informada pelo mantenedor. O pacote original, o Minecraft e as bibliotecas do Stein são fornecidos pela instalação local. BetterSprint e outros mods não fazem parte deste projeto.

## Download e atualizações

Baixe **Macros.steinmod** na [última release](https://github.com/davi-aires/Macro-Stein/releases/latest).

Para instalar e receber atualizações pelo **Stein Client**, adicione este link em mods de terceiros:

```text
https://github.com/davi-aires/Macro-Stein/releases/latest/download/steinmods.json
```

O índice inclui a versão, o Loader mínimo e o SHA256 do arquivo. O `modId` continua sendo `macros`, preservando a identificação do mod original. Se uma fonte oficial fornecer esse mesmo `modId`, o Client dará prioridade à versão oficial, conforme o [guia do SDK](https://github.com/Stein-Project/sdk#publishing-your-mod).

A versão **1.0.1** identifica esta adaptação; o interpretador continua sendo o Macro / Keybind Mod **0.11.4**. O pacote exige Loader **0.1.27** ou superior e foi validado com Bridge **0.1.21**. Versões futuras do motor precisam de nova validação.

## Compilar

Requisitos: **Python 3.10+**, **Java 25+**, uma instalação local da SVG 7.0 e o **Macros 0.11.4 já portado para Stein** (`Macros.jar` ou `Macros.steinmod`). O arquivo `.litemod` para LiteLoader não serve como base.

Na raiz do repositório, em PowerShell:

```powershell
python scripts/build.py --input "CAMINHO/DO/Macros.jar" --game-dir "CAMINHO/DA/.steinclient" --download-deps
```

O Java é encontrado no runtime da instalação. Para usar outro executável, acrescente `--java "CAMINHO/DO/java"`.

O primeiro build baixa ASM 9.8 e ECJ 3.43.0 do Maven Central para `deps/`. Versões e SHA256 estão fixados em `dependencies.lock.json`. Depois, omita `--download-deps` para trabalhar offline. Também é possível fornecer uma pasta com as dependências usando `--deps-dir`.

Saídas locais:

```text
build/libs/Macros.steinmod          mod adaptado para uso local
build/libs/MigrationProbe.steinmod  mod auxiliar, exclusivamente para testes
build/build-report.json            hashes da entrada e da saída
```

O build aceita tanto a base original quanto a etapa intermediária que já contém `Svg7Input`. Ele rejeita um pacote que já tenha a adaptação GFX. Não há instalação automática nem alteração das configurações do launcher.

## Usar

Feche o jogo, faça backup do seu `mods/Macros.steinmod` e substitua-o pelo arquivo gerado em `build/libs/Macros.steinmod`. Mantenha apenas uma cópia do Macros na pasta de mods. Preserve seu diretório `liteconfig` e mantenha o GFX ligado.

Em **Options → Controls → Macro / Keybind Mod**, use **Macro Settings...** para abrir as atribuições. A combinação configurada em **Settings key combo** também permite abrir as configurações.

Não instale `MigrationProbe.steinmod` na sua modpack: ele abre telas automaticamente durante o teste.

## Testar

O build já executa os testes de entrada e a verificação real de compatibilidade OpenGL do Stein Loader.

Para testar as telas em uma instância isolada:

```powershell
python scripts/test_launch.py --game-dir "CAMINHO/DA/.steinclient" --probe --gl-debug
```

O teste abre uma janela temporária e a encerra ao concluir. Usa as bibliotecas e os assets da instalação, carrega somente o Macros e o probe em `build/test-gfx-true-*` e usa um perfil de teste sem autenticação. Não abre mundos nem usa a sessão da instância principal. Logs e capturas ficam nessa pasta de build.

Verificações: três controles extras do Macros, clique real em **Macro Settings...**, tela de atribuições, editor, seleção, botão de parar e execução de uma macro `SET` pelo interpretador original e seletor de itens `$$i:d` com ícones. Não testa permissões ou comandos de servidores.

## Organização

```text
src/main/java/       adaptadores de entrada, desenho e reflexão
src/main/resources/ manifest da versão distribuída
src/patcher/java/    aplicação das alterações de bytecode
scripts/            build e teste de interface
tests/java/         testes de entrada, compatibilidade e interface
docs/               detalhes técnicos e validação
changelog/          histórico da adaptação
```

Veja [a arquitetura](docs/architecture.md) e [o histórico da versão atual](changelog/1.0.1.pt_BR.md).

## Contribuir

Ao alterar a adaptação, execute o build e o teste de interface. Preserve o formato dos scripts, as configurações existentes e as métricas das fontes. Mudanças nas versões do Loader, Bridge ou GFX exigem nova validação: esta adaptação usa também classes internas do Stein.

Não adicione binários, arquivos da instalação, configurações pessoais ou logs ao Git. Eles são ignorados pelo `.gitignore`. Os pacotes distribuídos ficam como assets das releases, separados dos fontes.

## Preparar uma publicação

1. Atualize `version` em `src/main/resources/steinmod.json` e escreva `changelog/<versão>.pt_BR.md`.
2. Execute o build, os testes de interface e `python -m unittest discover -s tests -p 'test_*.py'`.
3. Execute `python scripts/prepare_release.py`. Ele cria `dist/Macros.steinmod`, `steinmods.json`, `SHA256SUMS.txt` e `notes.md`, sem publicar nada.
4. Faça commit dos fontes, crie a tag `v<versão>` e crie uma release estável nessa tag, com os três assets e o texto de `notes.md`. Não inclua o MigrationProbe.
5. Confira o link fixo do índice e o SHA256 do download antes de anunciar a versão.

O build permanece local porque depende do pacote base e das bibliotecas da instalação. O workflow de CI testa os scripts e a geração do índice; ele não baixa uma cópia privada do mod original. Detalhes em [docs/publishing.md](docs/publishing.md).

## Créditos e distribuição

Macro / Keybind Mod é de **Mumfrey**. Este projeto não é uma publicação oficial do mod nem do Stein. Veja [NOTICE](NOTICE) para a origem dos componentes. A licença do repositório Replay não é aplicada a este projeto; nenhuma licença de distribuição foi escolhida para os fontes desta adaptação.

O pacote base traz uma indicação de uso local; a distribuição desta adaptação foi autorizada pelo mantenedor, que informou ter autorização para redistribuir o Macros original junto com ela. A autoria original e suas condições de licença são preservadas. Minecraft e as bibliotecas do Stein não são incluídos no pacote.
