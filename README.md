# Academia de Base (Club Base)

## Equipe
* Arthur da Fonte
* Bruno Santana
* Matheus Fialho
* Pablo Coelho

Aplicação desktop em **Java (Swing) + JDBC** para o banco `academia_futebol` (MySQL).
Todos os comandos SQL são escritos explicitamente e enviados com `PreparedStatement` / `CallableStatement`. Não usa ORM nem biblioteca de gráficos: os gráficos são desenhados com `Graphics2D`.

## Requisitos

- JDK 17 ou mais novo
- MySQL 8 com os scripts da pasta `sql/` rodados **nesta ordem**:
  1. `01_criacao_tabelas.sql`
  2. `02_insercao_dados.sql`
  3. `03_ajuste_categoria_jogo.sql`
  4. `04_indices_consultas_views.sql`
  5. `05_funcoes.sql`
  6. `05_procedimentos.sql`
  7. `05_triggers.sql`

## Como rodar

1. Abram `src/main/java/academia/Db.java` e troquem o usuário e a senha do MySQL (`USUARIO` e `SENHA`).
2. **Eclipse**: File > Import... > Maven > Existing Maven Projects, selecionem esta pasta e Finish (o driver do MySQL é baixado sozinho). Depois, botão direito em `App.java` > Run As > Java Application.
3. **IntelliJ**: File > Open, escolham esta pasta e rodem a classe `App`.
4. **Terminal** (com Maven): `mvn compile exec:java`

## Telas

| Aba | O que tem |
|---|---|
| **Dashboard** | Filtros (categoria, período, variável e par de correlação), 8 indicadores, tabela de medidas estatísticas e 10 gráficos, tudo calculado a partir do banco |
| **Views e Consultas Avançadas** | As 2 views e as 4 consultas da Etapa 04, com SQL, filtros, indicadores, tabela, busca e gráfico |
| **Funções, Procedimentos e Triggers** | Executa as rotinas da Etapa 05 e mostra o efeito delas |
| **Consultas (Etapa 03)** | As 10 consultas da Etapa 03, com busca no resultado |
| **Atletas, Jogos, Adversários, Escalações, Avaliações Físicas** | CRUD (listar, buscar, cadastrar, alterar, excluir) |

### Dashboard estatístico

- **Filtros**: categoria (ou todas), período (de/até), variável do histograma e as duas variáveis da correlação. Os filtros valem para os indicadores, para a tabela de medidas e para os gráficos.
- **Indicadores**: atletas (e idade média), jogos (V/E/D), aproveitamento (com tendência: 1ª x 2ª metade do período), média de gols pró e contra, saldo de gols, % de presença nos treinos, VO2 máx médio e IMC médio.
- **Medidas estatísticas**: n, média, mediana, moda, variância (amostral), desvio padrão, coeficiente de variação, mínimo e máximo de VO2 máx, % gordura, IMC, peso, altura, idade, gols pró/contra por jogo e minutos por escalação.
- **Gráficos (10)**:
  1. Linha: gols pró x gols contra por jogo (tendência temporal)
  2. Linha: aproveitamento acumulado ao longo do tempo
  3. Pizza: distribuição dos resultados (vitória/empate/derrota)
  4. Barras: aproveitamento por categoria, com linha da média
  5. Histograma (distribuição de frequência por classes, regra de Sturges) da variável escolhida
  6. Dispersão com reta de regressão e coeficiente de Pearson (r e r²)
  7. Radar: perfil das categorias (VO2, % gordura, IMC, presença, aproveitamento, gols)
  8. Barras agrupadas: presenças x faltas por categoria
  9. Pizza: minutos de treino por tipo de sessão
  10. Barras: minutos jogados por setor do campo

Passando o mouse sobre barras, pontos e fatias aparece o valor exato.

### Funções, procedimentos e triggers (Etapa 05) na interface

| Rotina | Como aparece |
|---|---|
| `fn_aproveitamento_categoria` | Tabela + gráfico de barras do aproveitamento de cada categoria |
| `fn_situacao_frequencia` | Situação de frequência de cada atleta, com filtro por categoria e pizza das situações |
| `sp_atualizar_placar` | Formulário que faz o `CALL`. Mostra a validação do `SIGNAL` (ex.: placar negativo) e o registro gerado no log |
| `sp_sequencia_invicta` (cursor) | `CALL` + tabela + barras (maior sequência invicta x atual) |
| `trg_log_placar` | Tabela `LOG_PLACAR` atualizada sempre que um placar muda (aba Jogos ou procedimento) |
| `trg_valida_categoria_escalacao` | Teste de INSERT (desfeito com ROLLBACK) que mostra se o banco aceita ou bloqueia. Também age no CRUD de Escalações |

## Estrutura

| Arquivo | O que faz |
|---|---|
| `App.java` | Janela principal com as abas |
| `Db.java` | Conexão JDBC, SELECT, INSERT/UPDATE/DELETE e `CALL` de procedimentos |
| `Ui.java` | Componentes e utilidades de interface (formulários, cartões, busca em tabela) |
| `BarraFiltros.java` | Filtro de categoria e período reutilizado nas telas |
| `Estatistica.java` | Média, mediana, moda, variância, desvio padrão, Pearson, regressão e classes do histograma |
| `Grafico.java` | Base dos gráficos (título, tooltip) |
| `GraficoBarras`, `GraficoLinha`, `GraficoPizza`, `GraficoRadar`, `GraficoDispersao` | Tipos de gráfico |
| `PainelDashboard.java` | Dashboard estatístico |
| `PainelViews.java` | Views e consultas da Etapa 04 |
| `PainelRotinas.java` | Funções, procedimentos e triggers da Etapa 05 |
| `PainelConsultas.java` / `Consultas.java` | Consultas da Etapa 03 |
| `PainelCrud.java` | Base das telas de cadastro |
| `PainelAtletas.java` | CRUD de PESSOA, ATLETA e TELEFONE (com transação) |
| `PainelJogos.java` | CRUD de JOGO |
| `PainelAdversarios.java` | CRUD de ADVERSARIO |
| `PainelEscalacoes.java` | CRUD de ESCALACAO (chave composta, validada pelo trigger) |
| `PainelAvaliacoes.java` | CRUD de AVALIACAOFISICA |

## Scripts SQL (pasta `sql/`)

| Arquivo | O que faz |
|---|---|
| `01_criacao_tabelas.sql` | Criação das tabelas (Etapa 02) |
| `02_insercao_dados.sql` | Inserção dos dados (Etapa 02) |
| `03_ajuste_categoria_jogo.sql` | Ajuste da Etapa 03: adiciona `cod_categoria` em JOGO |
| `consultas.sql` | As 10 consultas da Etapa 03 |
| `04_indices_consultas_views.sql` | Índices, views e consultas avançadas (Etapa 04) |
| `05_funcoes.sql`, `05_procedimentos.sql`, `05_triggers.sql` | Funções, procedimentos e triggers (Etapa 05) |
