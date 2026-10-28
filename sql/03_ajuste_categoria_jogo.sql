-- =====================================================================
-- Ajuste feito na Etapa 03: categoria do jogo
-- Motivo: a tabela JOGO não indicava qual categoria (Sub-11 a Sub-20)
-- disputou cada partida. Adicionamos a FK cod_categoria em JOGO.
-- Rodar DEPOIS dos scripts da Etapa 02 (criação + inserção).
-- =====================================================================
USE academia_futebol;

-- 1) Nova coluna (começa aceitando NULL pra poder preencher os jogos que já existem)
ALTER TABLE JOGO ADD COLUMN cod_categoria INT NULL;

-- 2) Preenche os 30 jogos já cadastrados, 6 jogos por categoria.
--    Jogos 1 a 5 batem com a escalação já registrada
--    (jogo 1 = Sub-11, 2 = Sub-13, 3 = Sub-15, 4 = Sub-17, 5 = Sub-20)
--    e os demais seguem a mesma sequência.
UPDATE JOGO
SET cod_categoria = ((cod_jogo - 1) MOD 5) + 1
WHERE cod_jogo > 0;

-- 3) Agora a coluna passa a ser obrigatória e ligada a CATEGORIA
ALTER TABLE JOGO MODIFY cod_categoria INT NOT NULL;
ALTER TABLE JOGO ADD CONSTRAINT fk_jogo_categoria
    FOREIGN KEY (cod_categoria) REFERENCES CATEGORIA(cod_categoria) ON DELETE RESTRICT;

-- Conferência: 6 jogos por categoria
SELECT c.nome_categoria, COUNT(*) AS jogos
FROM JOGO j
JOIN CATEGORIA c ON c.cod_categoria = j.cod_categoria
GROUP BY c.cod_categoria, c.nome_categoria
ORDER BY c.cod_categoria;
