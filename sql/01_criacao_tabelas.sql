-- Etapa 02 - Criação das tabelas (academia_futebol)


DROP SCHEMA IF EXISTS academia_futebol;
CREATE SCHEMA academia_futebol;
USE academia_futebol;

CREATE TABLE CATEGORIA (
   cod_categoria INT AUTO_INCREMENT PRIMARY KEY,
   nome_categoria VARCHAR(20) NOT NULL UNIQUE,
   idade_min SMALLINT NOT NULL CHECK (idade_min >= 0 AND idade_min <= 30),
   idade_max SMALLINT NOT NULL CHECK (idade_max <= 30),
   CONSTRAINT chk_categoria_idades CHECK (idade_max >= idade_min)
);

CREATE TABLE PESSOA (
   cod_pessoa INT AUTO_INCREMENT PRIMARY KEY,
   nome VARCHAR(100) NOT NULL,
   data_nasc DATE NOT NULL,
   rua VARCHAR(80),
   numero VARCHAR(10),
   bairro VARCHAR(50),
   cidade VARCHAR(50),
   cep VARCHAR(10)
);

CREATE TABLE TELEFONE (
   telefone VARCHAR(15),
   cod_pessoa INT NOT NULL,
   PRIMARY KEY (telefone, cod_pessoa),
   FOREIGN KEY (cod_pessoa) REFERENCES PESSOA(cod_pessoa) ON DELETE CASCADE
);

CREATE TABLE ATLETA (
   cod_pessoa INT PRIMARY KEY,
   pe_dominante VARCHAR(10) CHECK (pe_dominante IN ('Direito', 'Esquerdo')),
   num_camisa SMALLINT NOT NULL CHECK (num_camisa >= 1 AND num_camisa <= 99),
   cod_categoria INT NOT NULL,
   substituto_de INT,
   FOREIGN KEY (cod_pessoa) REFERENCES PESSOA(cod_pessoa) ON DELETE CASCADE,
   FOREIGN KEY (cod_categoria) REFERENCES CATEGORIA(cod_categoria) ON DELETE
RESTRICT,
   FOREIGN KEY (substituto_de) REFERENCES ATLETA(cod_pessoa) ON DELETE SET NULL,
   CONSTRAINT uq_camisa_categoria UNIQUE (num_camisa, cod_categoria),
   CONSTRAINT uq_substituto UNIQUE (substituto_de)
);

CREATE TABLE MEMBROCOMISSAO (
   cod_pessoa INT PRIMARY KEY,
   funcao VARCHAR(30) NOT NULL,
   registro_cref VARCHAR(20) UNIQUE,
   FOREIGN KEY (cod_pessoa) REFERENCES PESSOA(cod_pessoa) ON DELETE CASCADE
);

CREATE TABLE POSICAO (
   cod_posicao INT AUTO_INCREMENT PRIMARY KEY,
   nome_posicao VARCHAR(30) NOT NULL UNIQUE,
   setor VARCHAR(20) NOT NULL CHECK (setor IN ('Defesa', 'Meio', 'Ataque'))
);

CREATE TABLE ADVERSARIO (
   cod_adversario INT AUTO_INCREMENT PRIMARY KEY,
   nome_clube VARCHAR(50) NOT NULL UNIQUE,
   cidade VARCHAR(50) NOT NULL
);

CREATE TABLE TREINO (
   cod_treino INT AUTO_INCREMENT PRIMARY KEY,
   turno VARCHAR(20) NOT NULL CHECK (turno IN ('Manhã', 'Tarde', 'Noite')),
   local VARCHAR(80) NOT NULL,
   data_treino DATE NOT NULL,
   cod_categoria INT NOT NULL,
   FOREIGN KEY (cod_categoria) REFERENCES CATEGORIA(cod_categoria) ON DELETE
RESTRICT
);

CREATE TABLE SESSAOTREINO (
   cod_treino INT,
   num_sessao SMALLINT,
   tipo_sessao VARCHAR(20) NOT NULL CHECK (tipo_sessao IN ('Aquecimento', 'Técnica', 'Tática',
'Condicionamento', 'Desaquecimento')),
   duracao_min SMALLINT NOT NULL CHECK (duracao_min > 0),
   PRIMARY KEY (cod_treino, num_sessao),
   FOREIGN KEY (cod_treino) REFERENCES TREINO(cod_treino) ON DELETE CASCADE ON
UPDATE CASCADE
);

CREATE TABLE PARTICIPA (
   cod_treino INT,
   cod_pessoa INT,
   presente BOOLEAN NOT NULL DEFAULT FALSE,
   justifica_falta VARCHAR(150),
   PRIMARY KEY (cod_treino, cod_pessoa),
   FOREIGN KEY (cod_treino) REFERENCES TREINO(cod_treino) ON DELETE CASCADE,
   FOREIGN KEY (cod_pessoa) REFERENCES ATLETA(cod_pessoa) ON DELETE CASCADE
);

CREATE TABLE JOGO (
   cod_jogo INT AUTO_INCREMENT PRIMARY KEY,
   data_jogo DATE NOT NULL,
   local_jogo VARCHAR(80) NOT NULL,
   mandante BOOLEAN NOT NULL,
   gols_pro SMALLINT NOT NULL DEFAULT 0 CHECK (gols_pro >= 0),
   gols_contra SMALLINT NOT NULL DEFAULT 0 CHECK (gols_contra >= 0),
   cod_adversario INT NOT NULL,
   FOREIGN KEY (cod_adversario) REFERENCES ADVERSARIO(cod_adversario) ON DELETE
RESTRICT
);

CREATE TABLE ESCALACAO (
   cod_pessoa INT,
   cod_jogo INT,
   cod_posicao INT NOT NULL,
   minutos_jogados SMALLINT NOT NULL DEFAULT 0 CHECK (minutos_jogados >= 0 AND
minutos_jogados <= 120),
   c_amarelo BOOLEAN NOT NULL DEFAULT FALSE,
   c_vermelho BOOLEAN NOT NULL DEFAULT FALSE,
   titular BOOLEAN NOT NULL DEFAULT FALSE,
   PRIMARY KEY (cod_pessoa, cod_jogo),
   FOREIGN KEY (cod_pessoa) REFERENCES ATLETA(cod_pessoa) ON DELETE CASCADE,
   FOREIGN KEY (cod_jogo) REFERENCES JOGO(cod_jogo) ON DELETE CASCADE,
   FOREIGN KEY (cod_posicao) REFERENCES POSICAO(cod_posicao) ON DELETE RESTRICT
);

CREATE TABLE AVALIACAOFISICA (
   cod_avaliacao INT AUTO_INCREMENT PRIMARY KEY,
   vo2_max DECIMAL(5,2) CHECK (vo2_max > 0),
   percentual_gordura DECIMAL(5,2) CHECK (percentual_gordura >= 0),
   altura DECIMAL(5,2) CHECK (altura > 0),
   peso DECIMAL(5,2) CHECK (peso > 0),
   data_avaliacao DATE NOT NULL,
   cod_pessoa INT NOT NULL,
   FOREIGN KEY (cod_pessoa) REFERENCES ATLETA(cod_pessoa) ON DELETE CASCADE
);

CREATE TABLE ORIENTA (
   cod_pessoa INT,
   cod_categoria INT,
   PRIMARY KEY (cod_pessoa, cod_categoria),
   FOREIGN KEY (cod_pessoa) REFERENCES MEMBROCOMISSAO(cod_pessoa) ON DELETE
CASCADE,
   FOREIGN KEY (cod_categoria) REFERENCES CATEGORIA(cod_categoria) ON DELETE
CASCADE
);

