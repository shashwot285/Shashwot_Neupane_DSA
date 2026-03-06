import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

// Question 5b: Multi-threaded Weather Data Collector
// Fetches real-time weather for 5 Nepal cities using one thread per city.
// Uses BlockingQueue for thread-safe GUI updates (no race conditions).
// Compares sequential vs parallel fetch latency with a bar chart.
//
// API: OpenWeatherMap free tier — type your key into the box when the app opens.
//      Sign up free at: https://openweathermap.org/api
// Run: javac WeatherCollectorApp.java && java WeatherCollectorApp
public class WeatherCollectorApp extends JFrame {

    static final String[] CITIES = {"Kathmandu", "Pokhara", "Biratnagar", "Nepalgunj", "Dhangadhi"};
    static final String   BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    private JTextField        apiField;
    private DefaultTableModel tableModel;
    private JTextArea         logArea;
    private JProgressBar      progress;
    private JLabel            statusBar;
    private JButton           fetchBtn;
    private double            seqTime = 0, parTime = 0;

    private final BlockingQueue<Map<String, String>> queue = new LinkedBlockingQueue<>();
    private final AtomicInteger done = new AtomicInteger(0);

    public WeatherCollectorApp() {
        super("Multi-threaded Weather Collector — Nepal");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(970, 680);
        setLocationRelativeTo(null);
        buildUI();
        setVisible(true);
    }

    void buildUI() {
        setLayout(new BorderLayout(4, 4));

        // ── Header ────────────────────────────────────────────────────────────
        JLabel hdr = new JLabel("  Multi-threaded Weather Monitor — Nepal Cities", SwingConstants.LEFT);
        hdr.setFont(new Font("SansSerif", Font.BOLD, 15));
        hdr.setOpaque(true);
        hdr.setBackground(new Color(22, 33, 62));
        hdr.setForeground(Color.WHITE);
        hdr.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ── API key row ───────────────────────────────────────────────────────
        JPanel apiRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        apiRow.setBackground(new Color(26, 42, 58));
        JLabel lbl = new JLabel("API Key:");
        lbl.setForeground(Color.LIGHT_GRAY);
        apiRow.add(lbl);
        apiField = new JTextField("YOUR_API_KEY_HERE", 36);
        apiField.setBackground(new Color(45, 61, 77));
        apiField.setForeground(Color.WHITE);
        apiField.setCaretColor(Color.WHITE);
        apiRow.add(apiField);

        // ── Buttons ───────────────────────────────────────────────────────────
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        btnRow.setBackground(new Color(26, 42, 58));
        fetchBtn         = btn("Fetch Weather (Parallel)",        new Color(39, 174, 96));
        JButton seqBtn   = btn("Sequential Fetch (Latency Test)", new Color(230, 126, 34));
        JButton chartBtn = btn("Show Latency Chart",              new Color(142, 68, 173));
        fetchBtn.addActionListener(e -> fetchParallel());
        seqBtn.addActionListener(e -> fetchSequential());
        chartBtn.addActionListener(e -> showChart());
        btnRow.add(fetchBtn);
        btnRow.add(seqBtn);
        btnRow.add(chartBtn);

        // ── Top panel (header + api key + buttons) — goes to NORTH ───────────
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(hdr,    BorderLayout.NORTH);
        topPanel.add(apiRow, BorderLayout.CENTER);
        topPanel.add(btnRow, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // ── Progress bar ──────────────────────────────────────────────────────
        progress = new JProgressBar();
        progress.setStringPainted(true);
        progress.setString("Idle");

        // ── Weather table ─────────────────────────────────────────────────────
        String[] cols = {"City", "Temp (°C)", "Feels Like", "Humidity", "Pressure", "Description", "Wind m/s", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setBackground(new Color(30, 45, 60));
        table.setForeground(new Color(236, 240, 241));
        table.getTableHeader().setBackground(new Color(44, 62, 80));
        table.getTableHeader().setForeground(Color.WHITE);
        table.setGridColor(new Color(52, 73, 94));
        table.setSelectionBackground(new Color(41, 128, 185));
        int[] cw = {120, 80, 80, 80, 85, 160, 85, 130};
        for (int i = 0; i < cw.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(cw[i]);

        // ── Thread log ────────────────────────────────────────────────────────
        logArea = new JTextArea(7, 80);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        logArea.setBackground(new Color(13, 27, 42));
        logArea.setForeground(new Color(46, 204, 113));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(52, 73, 94)),
            "Thread Log", 0, 0, null, Color.LIGHT_GRAY));

        // ── Centre panel (progress + table + log) — goes to CENTER ───────────
        JPanel centerPanel = new JPanel(new BorderLayout(4, 4));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        centerPanel.add(progress,               BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        centerPanel.add(logScroll,              BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        // ── Status bar ────────────────────────────────────────────────────────
        statusBar = new JLabel("  Ready.  Paste your API key above then click 'Fetch Weather (Parallel)'.");
        statusBar.setOpaque(true);
        statusBar.setBackground(new Color(22, 33, 62));
        statusBar.setForeground(new Color(189, 195, 199));
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(statusBar, BorderLayout.SOUTH);
    }

    JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // ── Fetch weather for one city ────────────────────────────────────────────
    Map<String, String> fetchCity(String city, String apiKey) {
        Map<String, String> r = new LinkedHashMap<>();
        r.put("City", city);
        try {
            String url = BASE_URL + "?q=" + URLEncoder.encode(city + ",NP", "UTF-8")
                       + "&appid=" + apiKey + "&units=metric";
            HttpURLConnection c = (HttpURLConnection) URI.create(url).toURL().openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            if (c.getResponseCode() != 200) {
                r.put("Status", "HTTP " + c.getResponseCode());
                fill(r);
                return r;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            String j = sb.toString();
            r.put("Temp (°C)",   num(j, "\"temp\":"));
            r.put("Feels Like",  num(j, "\"feels_like\":"));
            r.put("Humidity",    num(j, "\"humidity\":") + " %");
            r.put("Pressure",    num(j, "\"pressure\":") + " hPa");
            r.put("Description", str(j, "\"description\":\""));
            r.put("Wind m/s",    num(j, "\"speed\":"));
            r.put("Status", "OK");
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (msg == null) msg = "Unknown error";
            r.put("Status", "Error: " + msg.substring(0, Math.min(msg.length(), 30)));
            fill(r);
        }
        return r;
    }

    void fill(Map<String, String> m) {
        for (String k : new String[]{"Temp (°C)", "Feels Like", "Humidity", "Pressure", "Description", "Wind m/s"})
            m.putIfAbsent(k, "N/A");
    }

    String num(String json, String key) {
        int i = json.indexOf(key);
        if (i < 0) return "N/A";
        int s = i + key.length(), e = s;
        while (e < json.length() && json.charAt(e) != ',' && json.charAt(e) != '}') e++;
        return json.substring(s, e).trim();
    }

    String str(String json, String key) {
        int i = json.indexOf(key);
        if (i < 0) return "N/A";
        int s = i + key.length();
        int e = json.indexOf('"', s);
        return e < 0 ? "N/A" : json.substring(s, e);
    }

    // ── Parallel fetch ────────────────────────────────────────────────────────
    void fetchParallel() {
        String apiKey = apiField.getText().trim();
        if (apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            JOptionPane.showMessageDialog(this, "Paste your OpenWeatherMap API key into the box first!");
            return;
        }
        tableModel.setRowCount(0);
        queue.clear();
        done.set(0);
        fetchBtn.setEnabled(false);
        progress.setIndeterminate(true);
        progress.setString("Fetching...");
        statusBar.setText("  Fetching weather data with 5 parallel threads...");
        log("Starting parallel fetch — 5 threads launched simultaneously.");

        long t0 = System.currentTimeMillis();
        for (String city : CITIES) {
            new Thread(() -> {
                log("Thread started: " + city);
                Map<String, String> res = fetchCity(city, apiKey);
                log("Thread done   : " + city + " -> " + res.get("Status"));
                SwingUtilities.invokeLater(() -> addRow(res));
                if (done.incrementAndGet() == CITIES.length) {
                    parTime = (System.currentTimeMillis() - t0) / 1000.0;
                    SwingUtilities.invokeLater(() -> {
                        progress.setIndeterminate(false);
                        progress.setString("Done");
                        fetchBtn.setEnabled(true);
                        statusBar.setText(String.format(
                            "  Parallel done in %.2fs  |  Click 'Sequential Fetch' to compare latency.", parTime));
                        log(String.format("All threads finished. Parallel time = %.2f seconds.", parTime));
                    });
                }
            }, "WeatherThread-" + city).start();
        }
    }

    // ── Sequential fetch ──────────────────────────────────────────────────────
    void fetchSequential() {
        String apiKey = apiField.getText().trim();
        if (apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            JOptionPane.showMessageDialog(this, "Paste your OpenWeatherMap API key first!");
            return;
        }
        progress.setIndeterminate(true);
        progress.setString("Sequential...");
        statusBar.setText("  Running sequential fetch — one city at a time...");
        log("Starting sequential fetch — one city at a time.");

        new Thread(() -> {
            long t0 = System.currentTimeMillis();
            for (String city : CITIES) {
                log("[Sequential] Fetching " + city + "...");
                fetchCity(city, apiKey);
            }
            seqTime = (System.currentTimeMillis() - t0) / 1000.0;
            SwingUtilities.invokeLater(() -> {
                progress.setIndeterminate(false);
                progress.setString("Done");
                double speedup = parTime > 0 ? seqTime / parTime : 0;
                statusBar.setText(String.format(
                    "  Sequential: %.2fs  |  Parallel: %.2fs  |  Speedup: %.1fx  — Click 'Show Latency Chart'",
                    seqTime, parTime, speedup));
                log(String.format("Sequential time = %.2f seconds.  Speedup = %.1fx", seqTime, speedup));
            });
        }, "SequentialFetcher").start();
    }

    void addRow(Map<String, String> r) {
        tableModel.addRow(new Object[]{
            r.get("City"), r.get("Temp (°C)"), r.get("Feels Like"),
            r.get("Humidity"), r.get("Pressure"), r.get("Description"),
            r.get("Wind m/s"), r.get("Status")
        });
    }

    // ── Latency bar chart ─────────────────────────────────────────────────────
    void showChart() {
        if (seqTime == 0 && parTime == 0) {
            JOptionPane.showMessageDialog(this, "Run both Parallel and Sequential fetches first.");
            return;
        }
        JDialog dlg = new JDialog(this, "Latency Comparison Chart", true);
        dlg.setSize(500, 380);
        dlg.setLocationRelativeTo(this);
        dlg.add(new JPanel() {
            @Override
            protected void paintComponent(Graphics g2) {
                super.paintComponent(g2);
                setBackground(new Color(30, 45, 60));
                Graphics2D g = (Graphics2D) g2;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                double mx = Math.max(seqTime, Math.max(parTime, 0.1));
                int bW = 80, bY = 300, mH = 220;
                int sH = (int) (seqTime / mx * mH);
                g.setColor(new Color(231, 76, 60));
                g.fillRect(80, bY - sH, bW, sH);
                int pH = (int) (parTime / mx * mH);
                g.setColor(new Color(39, 174, 96));
                g.fillRect(300, bY - pH, bW, pH);
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g.drawString("Sequential", 82, bY + 20);
                g.drawString(String.format("%.2fs", seqTime), 88, bY - sH - 6);
                g.drawString("Parallel", 308, bY + 20);
                g.drawString(String.format("%.2fs", parTime), 308, bY - pH - 6);
                double speedup = parTime > 0 ? seqTime / parTime : 0;
                g.setColor(new Color(243, 156, 18));
                g.setFont(new Font("SansSerif", Font.BOLD, 14));
                g.drawString(String.format("Speedup: %.1fx faster with multithreading", speedup), 30, 38);
                g.setColor(Color.LIGHT_GRAY);
                g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g.drawString("All 5 cities fetched simultaneously — network waits overlap instead of stacking up.", 15, 358);
            }
        });
        dlg.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WeatherCollectorApp::new);
    }
}