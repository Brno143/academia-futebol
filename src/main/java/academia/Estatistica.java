package academia;

import java.util.*;

/** Medidas da disciplina de Estatística calculadas em Java sobre os valores lidos do banco. */
public class Estatistica {

    /** Resumo de uma variável: n, média, mediana, moda, variância, desvio padrão, CV, mínimo e máximo. */
    public record Resumo(int n, double media, double mediana, String moda, double variancia,
                         double desvio, double cv, double min, double max) { }

    public static Resumo resumo(List<Double> valores) {
        int n = valores.size();
        if (n == 0) return new Resumo(0, Double.NaN, Double.NaN, "-", Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        double media = media(valores);
        double var = variancia(valores);
        double dp = Math.sqrt(var);
        return new Resumo(n, media, mediana(valores), moda(valores), var, dp,
                media == 0 ? Double.NaN : 100 * dp / media,
                Collections.min(valores), Collections.max(valores));
    }

    public static double media(List<Double> v) {
        if (v.isEmpty()) return Double.NaN;
        double s = 0;
        for (double x : v) s += x;
        return s / v.size();
    }

    public static double mediana(List<Double> v) {
        if (v.isEmpty()) return Double.NaN;
        List<Double> o = new ArrayList<>(v);
        Collections.sort(o);
        int n = o.size();
        return n % 2 == 1 ? o.get(n / 2) : (o.get(n / 2 - 1) + o.get(n / 2)) / 2;
    }

    /** Moda: valor(es) mais frequente(s). Se todos aparecem uma vez só, a amostra é amodal. */
    public static String moda(List<Double> v) {
        if (v.isEmpty()) return "-";
        Map<Double, Integer> freq = new TreeMap<>();
        for (double x : v) freq.merge(Math.round(x * 100) / 100.0, 1, Integer::sum);
        int max = Collections.max(freq.values());
        if (max == 1) return "amodal";
        List<String> modas = new ArrayList<>();
        for (var e : freq.entrySet()) if (e.getValue() == max) modas.add(fmt(e.getKey()));
        if (modas.size() > 3) return modas.size() + " modas (" + max + "x)";
        return String.join(" / ", modas) + " (" + max + "x)";
    }

    /** Variância amostral (divide por n - 1), a mesma usada na disciplina. */
    public static double variancia(List<Double> v) {
        int n = v.size();
        if (n < 2) return 0;
        double m = media(v), s = 0;
        for (double x : v) s += (x - m) * (x - m);
        return s / (n - 1);
    }

    /** Coeficiente de correlação de Pearson entre x e y. */
    public static double pearson(List<Double> x, List<Double> y) {
        int n = x.size();
        if (n < 3) return Double.NaN;
        double mx = media(x), my = media(y), sxy = 0, sxx = 0, syy = 0;
        for (int i = 0; i < n; i++) {
            double dx = x.get(i) - mx, dy = y.get(i) - my;
            sxy += dx * dy; sxx += dx * dx; syy += dy * dy;
        }
        return (sxx == 0 || syy == 0) ? Double.NaN : sxy / Math.sqrt(sxx * syy);
    }

    /** Reta de regressão y = a + b x (mínimos quadrados). Devolve {a, b}. */
    public static double[] regressao(List<Double> x, List<Double> y) {
        int n = x.size();
        if (n < 2) return new double[]{0, 0};
        double mx = media(x), my = media(y), sxy = 0, sxx = 0;
        for (int i = 0; i < n; i++) { sxy += (x.get(i) - mx) * (y.get(i) - my); sxx += (x.get(i) - mx) * (x.get(i) - mx); }
        double b = sxx == 0 ? 0 : sxy / sxx;
        return new double[]{my - b * mx, b};
    }

    /** Interpretação usual da força da correlação. */
    public static String forca(double r) {
        if (Double.isNaN(r)) return "sem dados suficientes";
        double a = Math.abs(r);
        String f = a >= 0.9 ? "muito forte" : a >= 0.7 ? "forte" : a >= 0.5 ? "moderada" : a >= 0.3 ? "fraca" : "desprezível";
        return f + (r >= 0 ? " positiva" : " negativa");
    }

    /** Tabela de frequência por classes (regra de Sturges). Devolve rótulos e frequências. */
    public record Classes(List<String> rotulos, double[] freq) { }

    public static Classes classes(List<Double> v) {
        if (v.isEmpty()) return new Classes(List.of(), new double[0]);
        double min = Collections.min(v), max = Collections.max(v);
        int k = (int) Math.ceil(1 + 3.322 * Math.log10(v.size()));
        k = Math.max(3, Math.min(k, 9));
        if (max == min) { max = min + 1; }
        double amp = (max - min) / k;
        double[] f = new double[k];
        for (double x : v) {
            int i = (int) ((x - min) / amp);
            if (i >= k) i = k - 1;
            f[i]++;
        }
        List<String> r = new ArrayList<>();
        for (int i = 0; i < k; i++) r.add(fmt(min + i * amp) + "–" + fmt(min + (i + 1) * amp));
        return new Classes(r, f);
    }

    public static String fmt(double v) {
        if (Double.isNaN(v)) return "-";
        if (v == Math.rint(v) && Math.abs(v) < 1e6) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }
}
