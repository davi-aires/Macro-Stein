# Publicar pelo Stein Client

Formato seguido: [Publishing your mod — Stein SDK](https://github.com/Stein-Project/sdk#publishing-your-mod).

O repositório distribui uma adaptação de Macros. O manifest preserva `id: macros` e a classe de entrada original; a versão da adaptação é independente da versão 0.11.4 do interpretador. `loader: 0.1.27` registra o mínimo validado para as integrações usadas.

## Preparação

O build gera `build/libs/Macros.steinmod`. Depois dos testes, execute:

```powershell
python -m unittest discover -s tests -p 'test_*.py'
python scripts/prepare_release.py
```

O script lê o manifest **de dentro do arquivo final**, verifica o id, a classe de entrada, a versão e o Loader mínimo e calcula o SHA256. Ele também rejeita o probe de testes e arquivos nos namespaces do jogo. Em `dist/`, gera:

- `Macros.steinmod`: o pacote final, sem alteração posterior ao cálculo do hash.
- `steinmods.json`: índice no formato 1 do Client, com id, modId, versão, URL, hash e Loader mínimo.
- `SHA256SUMS.txt`: checksum do pacote.
- `notes.md`: changelog e informações para o corpo da release.

O índice aponta para uma URL de download com a tag exata, evitando que o pacote mude sem mudar o hash. O campo `source` aponta para o índice da última release estável.

## Publicação

Faça commit dos fontes na `main` antes de criar a tag `v<versão>`. Crie a release nessa tag, inicialmente como rascunho, com `Macros.steinmod`, `steinmods.json` e `SHA256SUMS.txt`. Use `dist/notes.md` como descrição. Confira os arquivos e publique como release estável e mais recente.

Não envie `MigrationProbe.steinmod`, bibliotecas de compilação, a instalação do jogo ou pastas de testes. Não substitua assets de uma versão publicada: corrija a versão do manifest, adicione um changelog e publique uma nova tag.

Depois de publicar, baixe novamente os assets e confira que o SHA256 do pacote corresponde ao índice e ao `SHA256SUMS.txt`.

## Link para os jogadores

```text
https://github.com/davi-aires/Macro-Stein/releases/latest/download/steinmods.json
```

O jogador adiciona esse link nas fontes de mods de terceiros do Stein Client. Isso não inscreve o mod no catálogo oficial: fontes oficiais têm prioridade se oferecerem o mesmo `modId`.

## Distribuição do mod base

A publicação inclui conteúdo do Macros original. O mantenedor informou em 18/10/2026 possuir autorização para redistribuí-lo junto com a adaptação. Os créditos originais são mantidos. Essa autorização informada não aplica automaticamente uma licença MIT ou MPL ao código do Macros.
