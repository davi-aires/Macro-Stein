# Validação da preparação do repositório

Executada em 18/10/2026, com Minecraft 1.8.9, Loader 0.1.27, Bridge 0.1.21 e Stein GFX ligado.

- Build completo a partir do pacote base não adaptado: passou.
- Eventos de teclado/mouse, ordem, chat, consumo, estados e overflow: passaram.
- Verificação real de chamadas OpenGL do Stein Loader: passou.
- Instância isolada com somente Macros e MigrationProbe: iniciou.
- Controles extras e clique no botão Macro Settings: passaram.
- Atribuições, editor, seleção e botão de parar: passaram.
- Execução de `SET` pelo interpretador original: passou.
- Na preparação inicial, os 45 recursos não Java foram preservados byte a byte. Na release, somente steinmod.json muda para registrar versão, créditos e Loader mínimo; os demais 44 continuam preservados.
- As classes gráficas geradas são idênticas às do pacote anteriormente validado no jogo. A classe de entrada foi recompilada com o compilador atual e passou nos testes de comportamento.

O ambiente restrito registrou falhas de conexão loopback no subsistema de rede do Loader e um aviso de metadata de resourcepack do pacote base. Esses avisos não impediram os testes locais de interface. Não foi feita validação de conexão, permissões ou comandos de servidor.

Logs e capturas ficam somente na pasta ignorada `build/`. Não são necessários para compilar a adaptação.

## 1.0.1 — seletor de itens (19/10/2026)

O teste foi ampliado para abrir `GuiMacroParam` pelo parser real de `$$i:d`, como no prompt da macro de venda. Na 1.0.0 reproduziu o mesmo crash de `ItemStackInfo.itemRenderer` nulo. Na 1.0.1 abriu a lista com itens, desenhou os ícones e capturou a tela com GFX ligado. Os demais testes de interface, entrada e compatibilidade também passaram. Não foram executadas vendas ou operações em servidor.
