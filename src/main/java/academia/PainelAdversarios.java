package academia;

import javax.swing.*;
import java.sql.SQLException;

/** CRUD da tabela ADVERSARIO. */
public class PainelAdversarios extends PainelCrud {

    public PainelAdversarios() { super("Adversários", "adversário"); }

    @Override
    protected Db.Resultado listar() throws SQLException {
        return Db.consultar("""
                SELECT ad.cod_adversario AS codigo, ad.nome_clube AS clube, ad.cidade,
                       COUNT(j.cod_jogo) AS jogos
                FROM ADVERSARIO ad
                LEFT JOIN JOGO j ON j.cod_adversario = ad.cod_adversario
                GROUP BY ad.cod_adversario, ad.nome_clube, ad.cidade
                ORDER BY ad.nome_clube""");
    }

    @Override
    protected void novo() {
        JTextField nome = new JTextField(), cidade = new JTextField();
        while (Ui.dialogo(this, "Cadastrar adversário", Ui.formulario("Nome do clube", nome, "Cidade", cidade))) {
            if (Ui.texto(nome) == null || Ui.texto(cidade) == null) { Ui.erro(this, "Preencha nome e cidade."); continue; }
            try {
                Db.executar("INSERT INTO ADVERSARIO (nome_clube, cidade) VALUES (?, ?)", Ui.texto(nome), Ui.texto(cidade));
                Ui.ok(this, Ui.texto(nome) + " cadastrado.");
                return;
            } catch (SQLException e) { Ui.erro(this, e); }
        }
    }

    @Override
    protected void editar(Object id) {
        try {
            Object[] a = Db.consultar("SELECT nome_clube, cidade FROM ADVERSARIO WHERE cod_adversario = ?", id).linhas().get(0);
            JTextField nome = new JTextField((String) a[0]), cidade = new JTextField((String) a[1]);
            while (Ui.dialogo(this, "Alterar adversário", Ui.formulario("Nome do clube", nome, "Cidade", cidade))) {
                if (Ui.texto(nome) == null || Ui.texto(cidade) == null) { Ui.erro(this, "Preencha nome e cidade."); continue; }
                try {
                    Db.executar("UPDATE ADVERSARIO SET nome_clube = ?, cidade = ? WHERE cod_adversario = ?",
                            Ui.texto(nome), Ui.texto(cidade), id);
                    Ui.ok(this, "Adversário atualizado.");
                    return;
                } catch (SQLException e) { Ui.erro(this, e); }
            }
        } catch (SQLException e) { Ui.erro(this, e); }
    }

    @Override
    protected void excluir(Object id) {
        try {
            int jogos = Ui.inteiro(Db.consultar("SELECT COUNT(*) FROM JOGO WHERE cod_adversario = ?", id).valor(0, 0));
            String msg = jogos > 0
                    ? "Esse adversário tem " + jogos + " jogo(s) cadastrado(s), então o banco vai bloquear a exclusão (ON DELETE RESTRICT).\nTentar mesmo assim?"
                    : "Excluir esse adversário?";
            if (!Ui.confirmar(this, msg)) return;
            Db.executar("DELETE FROM ADVERSARIO WHERE cod_adversario = ?", id);
            Ui.ok(this, "Adversário excluído.");
        } catch (SQLException e) { Ui.erro(this, e); }
    }
}
