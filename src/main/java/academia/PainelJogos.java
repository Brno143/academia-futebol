package academia;
 
import javax.swing.*;
import java.sql.SQLException;
import java.time.LocalDate;
 
/** CRUD da tabela JOGO. */
public class PainelJogos extends PainelCrud {
 
    public PainelJogos() { super("Jogos", "jogo"); }
 
    @Override
    protected Db.Resultado listar() throws SQLException {
        return Db.consultar("""
                SELECT j.cod_jogo AS codigo, DATE_FORMAT(j.data_jogo, '%d/%m/%Y') AS data,
                       c.nome_categoria AS categoria,
                       ad.nome_clube AS adversario, j.local_jogo AS local,
                       IF(j.mandante, 'Casa', 'Fora') AS mando,
                       CONCAT(j.gols_pro, ' x ', j.gols_contra) AS placar
                FROM JOGO j
                JOIN ADVERSARIO ad ON ad.cod_adversario = j.cod_adversario
                JOIN CATEGORIA c ON c.cod_categoria = j.cod_categoria
                ORDER BY j.data_jogo DESC""");
    }
 
    /** Campos do formulário de jogo (usados no cadastrar e no alterar). */
    private record Campos(JComboBox<Ui.Item> categoria, JComboBox<Ui.Item> adversario, JSpinner data,
                          JComboBox<String> mando, JTextField local, JSpinner golsPro, JSpinner golsContra) {
        JPanel painel() {
            return Ui.formulario("Categoria", categoria, "Adversário", adversario, "Data", data, "Mando", mando,
                    "Local", local, "Gols pró", golsPro, "Gols contra", golsContra);
        }
    }
 
    private Campos campos() throws SQLException {
        return new Campos(
                Ui.combo(Db.consultar("SELECT cod_categoria, nome_categoria FROM CATEGORIA ORDER BY idade_min"), false),
                Ui.combo(Db.consultar("SELECT cod_adversario, nome_clube FROM ADVERSARIO ORDER BY nome_clube"), false),
                Ui.campoData(LocalDate.now()),
                new JComboBox<>(new String[]{"Casa", "Fora"}),
                new JTextField("Campo Principal"),
                Ui.campoNumero(0, 0, 50),
                Ui.campoNumero(0, 0, 50));
    }
 
    @Override
    protected void novo() {
        try {
            Campos c = campos();
            while (Ui.dialogo(this, "Cadastrar jogo", c.painel())) {
                if (Ui.texto(c.local()) == null) { Ui.erro(this, "Preencha o local."); continue; }
                try {
                    Db.executar("""
                            INSERT INTO JOGO (data_jogo, local_jogo, mandante, gols_pro, gols_contra,
                                              cod_adversario, cod_categoria)
                            VALUES (?, ?, ?, ?, ?, ?, ?)""",
                            Ui.data(c.data()), Ui.texto(c.local()), "Casa".equals(c.mando().getSelectedItem()),
                            c.golsPro().getValue(), c.golsContra().getValue(), Ui.idSelecionado(c.adversario()),
                            Ui.idSelecionado(c.categoria()));
                    Ui.ok(this, "Jogo cadastrado.");
                    return;
                } catch (SQLException e) { Ui.erro(this, e); }
            }
        } catch (SQLException e) { Ui.erro(this, e); }
    }
 
    @Override
    protected void editar(Object id) {
        try {
            Object[] j = Db.consultar("""
                    SELECT data_jogo, local_jogo, mandante, gols_pro, gols_contra, cod_adversario, cod_categoria
                    FROM JOGO WHERE cod_jogo = ?""", id).linhas().get(0);
            Campos c = campos();
            c.data().setValue(java.util.Date.from(Ui.paraLocalDate(j[0])
                    .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
            c.local().setText((String) j[1]);
            boolean mandante = j[2] instanceof Boolean b ? b : Ui.inteiro(j[2]) == 1;
            c.mando().setSelectedItem(mandante ? "Casa" : "Fora");
            c.golsPro().setValue(Ui.inteiro(j[3]));
            c.golsContra().setValue(Ui.inteiro(j[4]));
            Ui.selecionar(c.adversario(), j[5]);
            Ui.selecionar(c.categoria(), j[6]);
 
            while (Ui.dialogo(this, "Alterar jogo", c.painel())) {
                if (Ui.texto(c.local()) == null) { Ui.erro(this, "Preencha o local."); continue; }
                try {
                    Db.executar("""
                            UPDATE JOGO SET data_jogo = ?, local_jogo = ?, mandante = ?,
                                   gols_pro = ?, gols_contra = ?, cod_adversario = ?, cod_categoria = ?
                            WHERE cod_jogo = ?""",
                            Ui.data(c.data()), Ui.texto(c.local()), "Casa".equals(c.mando().getSelectedItem()),
                            c.golsPro().getValue(), c.golsContra().getValue(), Ui.idSelecionado(c.adversario()),
                            Ui.idSelecionado(c.categoria()), id);
                    Ui.ok(this, "Jogo atualizado.");
                    return;
                } catch (SQLException e) { Ui.erro(this, e); }
            }
        } catch (SQLException e) { Ui.erro(this, e); }
    }
 
    @Override
    protected void excluir(Object id) {
        try {
            int escalados = Ui.inteiro(Db.consultar("SELECT COUNT(*) FROM ESCALACAO WHERE cod_jogo = ?", id).valor(0, 0));
            String msg = escalados > 0
                    ? "A escalação desse jogo (" + escalados + " atleta(s)) também será apagada (ON DELETE CASCADE).\nExcluir o jogo?"
                    : "Excluir esse jogo?";
            if (!Ui.confirmar(this, msg)) return;
            Db.executar("DELETE FROM JOGO WHERE cod_jogo = ?", id);
            Ui.ok(this, "Jogo excluído.");
        } catch (SQLException e) { Ui.erro(this, e); }
    }
}
 