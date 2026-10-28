-- =====================================================================
-- Etapa 04 - Consultas avançadas, visões e índices (academia_futebol)
-- Rodar DEPOIS dos scripts 01, 02 e 03.
-- Este script não deve ser rodado duas vezes sem recriar o banco:
-- os índices já existiriam e o CREATE INDEX daria erro.
-- =====================================================================
USE academia_futebol;


-- =====================================================================
-- 1) ÍNDICES
-- =====================================================================

-- Índice 1: PARTICIPA (cod_pessoa, presente)
-- Problema: a PK de PARTICIPA começa por cod_treino. Ela serve para
-- "quem participou do treino X", mas não para "como está a frequência
-- do atleta Y". Para contar presenças e faltas de um atleta, o banco
-- precisa achar as linhas pelo cod_pessoa e ler a coluna presente.
-- Com (cod_pessoa, presente) as duas informações ficam no próprio
-- índice, sem precisar ler a tabela (índice de cobertura).
-- Crescimento: cada atleta participa de vários treinos por semana,
-- então PARTICIPA é a tabela que mais cresce no sistema.
-- Usado em: view vw_ficha_atleta, consulta 2 (full outer join) e
-- consulta 4 (subconsultas).
-- Observação: o MySQL tinha criado sozinho um índice só em cod_pessoa
-- por causa da FK. Ao criar este, ele remove o antigo automaticamente,
-- porque o novo também serve para a FK (cod_pessoa é a 1ª coluna).
CREATE INDEX idx_participa_atleta_presenca
    ON PARTICIPA (cod_pessoa, presente);

-- Índice 2: AVALIACAOFISICA (cod_pessoa, data_avaliacao)
-- Problema: o atleta é avaliado várias vezes ao longo do ano, e o que
-- a comissão quer ver é a avaliação MAIS RECENTE de cada atleta.
-- Com (cod_pessoa, data_avaliacao) as avaliações de um atleta ficam
-- juntas e já ordenadas por data no índice, então achar a última
-- é ir direto ao fim do trecho daquele atleta, sem ordenar nada.
-- Usado em: view vw_ficha_atleta (última avaliação) e consulta 3
-- (busca das avaliações dos atletas da categoria de cima).
-- É UNIQUE: o mesmo atleta não faz duas avaliações físicas completas
-- no mesmo dia. Isso também garante que "a avaliação da data mais
-- recente" seja uma linha só, sem empate na view.
-- Mesma observação: substitui o índice automático da FK cod_pessoa.
CREATE UNIQUE INDEX idx_avaliacao_atleta_data
    ON AVALIACAOFISICA (cod_pessoa, data_avaliacao);


-- =====================================================================
-- 2) VISÕES
-- =====================================================================

-- View 1: vw_ficha_atleta
-- Justificativa: o minimundo diz que as informações dos atletas
-- ficavam espalhadas em planilhas. Esta view junta, numa linha por
-- atleta, tudo que a comissão técnica olha antes de convocar:
-- dados pessoais, categoria, de quem é reserva, a última avaliação
-- física (com IMC), a frequência nos treinos e o histórico em jogos.
-- Atletas sem avaliação, sem treino ou sem jogo continuam aparecendo
-- (LEFT JOIN), com zero ou NULL nesses campos.
-- Joins: PESSOA, CATEGORIA, PESSOA do titular, AVALIACAOFISICA,
-- resumo de PARTICIPA e resumo de ESCALACAO (6 joins).
CREATE OR REPLACE VIEW vw_ficha_atleta AS
SELECT a.cod_pessoa,
       p.nome                                        AS atleta,
       c.cod_categoria,
       c.nome_categoria                              AS categoria,
       a.num_camisa                                  AS camisa,
       a.pe_dominante,
       TIMESTAMPDIFF(YEAR, p.data_nasc, CURDATE())   AS idade,
       titular.nome                                  AS reserva_de,
       af.data_avaliacao                             AS ultima_avaliacao,
       af.vo2_max,
       af.percentual_gordura,
       ROUND(af.peso / POWER(af.altura / 100, 2), 1) AS imc,
       COALESCE(freq.treinos_registrados, 0)         AS treinos_registrados,
       COALESCE(freq.presencas, 0)                   AS presencas,
       COALESCE(freq.faltas, 0)                      AS faltas,
       ROUND(100 * freq.presencas / freq.treinos_registrados, 1) AS pct_presenca,
       COALESCE(jogos.jogos, 0)                      AS jogos,
       COALESCE(jogos.jogos_titular, 0)              AS jogos_titular,
       COALESCE(jogos.minutos, 0)                    AS minutos_jogados,
       COALESCE(jogos.amarelos, 0)                   AS cartoes_amarelos,
       COALESCE(jogos.vermelhos, 0)                  AS cartoes_vermelhos
FROM ATLETA a
JOIN PESSOA p    ON p.cod_pessoa = a.cod_pessoa
JOIN CATEGORIA c ON c.cod_categoria = a.cod_categoria
-- substituto_de guarda o código do TITULAR deste atleta
LEFT JOIN PESSOA titular ON titular.cod_pessoa = a.substituto_de
-- última avaliação: a subconsulta acha a data mais recente do atleta
-- e o JOIN busca a avaliação daquele atleta naquela data.
-- As duas buscas usam idx_avaliacao_atleta_data.
LEFT JOIN AVALIACAOFISICA af
       ON af.cod_pessoa = a.cod_pessoa
      AND af.data_avaliacao = (SELECT MAX(af2.data_avaliacao)
                                 FROM AVALIACAOFISICA af2
                                WHERE af2.cod_pessoa = a.cod_pessoa)
-- frequência por atleta: usa idx_participa_atleta_presenca
LEFT JOIN (SELECT cod_pessoa,
                  COUNT(*)          AS treinos_registrados,
                  SUM(presente)     AS presencas,
                  SUM(NOT presente) AS faltas
             FROM PARTICIPA
            GROUP BY cod_pessoa) freq
       ON freq.cod_pessoa = a.cod_pessoa
-- histórico em jogos por atleta
LEFT JOIN (SELECT cod_pessoa,
                  COUNT(*)             AS jogos,
                  SUM(titular)         AS jogos_titular,
                  SUM(minutos_jogados) AS minutos,
                  SUM(c_amarelo)       AS amarelos,
                  SUM(c_vermelho)      AS vermelhos
             FROM ESCALACAO
            GROUP BY cod_pessoa) jogos
       ON jogos.cod_pessoa = a.cod_pessoa;

-- View 2: vw_resultado_jogos
-- Justificativa: cada jogo hoje está espalhado em JOGO (placar),
-- ADVERSARIO (clube e cidade), CATEGORIA (quem jogou) e ESCALACAO
-- (quantos atletas entraram e quantos cartões). Esta view é a
-- "súmula" de cada partida em uma linha, já com resultado escrito,
-- saldo e pontos (3 vitória, 1 empate, 0 derrota). Com os pontos dá
-- para calcular aproveitamento por categoria ou por período, que é
-- o que o dashboard da Etapa 06 vai precisar.
-- Jogos sem escalação lançada aparecem com 0 atletas (LEFT JOIN).
-- Joins: CATEGORIA, ADVERSARIO e resumo de ESCALACAO (3 joins).
CREATE OR REPLACE VIEW vw_resultado_jogos AS
SELECT j.cod_jogo,
       j.data_jogo,
       c.cod_categoria,
       c.nome_categoria                       AS categoria,
       ad.nome_clube                          AS adversario,
       ad.cidade                              AS cidade_adversario,
       IF(j.mandante, 'Casa', 'Fora')         AS mando,
       j.gols_pro,
       j.gols_contra,
       j.gols_pro - j.gols_contra             AS saldo,
       CASE
           WHEN j.gols_pro > j.gols_contra THEN 'Vitória'
           WHEN j.gols_pro = j.gols_contra THEN 'Empate'
           ELSE 'Derrota'
       END                                    AS resultado,
       CASE
           WHEN j.gols_pro > j.gols_contra THEN 3
           WHEN j.gols_pro = j.gols_contra THEN 1
           ELSE 0
       END                                    AS pontos,
       COALESCE(esc.atletas, 0)               AS atletas_escalados,
       COALESCE(esc.amarelos, 0)              AS cartoes_amarelos,
       COALESCE(esc.vermelhos, 0)             AS cartoes_vermelhos
FROM JOGO j
JOIN CATEGORIA c   ON c.cod_categoria = j.cod_categoria
JOIN ADVERSARIO ad ON ad.cod_adversario = j.cod_adversario
LEFT JOIN (SELECT cod_jogo,
                  COUNT(*)        AS atletas,
                  SUM(c_amarelo)  AS amarelos,
                  SUM(c_vermelho) AS vermelhos
             FROM ESCALACAO
            GROUP BY cod_jogo) esc
       ON esc.cod_jogo = j.cod_jogo;


-- =====================================================================
-- 3) CONSULTAS
-- =====================================================================

-- Consulta 1 - ANTI JOIN (esquerda): treinos sem sessões planejadas
-- Todo treino deveria ter um plano (aquecimento, técnica, tática...)
-- em SESSAOTREINO. Esta consulta mostra os treinos cadastrados que
-- ainda não têm nenhuma sessão, para a comissão completar o plano.
-- Como funciona: o LEFT JOIN mantém todos os treinos; quando o treino
-- não tem sessão, as colunas de SESSAOTREINO vêm NULL. O WHERE
-- s.cod_treino IS NULL deixa SÓ esses treinos sem par.
SELECT t.cod_treino,
       t.data_treino,
       t.turno,
       t.local,
       c.nome_categoria AS categoria
FROM TREINO t
JOIN CATEGORIA c ON c.cod_categoria = t.cod_categoria
LEFT JOIN SESSAOTREINO s ON s.cod_treino = t.cod_treino
WHERE s.cod_treino IS NULL
ORDER BY t.data_treino, c.idade_min;

-- Consulta 2 - FULL OUTER JOIN: relatório disciplinar dos atletas
-- Junta dois problemas que a comissão acompanha: faltas em treino e
-- cartões em jogo. Aparece quem tem só falta, quem tem só cartão e
-- quem tem os dois.
-- O MySQL NÃO tem FULL OUTER JOIN. Ele é simulado assim:
--   LEFT JOIN  -> todos que têm falta (com ou sem cartão)
--   RIGHT JOIN -> todos que têm cartão (com ou sem falta)
--   UNION      -> junta as duas listas e remove as linhas repetidas
--                 (quem tem os dois aparece nas duas metades).
-- O WITH cria duas tabelas temporárias com nome, só para esta consulta.
WITH faltas AS (
    -- usa idx_participa_atleta_presenca
    SELECT cod_pessoa, COUNT(*) AS qtd_faltas
      FROM PARTICIPA
     WHERE presente = FALSE
     GROUP BY cod_pessoa
),
cartoes AS (
    SELECT cod_pessoa,
           SUM(c_amarelo)  AS amarelos,
           SUM(c_vermelho) AS vermelhos
      FROM ESCALACAO
     WHERE c_amarelo = TRUE OR c_vermelho = TRUE
     GROUP BY cod_pessoa
),
disciplina AS (
    SELECT f.cod_pessoa AS cod_pessoa, f.qtd_faltas, ca.amarelos, ca.vermelhos
      FROM faltas f
      LEFT JOIN cartoes ca ON ca.cod_pessoa = f.cod_pessoa
    UNION
    SELECT ca.cod_pessoa AS cod_pessoa, f.qtd_faltas, ca.amarelos, ca.vermelhos
      FROM faltas f
     RIGHT JOIN cartoes ca ON ca.cod_pessoa = f.cod_pessoa
)
SELECT p.nome           AS atleta,
       c.nome_categoria AS categoria,
       COALESCE(d.qtd_faltas, 0) AS faltas,
       COALESCE(d.amarelos, 0)   AS cartoes_amarelos,
       COALESCE(d.vermelhos, 0)  AS cartoes_vermelhos,
       CASE
           WHEN d.qtd_faltas IS NOT NULL AND d.amarelos IS NOT NULL THEN 'Falta e cartão'
           WHEN d.qtd_faltas IS NOT NULL THEN 'Só falta'
           ELSE 'Só cartão'
       END AS situacao
FROM disciplina d
JOIN ATLETA a    ON a.cod_pessoa = d.cod_pessoa
JOIN PESSOA p    ON p.cod_pessoa = a.cod_pessoa
JOIN CATEGORIA c ON c.cod_categoria = a.cod_categoria
ORDER BY situacao, c.idade_min, p.nome;

-- Consulta 3 - SUBCONSULTA (com ANY): candidatos a subir de categoria
-- Na base, um atleta pode treinar com a categoria de cima quando já
-- tem condicionamento compatível com ela. Aqui: atletas cujo VO2 máx
-- é MAIOR que o de PELO MENOS UM atleta da categoria imediatamente
-- acima (> ANY).
-- Subconsulta interna: descobre qual é a categoria de cima (a de
-- menor idade_min que ainda seja maior que a do atleta).
-- Subconsulta do meio: lista os VO2 dos atletas dessa categoria.
-- O Sub-20 não tem categoria acima: a lista vem vazia e "> ANY" de
-- lista vazia é falso, então ninguém do Sub-20 aparece.
SELECT c.nome_categoria AS categoria_atual,
       p.nome           AS atleta,
       af.vo2_max,
       (SELECT c3.nome_categoria
          FROM CATEGORIA c3
         WHERE c3.idade_min > c.idade_min
         ORDER BY c3.idade_min
         LIMIT 1)       AS categoria_acima
FROM AVALIACAOFISICA af
JOIN ATLETA a    ON a.cod_pessoa = af.cod_pessoa
JOIN PESSOA p    ON p.cod_pessoa = a.cod_pessoa
JOIN CATEGORIA c ON c.cod_categoria = a.cod_categoria
WHERE af.vo2_max > ANY (
        SELECT af2.vo2_max
          FROM AVALIACAOFISICA af2
          JOIN ATLETA a2 ON a2.cod_pessoa = af2.cod_pessoa
         WHERE a2.cod_categoria = (SELECT c2.cod_categoria
                                     FROM CATEGORIA c2
                                    WHERE c2.idade_min > c.idade_min
                                    ORDER BY c2.idade_min
                                    LIMIT 1))
ORDER BY c.idade_min, af.vo2_max DESC;

-- Consulta 4 - SUBCONSULTAS NO SELECT: raio-x de cada categoria
-- Uma linha por categoria com o tamanho do elenco, quantos membros da
-- comissão a orientam, quantos atletas cada membro acompanha, quantos
-- treinos e jogos ela teve e quantas faltas os atletas dela somam.
-- Cada coluna é uma subconsulta correlacionada: ela roda uma vez para
-- cada categoria, usando c.cod_categoria da consulta de fora.
-- Fazer isso com JOIN + GROUP BY multiplicaria as linhas (atletas x
-- treinos x jogos) e as contagens sairiam erradas.
SELECT c.nome_categoria AS categoria,
       (SELECT COUNT(*) FROM ATLETA a
         WHERE a.cod_categoria = c.cod_categoria)  AS atletas,
       (SELECT COUNT(*) FROM ORIENTA o
         WHERE o.cod_categoria = c.cod_categoria)  AS membros_comissao,
       ROUND((SELECT COUNT(*) FROM ATLETA a
               WHERE a.cod_categoria = c.cod_categoria)
           / NULLIF((SELECT COUNT(*) FROM ORIENTA o
                      WHERE o.cod_categoria = c.cod_categoria), 0), 1)
                                                   AS atletas_por_membro,
       (SELECT COUNT(*) FROM TREINO t
         WHERE t.cod_categoria = c.cod_categoria)  AS treinos,
       (SELECT COUNT(*) FROM JOGO j
         WHERE j.cod_categoria = c.cod_categoria)  AS jogos,
       -- usa idx_participa_atleta_presenca
       (SELECT COUNT(*) FROM PARTICIPA pa
          JOIN ATLETA a ON a.cod_pessoa = pa.cod_pessoa
         WHERE a.cod_categoria = c.cod_categoria
           AND pa.presente = FALSE)                AS faltas_em_treino
FROM CATEGORIA c
ORDER BY c.idade_min;


-- =====================================================================
-- 4) EXEMPLOS DE USO DAS VIEWS
-- =====================================================================

-- Ficha de todos os atletas do Sub-15
SELECT atleta, camisa, reserva_de, vo2_max, imc, pct_presenca, jogos, minutos_jogados
  FROM vw_ficha_atleta
 WHERE categoria = 'Sub-15'
 ORDER BY camisa;

-- Aproveitamento por categoria (pontos ganhos / pontos possíveis)
SELECT categoria,
       COUNT(*)    AS jogos,
       SUM(pontos) AS pontos,
       ROUND(100 * SUM(pontos) / (3 * COUNT(*)), 1) AS aproveitamento_pct
  FROM vw_resultado_jogos
 GROUP BY cod_categoria, categoria
 ORDER BY aproveitamento_pct DESC;


-- =====================================================================
-- 5) PROVA DE USO DOS ÍNDICES (EXPLAIN)
-- Procure o nome do índice na coluna "key" do resultado.
-- =====================================================================

-- idx_avaliacao_atleta_data: última avaliação de um atleta
EXPLAIN
SELECT vo2_max, data_avaliacao
  FROM AVALIACAOFISICA
 WHERE cod_pessoa = 1
 ORDER BY data_avaliacao DESC
 LIMIT 1;

-- idx_participa_atleta_presenca: frequência por atleta
EXPLAIN
SELECT cod_pessoa, COUNT(*), SUM(presente)
  FROM PARTICIPA
 GROUP BY cod_pessoa;

-- Os dois índices dentro da view de ficha do atleta
EXPLAIN SELECT * FROM vw_ficha_atleta;