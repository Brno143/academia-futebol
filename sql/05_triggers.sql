-- Entrega 05 - SQL dos triggers


USE academia_futebol;

DROP TRIGGER IF EXISTS trg_log_placar;
DROP TRIGGER IF EXISTS trg_valida_categoria_escalacao;

-- Tabela de log das alterações de placar
CREATE TABLE IF NOT EXISTS LOG_PLACAR (
    cod_log            INT AUTO_INCREMENT PRIMARY KEY,
    cod_jogo           INT NOT NULL,
    gols_pro_antes     SMALLINT NOT NULL,
    gols_contra_antes  SMALLINT NOT NULL,
    gols_pro_depois    SMALLINT NOT NULL,
    gols_contra_depois SMALLINT NOT NULL,
    usuario            VARCHAR(100) NOT NULL,
    data_alteracao     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DELIMITER $$

-- trg_log_placar (atualiza a tabela de log)
-- O placar é o dado oficial do jogo e entra nos cálculos de resultado
-- e aproveitamento. Toda vez que ele é alterado, o trigger guarda o
-- placar antigo, o novo, quem alterou e quando. Se o UPDATE mudar outra
-- coluna do jogo e não mexer no placar, nada é registrado.
CREATE TRIGGER trg_log_placar
AFTER UPDATE ON JOGO
FOR EACH ROW
BEGIN
    IF OLD.gols_pro <> NEW.gols_pro OR OLD.gols_contra <> NEW.gols_contra THEN
        INSERT INTO LOG_PLACAR (cod_jogo, gols_pro_antes, gols_contra_antes,
                                gols_pro_depois, gols_contra_depois, usuario)
        VALUES (NEW.cod_jogo, OLD.gols_pro, OLD.gols_contra,
                NEW.gols_pro, NEW.gols_contra, CURRENT_USER());
    END IF;
END$$

-- trg_valida_categoria_escalacao
-- Um atleta só pode ser escalado em jogo da categoria dele. A FK de
-- ESCALACAO garante que o atleta e o jogo existem, mas não compara as
-- categorias. Este trigger bloqueia a escalação quando elas são diferentes.
CREATE TRIGGER trg_valida_categoria_escalacao
BEFORE INSERT ON ESCALACAO
FOR EACH ROW
BEGIN
    DECLARE v_cat_atleta INT;
    DECLARE v_cat_jogo INT;

    SELECT cod_categoria INTO v_cat_atleta FROM ATLETA WHERE cod_pessoa = NEW.cod_pessoa;
    SELECT cod_categoria INTO v_cat_jogo   FROM JOGO   WHERE cod_jogo   = NEW.cod_jogo;

    IF v_cat_atleta <> v_cat_jogo THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Atleta não pertence à categoria deste jogo.';
    END IF;
END$$

DELIMITER ;

-- Exemplos de uso
-- 1) Altera o placar do jogo 1, volta ao original e confere o log
UPDATE JOGO SET gols_pro = 4 WHERE cod_jogo = 1;
UPDATE JOGO SET gols_pro = 3 WHERE cod_jogo = 1;
SELECT * FROM LOG_PLACAR;

-- 2) Tenta escalar um atleta do Sub-11 (cod 1) no jogo 2, que é do Sub-13.
--    Deve dar o erro 'Atleta não pertence à categoria deste jogo.'
-- INSERT INTO ESCALACAO (cod_pessoa, cod_jogo, cod_posicao, minutos_jogados, c_amarelo, c_vermelho, titular)
-- VALUES (1, 2, 1, 90, FALSE, FALSE, TRUE);