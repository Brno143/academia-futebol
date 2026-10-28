-- Etapa 03 - Consultas do dashboard (MySQL)
USE academia_futebol;

-- 1. Elenco completo com substitutos
-- Lista todos os atletas com categoria, idade e de quem cada um é reserva.
-- Conceitos: JOIN, LEFT JOIN, auto-relacionamento (ATLETA x ATLETA), função de data
SELECT c.nome_categoria AS categoria,
       a.num_camisa AS camisa,
       p.nome AS atleta,
       a.pe_dominante,
       TIMESTAMPDIFF(YEAR, p.data_nasc, CURDATE()) AS idade,
       ps.nome AS reserva_de
FROM ATLETA a
JOIN PESSOA p ON p.cod_pessoa = a.cod_pessoa
JOIN CATEGORIA c ON c.cod_categoria = a.cod_categoria
LEFT JOIN ATLETA s ON s.cod_pessoa = a.substituto_de
LEFT JOIN PESSOA ps ON ps.cod_pessoa = s.cod_pessoa
ORDER BY c.idade_min, a.num_camisa;

-- 2. Frequência nos treinos por categoria
-- Presenças, faltas e percentual de presença de cada categoria.
-- Conceitos: JOIN com 3 tabelas, GROUP BY, SUM, AVG, ROUND
SELECT c.nome_categoria AS categoria,
       COUNT(*) AS registros,
       SUM(pa.presente) AS presencas,
       SUM(NOT pa.presente) AS faltas,
       ROUND(100 * AVG(pa.presente), 1) AS pct_presenca
FROM PARTICIPA pa
JOIN TREINO t ON t.cod_treino = pa.cod_treino
JOIN CATEGORIA c ON c.cod_categoria = t.cod_categoria
GROUP BY c.cod_categoria, c.nome_categoria
ORDER BY c.cod_categoria;

-- 3. Atletas com VO2 acima da média da própria categoria
-- Compara cada atleta com a média de VO2 máx da categoria dele.
-- Conceitos: JOIN, subconsulta correlacionada (no SELECT e no WHERE), AVG
SELECT c.nome_categoria AS categoria,
       p.nome AS atleta,
       af.vo2_max,
       (SELECT ROUND(AVG(af2.vo2_max), 2)
          FROM AVALIACAOFISICA af2
          JOIN ATLETA a2 ON a2.cod_pessoa = af2.cod_pessoa
         WHERE a2.cod_categoria = a.cod_categoria) AS media_categoria
FROM AVALIACAOFISICA af
JOIN ATLETA a ON a.cod_pessoa = af.cod_pessoa
JOIN PESSOA p ON p.cod_pessoa = a.cod_pessoa
JOIN CATEGORIA c ON c.cod_categoria = a.cod_categoria
WHERE af.vo2_max > (SELECT AVG(af3.vo2_max)
                      FROM AVALIACAOFISICA af3
                      JOIN ATLETA a3 ON a3.cod_pessoa = af3.cod_pessoa
                     WHERE a3.cod_categoria = a.cod_categoria)
ORDER BY c.cod_categoria, af.vo2_max DESC;

-- 4. Resultados por cidade do adversário
-- Vitórias, empates, derrotas e saldo de gols agrupados pela cidade do adversário.
-- Conceitos: JOIN, GROUP BY, SUM com expressão lógica
SELECT ad.cidade,
       COUNT(*) AS jogos,
       SUM(j.gols_pro > j.gols_contra) AS vitorias,
       SUM(j.gols_pro = j.gols_contra) AS empates,
       SUM(j.gols_pro < j.gols_contra) AS derrotas,
       SUM(j.gols_pro) - SUM(j.gols_contra) AS saldo_gols
FROM JOGO j
JOIN ADVERSARIO ad ON ad.cod_adversario = j.cod_adversario
GROUP BY ad.cidade
ORDER BY vitorias DESC, saldo_gols DESC;

-- 5. Jogos com mais gols que a média
-- Jogos em que o time marcou acima da média de gols pró, com o resultado.
-- Conceitos: JOIN, subconsulta no WHERE, CASE, CONCAT
SELECT j.data_jogo,
       ad.nome_clube AS adversario,
       CASE WHEN j.mandante THEN 'Casa' ELSE 'Fora' END AS mando,
       CONCAT(j.gols_pro, ' x ', j.gols_contra) AS placar,
       CASE
           WHEN j.gols_pro > j.gols_contra THEN 'Vitória'
           WHEN j.gols_pro = j.gols_contra THEN 'Empate'
           ELSE 'Derrota'
       END AS resultado
FROM JOGO j
JOIN ADVERSARIO ad ON ad.cod_adversario = j.cod_adversario
WHERE j.gols_pro > (SELECT AVG(gols_pro) FROM JOGO)
ORDER BY j.gols_pro DESC, j.data_jogo;

-- 6. Titulares que faltaram treino
-- Atletas que começaram jogando mesmo tendo falta registrada em treino, com o motivo.
-- Conceitos: JOIN com 5 tabelas, EXISTS, subconsulta correlacionada, GROUP_CONCAT
SELECT p.nome AS atleta,
       c.nome_categoria AS categoria,
       j.data_jogo,
       ad.nome_clube AS adversario,
       (SELECT GROUP_CONCAT(pa.justifica_falta SEPARATOR '; ')
          FROM PARTICIPA pa
         WHERE pa.cod_pessoa = e.cod_pessoa AND pa.presente = FALSE) AS motivo_falta
FROM ESCALACAO e
JOIN ATLETA a ON a.cod_pessoa = e.cod_pessoa
JOIN PESSOA p ON p.cod_pessoa = a.cod_pessoa
JOIN CATEGORIA c ON c.cod_categoria = a.cod_categoria
JOIN JOGO j ON j.cod_jogo = e.cod_jogo
JOIN ADVERSARIO ad ON ad.cod_adversario = j.cod_adversario
WHERE e.titular = TRUE
  AND EXISTS (SELECT 1 FROM PARTICIPA pa2
               WHERE pa2.cod_pessoa = e.cod_pessoa AND pa2.presente = FALSE)
ORDER BY c.cod_categoria, p.nome;

-- 7. Minutos e cartões por setor do campo
-- Quanto cada setor (defesa, meio, ataque) jogou e quantos cartões levou.
-- Conceitos: JOIN, GROUP BY, COUNT DISTINCT, SUM
SELECT po.setor,
       COUNT(DISTINCT e.cod_pessoa) AS atletas,
       SUM(e.minutos_jogados) AS minutos_jogados,
       SUM(e.c_amarelo) AS amarelos,
       SUM(e.c_vermelho) AS vermelhos
FROM ESCALACAO e
JOIN POSICAO po ON po.cod_posicao = e.cod_posicao
GROUP BY po.setor
ORDER BY minutos_jogados DESC;

-- 8. Tempo de treino por categoria e tipo de sessão
-- Minutos gastos em cada tipo de sessão e quanto isso representa do total da categoria.
-- Conceitos: JOIN com entidade fraca (SESSAOTREINO), GROUP BY, subconsulta correlacionada
SELECT c.nome_categoria AS categoria,
       s.tipo_sessao,
       COUNT(*) AS sessoes,
       SUM(s.duracao_min) AS minutos,
       ROUND(100 * SUM(s.duracao_min) /
             (SELECT SUM(s2.duracao_min)
                FROM SESSAOTREINO s2
                JOIN TREINO t2 ON t2.cod_treino = s2.cod_treino
               WHERE t2.cod_categoria = c.cod_categoria), 1) AS pct_do_total
FROM SESSAOTREINO s
JOIN TREINO t ON t.cod_treino = s.cod_treino
JOIN CATEGORIA c ON c.cod_categoria = t.cod_categoria
GROUP BY c.cod_categoria, c.nome_categoria, s.tipo_sessao
ORDER BY c.cod_categoria, minutos DESC;

-- 9. Comissão técnica que orienta mais de uma categoria
-- Membros da comissão que trabalham com várias categorias, e quais são.
-- Conceitos: JOIN com tabela associativa (ORIENTA), subconsulta com IN + HAVING, GROUP_CONCAT
SELECT p.nome,
       m.funcao,
       COUNT(*) AS qtd_categorias,
       GROUP_CONCAT(c.nome_categoria ORDER BY c.idade_min SEPARATOR ', ') AS categorias
FROM ORIENTA o
JOIN MEMBROCOMISSAO m ON m.cod_pessoa = o.cod_pessoa
JOIN PESSOA p ON p.cod_pessoa = m.cod_pessoa
JOIN CATEGORIA c ON c.cod_categoria = o.cod_categoria
WHERE o.cod_pessoa IN (SELECT cod_pessoa
                         FROM ORIENTA
                        GROUP BY cod_pessoa
                       HAVING COUNT(*) > 1)
GROUP BY m.cod_pessoa, p.nome, m.funcao
ORDER BY qtd_categorias DESC, p.nome;

-- 10. Destaque físico de cada categoria
-- O atleta com maior VO2 máx de cada categoria, com % de gordura e IMC.
-- Conceitos: JOIN com subconsulta no FROM (tabela derivada), MAX, cálculo de IMC
SELECT c.nome_categoria AS categoria,
       p.nome AS atleta,
       af.vo2_max,
       af.percentual_gordura,
       ROUND(af.peso / POWER(af.altura / 100, 2), 1) AS imc
FROM AVALIACAOFISICA af
JOIN ATLETA a ON a.cod_pessoa = af.cod_pessoa
JOIN PESSOA p ON p.cod_pessoa = a.cod_pessoa
JOIN CATEGORIA c ON c.cod_categoria = a.cod_categoria
JOIN (SELECT a2.cod_categoria, MAX(af2.vo2_max) AS max_vo2
        FROM AVALIACAOFISICA af2
        JOIN ATLETA a2 ON a2.cod_pessoa = af2.cod_pessoa
       GROUP BY a2.cod_categoria) m
  ON m.cod_categoria = a.cod_categoria AND af.vo2_max = m.max_vo2
ORDER BY c.cod_categoria;
