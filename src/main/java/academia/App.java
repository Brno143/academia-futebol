package academia;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;

/** Academia de Base - Etapa 03. Interface Swing + JDBC (MySQL), sem ORM. */
public class App {

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
                if ("Nimbus".equals(info.getName())) UIManager.setLookAndFeel(info.getClassName());
            UIManager.put("nimbusBase", new Color(0x1F5A34));
            UIManager.put("nimbusSelectionBackground", Ui.VERDE);
            UIManager.put("nimbusFocus", new Color(0x7FB08E));
        } catch (Exception ignorado) { }

        SwingUtilities.invokeLater(App::abrir);
    }

    private static void abrir() {
        try (Connection c = Db.conectar()) {
            // conexão ok
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Não foi possível conectar ao MySQL.\n\nConfira usuário e senha em Db.java e se o servidor está ligado.\n\n"
                            + e.getMessage(), "Erro de conexão", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        PainelDashboard painel = new PainelDashboard();
        PainelEstatistica estatistica = new PainelEstatistica();
        PainelAtletas atletas = new PainelAtletas();
        PainelJogos jogos = new PainelJogos();
        PainelAdversarios adversarios = new PainelAdversarios();

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Painel", painel);
        abas.addTab("Estatística", estatistica);
        abas.addTab("Consultas", new PainelConsultas());
        abas.addTab("Atletas", atletas);
        abas.addTab("Jogos", jogos);
        abas.addTab("Adversários", adversarios);

        // recarrega os dados sempre que troca de aba (pra refletir inserções/exclusões)
        abas.addChangeListener(e -> {
            Component atual = abas.getSelectedComponent();
            if (atual == painel) painel.carregar();
            else if (atual == estatistica) estatistica.carregar();
            else if (atual instanceof PainelCrud crud) crud.carregar();
        });
        painel.carregar();

        JFrame frame = new JFrame("Academia de Base - Gerenciamento");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(abas);
        frame.setSize(1200, 780);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
