# Academia de Base (Club Base) - Etapa 03

Aplicação desktop em **Java (Swing) + JDBC** para o banco `academia_futebol` (MySQL).
Todos os comandos SQL são escritos explicitamente e enviados com `PreparedStatement`. Não usa ORM.

## Requisitos

- JDK 17 ou mais novo
- MySQL com os scripts da pasta `sql/` já rodados, na ordem: `01_criacao_tabelas.sql`, `02_insercao_dados.sql` (Etapa 02) e `03_ajuste_etapa03_categoria_jogo.sql` (ajuste feito na Etapa 03, que adiciona a categoria em JOGO)

## Como rodar

1. Abram `src/main/java/academia/Db.java` e troquem o usuário e a senha do MySQL (`USUARIO` e `SENHA`).
2. **Eclipse**: File > Import... > Maven > Existing Maven Projects, selecionem esta pasta e Finish (ele baixa o driver do MySQL sozinho). Depois é só clicar com o botão direito em `App.java` > Run As > Java Application.
3. **IntelliJ**: File > Open, escolham esta pasta (ele reconhece o `pom.xml` e baixa o driver sozinho). Depois é só rodar a classe `App`.
4. **Terminal** (com Maven instalado): `mvn compile exec:java`

## Telas

- **Painel**: indicadores gerais e 4 gráficos com dados do banco
- **Estatística**: gráficos da disciplina de estatística (as imagens ficam na pasta `graficos_estatistica/`)
- **Consultas**: as 10 consultas, mostrando o SQL e o resultado
- **Atletas, Jogos e Adversários**: listar, cadastrar, alterar e excluir

## Estrutura

| Arquivo | O que faz |
|---|---|
| `Db.java` | Conexão JDBC e execução de SQL |
| `Consultas.java` | As 10 consultas (mesmas de `sql/04_consultas.sql`) |
| `App.java` | Janela principal com as abas |
| `PainelDashboard.java` / `GraficoBarras.java` | Painel e gráficos |
| `PainelEstatistica.java` | Exibe os gráficos de estatística |
| `PainelConsultas.java` | Tela das consultas |
| `PainelCrud.java` | Base das telas de cadastro |
| `PainelAtletas.java` | INSERT/UPDATE/DELETE em PESSOA, ATLETA e TELEFONE (com transação) |
| `PainelJogos.java` | INSERT/UPDATE/DELETE em JOGO |
| `PainelAdversarios.java` | INSERT/UPDATE/DELETE em ADVERSARIO |

## Scripts SQL (pasta `sql/`)

| Arquivo | O que faz |
|---|---|
| `01_criacao_tabelas.sql` | Criação das tabelas (Etapa 02) |
| `02_insercao_dados.sql` | Inserção dos dados (Etapa 02) |
| `03_ajuste_categoria_jogo.sql` | Ajuste feito na Etapa 03: adiciona `cod_categoria` na tabela JOGO |
| `consultas.sql` | As 10 consultas usadas na aba Consultas, documentadas |