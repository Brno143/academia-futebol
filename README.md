# Academia de Base (Club Base)

Sistema de gerenciamento de uma academia de futebol de base, desenvolvido ao longo das 6 etapas da disciplina de Banco de Dados. Esta é a **versão final do projeto**: uma aplicação desktop em **Java (Swing) + JDBC** integrada ao banco MySQL `academia_futebol`, com CRUD, consultas, views, funções, procedimentos, triggers e um dashboard estatístico.

## Equipe

* Arthur da Fonte
* Bruno Santana
* Matheus Fialho
* Pablo Coelho

## Tecnologias

* **Java 17** com Swing (interface) e JDBC (acesso ao banco)
* **MySQL 8**
* **Maven** (dependência: MySQL Connector/J)

Todos os comandos SQL são escritos explicitamente e enviados com `PreparedStatement` / `CallableStatement`. O projeto não usa ORM nem biblioteca de gráficos: os gráficos são desenhados com `Graphics2D`.

## O banco de dados

O banco modela atletas, comissão técnica, categorias (Sub-11 a Sub-20), treinos, jogos, escalações e avaliações físicas, em 14 tabelas:

`CATEGORIA`, `PESSOA`, `TELEFONE`, `ATLETA`, `MEMBROCOMISSAO`, `ORIENTA`, `POSICAO`, `ADVERSARIO`, `TREINO`, `SESSAOTREINO`, `PARTICIPA`, `JOGO`, `ESCALACAO` e `AVALIACAOFISICA`, além da tabela de auditoria `LOG_PLACAR`.

## Etapas do projeto

| Etapa | Entrega | Onde está |
|---|---|---|
| 01 | Modelagem conceitual e lógica | Documentação da disciplina |
| 02 | Criação das tabelas e inserção de dados | `sql/01_criacao_tabelas.sql`, `sql/02_insercao_dados.sql` |
| 03 | 10 consultas SQL e primeira interface com CRUD | `sql/03_ajuste_categoria_jogo.sql`, `sql/consultas.sql`, aba **Consultas (Etapa 03)** |
| 04 | Índices, views e consultas avançadas (anti join, full outer join, subconsultas) | `sql/04_indices_consultas_views.sql`, aba **Views e Consultas Avançadas** |
| 05 | Funções, procedimentos (com cursor) e triggers | `sql/05_funcoes.sql`, `sql/05_procedimentos.sql`, `sql/05_triggers.sql`, aba **Funções, Procedimentos e Triggers** |
| 06 | Versão final: CRUD de 5 telas, integração das rotinas e dashboard estatístico | aba **Dashboard** e demais telas |

## Como rodar

### 1. Criar o banco

No MySQL Workbench ou no DBeaver, rodem os scripts da pasta `sql/` **nesta ordem**, cada um como script inteiro:

1. `01_criacao_tabelas.sql` (apaga e recria o banco)
2. `02_insercao_dados.sql`
3. `03_ajuste_categoria_jogo.sql`
4. `04_indices_consultas_views.sql`
5. `05_funcoes.sql`
6. `05_procedimentos.sql`
7. `05_triggers.sql`

Se algum script der erro, rodem tudo de novo a partir do `01`.

### 2. Configurar a conexão

Em `src/main/java/academia/Db.java`, troquem o usuário e a senha do MySQL (`USUARIO` e `SENHA`).

### 3. Executar

* **Eclipse**: File > Import... > Maven > Existing Maven Projects, selecionem esta pasta e Finish. Depois, botão direito em `App.java` > Run As > Java Application.
* **IntelliJ**: File > Open, escolham esta pasta e rodem a classe `App`.
* **Terminal**: `mvn compile exec:java`

## Telas da aplicação

| Aba | O que tem |
|---|---|
| **Dashboard** | Filtros, 8 indicadores, tabela de medidas estatísticas e 10 gráficos calculados a partir do banco |
| **Views e Consultas Avançadas** | As 2 views e as 4 consultas da Etapa 04, com SQL executado, filtros, indicadores, tabela com busca e gráfico |
| **Funções, Procedimentos e Triggers** | Executa as rotinas da Etapa 05 e mostra o efeito de cada uma |
| **Consultas (Etapa 03)** | As 10 consultas da Etapa 03, com busca no resultado |
| **Atletas, Jogos, Adversários, Escalações, Avaliações Físicas** | CRUD completo (listar, buscar, cadastrar, alterar e excluir) |

Os filtros de categoria e período são compartilhados entre o Dashboard, as Views e as Rotinas: o que for escolhido numa aba vale nas outras.

### CRUD

| Tela | Tabelas |
|---|---|
| Atletas | `PESSOA`, `ATLETA` e `TELEFONE` (gravadas na mesma transação) |
| Jogos | `JOGO` |
| Adversários | `ADVERSARIO` |
| Escalações | `ESCALACAO` (chave composta; o INSERT é validado pelo trigger) |
| Avaliações Físicas | `AVALIACAOFISICA` |

### Dashboard estatístico

Todos os valores são calculados a partir dos dados do banco, respeitando os filtros.

* **Filtros**: categoria, período (de/até), variável do histograma e par de variáveis da correlação.
* **Indicadores**: total de atletas e idade média, jogos (vitórias/empates/derrotas), aproveitamento com tendência (1ª x 2ª metade do período), média de gols pró e contra, saldo de gols, % de presença nos treinos, VO2 máx médio e IMC médio.
* **Medidas estatísticas**: n, média, mediana, moda, variância amostral, desvio padrão, coeficiente de variação, mínimo e máximo de 9 variáveis (VO2 máx, % gordura, IMC, peso, altura, idade, gols pró e contra por jogo e minutos por escalação).
* **Gráficos (10)**:
  1. Linha: gols pró x gols contra por jogo (tendência temporal)
  2. Linha: aproveitamento acumulado ao longo do tempo
  3. Pizza: distribuição dos resultados
  4. Barras: aproveitamento por categoria, com linha da média
  5. Histograma da variável escolhida (classes pela regra de Sturges)
  6. Dispersão com reta de regressão e correlação de Pearson (r e r²)
  7. Radar: perfil das categorias
  8. Barras agrupadas: presenças x faltas por categoria
  9. Pizza: minutos de treino por tipo de sessão
  10. Barras: minutos jogados por setor do campo

Passando o mouse sobre barras, pontos e fatias aparece o valor exato.

### Funções, procedimentos e triggers na interface

| Rotina | Como aparece |
|---|---|
| `fn_aproveitamento_categoria` | Tabela e gráfico de barras com o aproveitamento de cada categoria |
| `fn_situacao_frequencia` | Situação de frequência de cada atleta (Regular, Atenção, Crítica) e pizza das situações |
| `sp_atualizar_placar` | Formulário que faz o `CALL`, mostra a validação do `SIGNAL` (ex.: placar negativo) e o registro gerado no log |
| `sp_sequencia_invicta` (com cursor) | `CALL`, tabela e barras (maior sequência invicta x sequência atual) |
| `trg_log_placar` | Tabela `LOG_PLACAR`, atualizada sempre que um placar muda |
| `trg_valida_categoria_escalacao` | Teste de INSERT (desfeito com ROLLBACK) que mostra se o banco aceita ou bloqueia; também age no CRUD de Escalações |

## Estrutura do código (`src/main/java/academia`)

| Arquivo | O que faz |
|---|---|
| `App.java` | Janela principal com as abas |
| `Db.java` | Conexão JDBC, SELECT, INSERT/UPDATE/DELETE e `CALL` de procedimentos |
| `Ui.java` | Componentes de interface (formulários, cartões, busca em tabela) |
| `BarraFiltros.java` | Filtro de categoria e período compartilhado entre as telas |
| `Estatistica.java` | Média, mediana, moda, variância, desvio padrão, Pearson, regressão e classes do histograma |
| `Grafico.java` | Base dos gráficos (título e tooltip) |
| `GraficoBarras`, `GraficoLinha`, `GraficoPizza`, `GraficoRadar`, `GraficoDispersao` | Tipos de gráfico |
| `PainelDashboard.java` | Dashboard estatístico |
| `PainelViews.java` | Views e consultas da Etapa 04 |
| `PainelRotinas.java` | Funções, procedimentos e triggers da Etapa 05 |
| `PainelConsultas.java`, `Consultas.java` | Consultas da Etapa 03 |
| `PainelCrud.java` | Base das telas de cadastro |
| `PainelAtletas`, `PainelJogos`, `PainelAdversarios`, `PainelEscalacoes`, `PainelAvaliacoes` | Telas de CRUD |

## Scripts SQL (`sql/`)

| Arquivo | O que faz |
|---|---|
| `01_criacao_tabelas.sql` | Criação do banco e das tabelas |
| `02_insercao_dados.sql` | Inserção dos dados |
| `03_ajuste_categoria_jogo.sql` | Adiciona `cod_categoria` em `JOGO` |
| `consultas.sql` | As 10 consultas da Etapa 03 |
| `04_indices_consultas_views.sql` | Índices, views (`vw_ficha_atleta`, `vw_resultado_jogos`) e consultas avançadas |
| `05_funcoes.sql` | `fn_aproveitamento_categoria`, `fn_situacao_frequencia` |
| `05_procedimentos.sql` | `sp_atualizar_placar`, `sp_sequencia_invicta` |
| `05_triggers.sql` | `trg_log_placar`, `trg_valida_categoria_escalacao` e a tabela `LOG_PLACAR` |

A pasta `graficos_estatistica/` guarda os gráficos produzidos na disciplina de Estatística.
