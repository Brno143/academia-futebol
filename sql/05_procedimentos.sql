-- Entrega 05 - SQL dos procedimentos


USE academia_futebol;

DROP PROCEDURE IF EXISTS sp_atualizar_placar;
DROP PROCEDURE IF EXISTS sp_sequencia_invicta;

DELIMITER $$

-- sp_atualizar_placar (atualização de dados)
-- Usado depois da partida para lançar ou corrigir o placar de um jogo.
-- Antes de atualizar, confere se o jogo existe e se o placar é válido.
CREATE PROCEDURE sp_atualizar_placar(
    IN p_cod_jogo    INT,
    IN p_gols_pro    SMALLINT,
    IN p_gols_contra SMALLINT
)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM JOGO WHERE cod_jogo = p_cod_jogo) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Jogo não encontrado.';
    END IF;

    IF p_gols_pro < 0 OR p_gols_contra < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'O placar não pode ser negativo.';
    END IF;

    UPDATE JOGO
       SET gols_pro    = p_gols_pro,
           gols_contra = p_gols_contra
     WHERE cod_jogo = p_cod_jogo;
END$$

-- sp_sequencia_invicta (com CURSOR)
-- Mostra, para cada categoria, a maior sequência de jogos sem perder
-- e a sequência atual (contando a partir do último jogo).
-- Precisa do cursor porque cada jogo depende dos anteriores da mesma
-- categoria: vitória ou empate soma 1 na sequência, derrota zera.
-- Os jogos são lidos um por um, em ordem de data. Um UPDATE trata cada
-- linha sozinha e não consegue carregar esse valor de um jogo para o outro.
CREATE PROCEDURE sp_sequencia_invicta()
BEGIN
    DECLARE v_fim INT DEFAULT 0;
    DECLARE v_categoria INT;
    DECLARE v_gols_pro SMALLINT;
    DECLARE v_gols_contra SMALLINT;
    DECLARE v_categoria_atual INT DEFAULT NULL;
    DECLARE v_seq_atual INT DEFAULT 0;
    DECLARE v_seq_maior INT DEFAULT 0;

    DECLARE cur_jogos CURSOR FOR
        SELECT cod_categoria, gols_pro, gols_contra
          FROM JOGO
         ORDER BY cod_categoria, data_jogo, cod_jogo;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_fim = 1;

    DROP TEMPORARY TABLE IF EXISTS tmp_sequencia;
    CREATE TEMPORARY TABLE tmp_sequencia (
        cod_categoria   INT,
        maior_sequencia INT,
        sequencia_atual INT
    );

    OPEN cur_jogos;

    leitura: LOOP
        FETCH cur_jogos INTO v_categoria, v_gols_pro, v_gols_contra;
        IF v_fim = 1 THEN
            LEAVE leitura;
        END IF;

        -- mudou de categoria: guarda o resultado da anterior e recomeça
        IF v_categoria_atual IS NULL OR v_categoria <> v_categoria_atual THEN
            IF v_categoria_atual IS NOT NULL THEN
                INSERT INTO tmp_sequencia VALUES (v_categoria_atual, v_seq_maior, v_seq_atual);
            END IF;
            SET v_categoria_atual = v_categoria;
            SET v_seq_atual = 0;
            SET v_seq_maior = 0;
        END IF;

        IF v_gols_pro >= v_gols_contra THEN
            SET v_seq_atual = v_seq_atual + 1;
            IF v_seq_atual > v_seq_maior THEN
                SET v_seq_maior = v_seq_atual;
            END IF;
        ELSE
            SET v_seq_atual = 0;
        END IF;
    END LOOP;

    CLOSE cur_jogos;

    IF v_categoria_atual IS NOT NULL THEN
        INSERT INTO tmp_sequencia VALUES (v_categoria_atual, v_seq_maior, v_seq_atual);
    END IF;

    SELECT c.nome_categoria                AS categoria,
           COALESCE(t.maior_sequencia, 0)  AS maior_sequencia_invicta,
           COALESCE(t.sequencia_atual, 0)  AS sequencia_atual
      FROM CATEGORIA c
      LEFT JOIN tmp_sequencia t ON t.cod_categoria = c.cod_categoria
     ORDER BY c.idade_min;

    DROP TEMPORARY TABLE tmp_sequencia;
END$$

DELIMITER ;

-- Exemplos de uso
CALL sp_sequencia_invicta();

-- Lança o placar do jogo 8 (0 x 2)
CALL sp_atualizar_placar(8, 0, 2);
SELECT cod_jogo, gols_pro, gols_contra FROM JOGO WHERE cod_jogo = 8;