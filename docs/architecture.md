# Arquitetura

O patch mantém o pacote Macros original e substitui pontos de integração incompatíveis. Não implementa um novo interpretador de macros.

1. `PatchMacros` adapta quatro classes de entrada para `com.litemod.Svg7Input`, que exporta e reimporta os eventos do `Win` nos buffers esperados pelo Macros. Mantém ordem, estados pressionados, limites dos lotes e interrupção no atalho que abre o chat.
2. `PatchMacrosGfx` substitui display lists das duas fontes por comandos na CPU. `Svg7Graphics` preserva métricas, atlas, estilos e cores e envia o desenho ao caminho de captura do GFX.
3. `Svg7Logic` desenha o cursor e o ícone invertido com OpenGL core, sincronizando o GFX e restaurando o estado alterado.
4. As chamadas de tessellator do Macros são redirecionadas para a adaptação. Chamadas ao setter removido `loader.compat.Reflect.setField` passam para `Svg7Reflect`, permitindo criar os controles extras e o botão de configurações.

O namespace das classes é preservado para manter compatibilidade com o bytecode já adaptado. Compilação usa bytecode Java 8, com ECJ executado em Java 25 para ler as bibliotecas recentes do Stein.

## Escopo verificado

Minecraft 1.8.9, Stein Loader 0.1.27, Bridge 0.1.21 e GFX ligado. A compatibilidade com versões posteriores não é presumida: nomes internos podem mudar.

`TestSvg7Input` verifica chat, ordem, consumo de teclas, botões, scroll e overflow. `TestGfxCompatibility` invoca o verificador real do Loader. `MigrationProbe`, carregado apenas na instância isolada, testa telas e execução do interpretador original.

O build preserva os recursos não alterados do arquivo de entrada. Apenas `steinmod.json` é substituído pelo manifest de distribuição, com versão da adaptação, Loader mínimo, descrição e créditos. Os hashes de entrada e saída são registrados localmente; dependências de compilação têm hashes fixados. O teste gráfico usa uma cópia dos mods e um diretório de jogo separado, sem copiar configurações pessoais de macros.

Não há envio de pacotes adicionais, alteração das bibliotecas instaladas ou configuração automática do GFX.
