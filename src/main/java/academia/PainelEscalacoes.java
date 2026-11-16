package academia;

import javax.swing.*;
import java.sql.SQLException;

/**
 * CRUD da tabela ESCALACAO (Etapa 06 - 4ª tabela com CRUD).
 * A chave é composta (cod_pessoa, cod_jogo), então a 1ª coluna da tabela guarda "pessoa-jogo".
 * O INSERT passa pelo trigger trg_valida_categoria_escalacao: se o atleta não for da
 * categoria do jogo, o banco bloqueia e a mensagem do trigger aparece na tela.
 */
public class PainelEscalacoes extends PainelCrud {

    public PainelEscalacoes() { super("Escalações", "escalação"); }

    @Override
    protected Db.Resultado listar() throws SQLException {
        return Db.consultar("""
                SELECT CONCAT(e.cod_pessoa, '-', e.cod_jogo) AS codigo,
                       CONCAT('#', j.cod_jogo, ' ', DATE_FORMAT(j.data_jogo, '%d/%m/%Y'), ' x ', ad.nome_clube) AS jogo,
                       c.nome_categoria AS categoria, p.nome AS atleta, po.nome_posicao AS posicao, po.setor,
                       e.minutos_jogados AS minutos, IF(e.titular, 'Sim', 'Não') AS titular,
                       IF(e.c_amarelo, 'Sim', 'Não') AS amarelo, IF(e.c_vermelho, 'Sim', 'Não') AS vermelho
                FROM ESCALACAO e
                JOIN JOGO j        ON j.cod_jogo = e.cod_jogo
                JOIN ADVERSARIO ad ON ad.cod_adversario = j.cod_adversario
                JOIN CATEGORIA c   ON c.cod_categoria = j.cod_categoria
                JOIN PESSOA p      ON p.cod_pessoa = e.cod_pessoa
                JOIN POSICAO po    ON po.cod_posicao = e.cod_posicao
                ORDER BY j.data_jogo DESC, e.titular DESC, po.cod_posicao""");
    }

    private record Campos(JComboBox<Ui.Item> jogo, JComboBox<Ui.Item> atleta, JComboBox<Ui.Item> posicao,
                          JSpinner minutos, JCheckBox titular, JCheckBox amarelo, JCheckBox vermelho) {
        JPanel painel() {
            return Ui.formulario("Jogo", jogo, "Atleta", atleta, "Posição", posicao, "Minutos jogados", minutos,
                    "Titular", titular, "Cartão amarelo", amarelo, "Cartão vermelho", vermelho);
        }
    }

    private Campos campos() throws SQLException {
        return new Campos(
                Ui.combo(Db.consultar("""
                        SELECT j.cod_jogo, CONCAT('#', j.cod_jogo, ' · ', DATE_FORMAT(j.data_jogo, '%d/%m/%Y'), ' · ',
                                                  c.nome_categoria, ' x ', ad.nome_clube)
                          FROM JOGO j JOIN CATEGORIA c ON c.cod_categoria = j.cod_categoria
                          JOIN ADVERSARIO ad ON ad.cod_adversario = j.cod_adversario
                         ORDER BY j.data_jogo DESC"""), false),
                // mostra todos os atletas (com a categoria) de propósito: quem valida é o trigger do banco
                Ui.combo(Db.consultar("""
                        SELECT a.cod_pessoa, CONCAT(p.nome, ' (', c.nome_categoria, ', camisa ', a.num_camisa, ')')
                          FROM ATLETA a JOIN PESSOA p ON p.cod_pessoa = a.cod_pessoa
                          JOIN CATEGORIA c ON c.cod_categoria = a.cod_categoria
                         ORDER BY c.idade_min, p.nome"""), false),
                Ui.combo(Db.consultar("SELECT cod_posicao, CONCAT(nome_posicao, ' (', setor, ')') FROM POSICAO ORDER BY cod_posicao"), false),
                Ui.campoNumero(90, 0, 120),
                new JCheckBox(), new JCheckBox(), new JCheckBox());
    }

    @Override
    protected void novo() {
        try {
            Campos c = campos();
            c.titular().setSelected(true);
            while (Ui.dialogo(this, "Cadastrar escalação", c.painel())) {
                try {
                    Db.executar("""
                            INSERT INTO ESCALACAO (cod_pessoa, cod_jogo, cod_posicao, minutos_jogados,
                                                   c_amarelo, c_vermelho, titular)
                            VALUES (?, ?, ?, ?, ?, ?, ?)""",
                            Ui.idSelecionado(c.atleta()), Ui.idSelecionado(c.jogo()), Ui.idSelecionado(c.posicao()),
                            c.minutos().getValue(), c.amarelo().isSelected(), c.vermelho().isSelected(), c.titular().isSelected());
                    Ui.ok(this, "Escalação cadastrada.");
                    return;
                } catch (SQLException e) {
                    if (e.getErrorCode() == 1062) Ui.erro(this, "Esse atleta já está escalado nesse jogo.");
                    else Ui.erro(this, e); // inclui a mensagem do trigger trg_valida_categoria_escalacao
                }
            }
        } catch (SQLException e) { Ui.erro(this, e); }
    }

    private static Object[] chave(Object id) {
        String[] p = id.toString().split("-");
        return new Object[]{Integer.parseInt(p[0]), Integer.parseInt(p[1])};
    }

    @Override
    protected void editar(Object id) {
        Object[] k = chave(id);
        try {
            Object[] e = Db.consultar("""
                    SELECT cod_posicao, minutos_jogados, titular, c_amarelo, c_vermelho
                      FROM ESCALACAO WHERE cod_pessoa = ? AND cod_jogo = ?""", k[0], k[1]).linhas().get(0);
            Campos c = campos();
            Ui.selecionar(c.atleta(), k[0]);
            Ui.selecionar(c.jogo(), k[1]);
            // atleta e jogo formam a chave: na alteração ficam travados
            c.atleta().setEnabled(false);
            c.jogo().setEnabled(false);
            Ui.selecionar(c.posicao(), e[0]);
            c.minutos().setValue(Ui.inteiro(e[1]));
            c.titular().setSelected(bool(e[2]));
            c.amarelo().setSelected(bool(e[3]));
            c.vermelho().setSelected(bool(e[4]));
            while (Ui.dialogo(this, "Alterar escalação", c.painel())) {
                try {
                    Db.executar("""
                            UPDATE ESCALACAO SET cod_posicao = ?, minutos_jogados = ?, titular = ?,
                                   c_amarelo = ?, c_vermelho = ?
                            WHERE cod_pessoa = ? AND cod_jogo = ?""",
                            Ui.idSelecionado(c.posicao()), c.minutos().getValue(), c.titular().isSelected(),
                            c.amarelo().isSelected(), c.vermelho().isSelected(), k[0], k[1]);
                    Ui.ok(this, "Escalação atualizada.");
                    return;
                } catch (SQLException ex) { Ui.erro(this, ex); }
            }
        } catch (SQLException ex) { Ui.erro(this, ex); }
    }

    @Override
    protected void excluir(Object id) {
        Object[] k = chave(id);
        if (!Ui.confirmar(this, "Excluir essa escalação?")) return;
        try {
            Db.executar("DELETE FROM ESCALACAO WHERE cod_pessoa = ? AND cod_jogo = ?", k[0], k[1]);
            Ui.ok(this, "Escalação excluída.");
        } catch (SQLException e) { Ui.erro(this, e); }
    }

    private static boolean bool(Object o) { return o instanceof Boolean b ? b : Ui.inteiro(o) == 1; }
}
