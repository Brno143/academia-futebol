-- Entrega 05 - SQL das funções


USE academia_futebol;

DROP FUNCTION IF EXISTS fn_aproveitamento_categoria;
DROP FUNCTION IF EXISTS fn_situacao_frequencia;

DELIMITER $$

-- fn_aproveitamento_categoria
-- Aproveitamento é a medida usada no futebol para comparar desempenho:
-- pontos ganhos divididos pelos pontos possíveis (3 por jogo), em %.
-- Recebe a categoria e devolve o aproveitamento dela nos jogos.
-- Se a categoria ainda não jogou, devolve NULL (evita divisão por zero).
CREATE FUNCTION fn_aproveitamento_categoria(p_cod_categoria INT)
RETURNS DECIMAL(5,1)
READS SQL DATA
BEGIN
    DECLARE v_jogos INT;
    DECLARE v_pontos INT;

    SELECT COUNT(*),
           SUM(CASE
                   WHEN gols_pro > gols_contra THEN 3
                   WHEN gols_pro = gols_contra THEN 1
                   ELSE 0
               END)
      INTO v_jogos, v_pontos
      FROM JOGO
     WHERE cod_categoria = p_cod_categoria;

    IF v_jogos = 0 THEN
        RETURN NULL;
    END IF;

    RETURN ROUND(100 * v_pontos / (3 * v_jogos), 1);
END$$

-- fn_situacao_frequencia
-- A comissão acompanha a frequência dos atletas nos treinos.
-- Recebe o atleta e devolve a situação dele pelo percentual de presença:
--   75% ou mais  -> 'Regular'
--   50% a 74,9%  -> 'Atenção'
--   abaixo de 50% -> 'Crítica'
--   sem registro de treino -> 'Sem registro'
CREATE FUNCTION fn_situacao_frequencia(p_cod_pessoa INT)
RETURNS VARCHAR(20)
READS SQL DATA
BEGIN
    DECLARE v_total INT;
    DECLARE v_presencas INT;
    DECLARE v_pct DECIMAL(5,1);

    SELECT COUNT(*), SUM(presente)
      INTO v_total, v_presencas
      FROM PARTICIPA
     WHERE cod_pessoa = p_cod_pessoa;

    IF v_total = 0 THEN
        RETURN 'Sem registro';
    END IF;

    SET v_pct = 100 * v_presencas / v_total;

    IF v_pct >= 75 THEN
        RETURN 'Regular';
    ELSEIF v_pct >= 50 THEN
        RETURN 'Atenção';
    ELSE
        RETURN 'Crítica';
    END IF;
END$$

DELIMITER ;

-- Exemplos de uso
SELECT nome_categoria AS categoria,
       fn_aproveitamento_categoria(cod_categoria) AS aproveitamento_pct
  FROM CATEGORIA
 ORDER BY idade_min;

SELECT p.nome AS atleta,
       c.nome_categoria AS categoria,
       fn_situacao_frequencia(a.cod_pessoa) AS frequencia
  FROM ATLETA a
  JOIN PESSOA p    ON p.cod_pessoa = a.cod_pessoa
  JOIN CATEGORIA c ON c.cod_categoria = a.cod_categoria
 ORDER BY c.idade_min, p.nome;