package academia;

import javax.swing.*;
import java.sql.SQLException;
import java.time.LocalDate;

/** CRUD da tabela AVALIACAOFISICA (Etapa 06). Os dados alimentam as medidas estatísticas do Dashboard. */
public class PainelAvaliacoes extends PainelCrud {

    public PainelAvaliacoes() { super("Avaliações físicas", "avaliação"); }

    @Override
    protected Db.Resultado listar() throws SQLException {
        return Db.consultar("""
                SELECT af.cod_avaliacao AS codigo, DATE_FORMAT(af.data_avaliacao, '%d/%m/%Y') AS data,
                       p.nome AS atleta, c.nome_categoria AS categoria,
                       af.vo2_max, af.percentual_gordura AS pct_gordura, af.altura AS altura_cm, af.peso AS peso_kg,
                       ROUND(af.peso / POWER(af.altura / 100, 2), 1) AS imc
                FROM AVALIACAOFISICA af
                JOIN ATLETA a    ON a.cod_pessoa = af.cod_pessoa
                JOIN PESSOA p    ON p.cod_pessoa = a.cod_pessoa
                JOIN CATEGORIA c ON c.cod_categoria = a.cod_categoria
                ORDER BY af.data_avaliacao DESC, c.idade_min, p.nome""");
    }

    private record Campos(JComboBox<Ui.Item> atleta, JSpinner data, JSpinner vo2, JSpinner gordura,
                          JSpinner altura, JSpinner peso) {
        JPanel painel() {
            return Ui.formulario("Atleta", atleta, "Data da avaliação", data, "VO2 máx (ml/kg/min)", vo2,
                    "% de gordura", gordura, "Altura (cm)", altura, "Peso (kg)", peso);
        }
    }

    private Campos campos() throws SQLException {
        return new Campos(
                Ui.combo(Db.consultar("""
                        SELECT a.cod_pessoa, CONCAT(p.nome, ' (', c.nome_categoria, ')')
                          FROM ATLETA a JOIN PESSOA p ON p.cod_pessoa = a.cod_pessoa
                          JOIN CATEGORIA c ON c.cod_categoria = a.cod_categoria
                         ORDER BY c.idade_min, p.nome"""), false),
                Ui.campoData(LocalDate.now()),
                Ui.campoDecimal(45, 10, 90, 0.1),
                Ui.campoDecimal(14, 0, 60, 0.1),
                Ui.campoDecimal(160, 100, 220, 0.5),
                Ui.campoDecimal(55, 20, 150, 0.5));
    }

    private Object[] valores(Campos c) {
        return new Object[]{c.vo2().getValue(), c.gordura().getValue(), c.altura().getValue(), c.peso().getValue(),
                Ui.data(c.data()), Ui.idSelecionado(c.atleta())};
    }

    @Override
    protected void novo() {
        try {
            Campos c = campos();
            while (Ui.dialogo(this, "Cadastrar avaliação física", c.painel())) {
                try {
                    Db.executar("""
                            INSERT INTO AVALIACAOFISICA (vo2_max, percentual_gordura, altura, peso, data_avaliacao, cod_pessoa)
                            VALUES (?, ?, ?, ?, ?, ?)""", valores(c));
                    Ui.ok(this, "Avaliação cadastrada.");
                    return;
                } catch (SQLException e) { erro(e); }
            }
        } catch (SQLException e) { Ui.erro(this, e); }
    }

    @Override
    protected void editar(Object id) {
        try {
            Object[] a = Db.consultar("""
                    SELECT cod_pessoa, data_avaliacao, vo2_max, percentual_gordura, altura, peso
                      FROM AVALIACAOFISICA WHERE cod_avaliacao = ?""", id).linhas().get(0);
            Campos c = campos();
            Ui.selecionar(c.atleta(), a[0]);
            c.data().setValue(java.util.Date.from(Ui.paraLocalDate(a[1]).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
            c.vo2().setValue(Ui.decimal(a[2]));
            c.gordura().setValue(Ui.decimal(a[3]));
            c.altura().setValue(Ui.decimal(a[4]));
            c.peso().setValue(Ui.decimal(a[5]));
            while (Ui.dialogo(this, "Alterar avaliação física", c.painel())) {
                try {
                    Object[] v = valores(c);
                    Db.executar("""
                            UPDATE AVALIACAOFISICA SET vo2_max = ?, percentual_gordura = ?, altura = ?, peso = ?,
                                   data_avaliacao = ?, cod_pessoa = ?
                            WHERE cod_avaliacao = ?""", v[0], v[1], v[2], v[3], v[4], v[5], id);
                    Ui.ok(this, "Avaliação atualizada.");
                    return;
                } catch (SQLException e) { erro(e); }
            }
        } catch (SQLException e) { Ui.erro(this, e); }
    }

    private void erro(SQLException e) {
        if (e.getErrorCode() == 1062) Ui.erro(this, "Esse atleta já tem uma avaliação nessa data (índice único idx_avaliacao_atleta_data).");
        else Ui.erro(this, e);
    }

    @Override
    protected void excluir(Object id) {
        if (!Ui.confirmar(this, "Excluir essa avaliação física?")) return;
        try {
            Db.executar("DELETE FROM AVALIACAOFISICA WHERE cod_avaliacao = ?", id);
            Ui.ok(this, "Avaliação excluída.");
        } catch (SQLException e) { Ui.erro(this, e); }
    }
}
