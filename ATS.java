import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ATS extends JFrame {

    static final String[] STAGES = {"APPLIED", "SCREENING", "INTERVIEW", "OFFER", "HIRED", "REJECTED"};
    static final Color[] STAGE_COLORS = {
            new Color(59, 130, 246), new Color(139, 92, 246), new Color(245, 158, 11),
            new Color(20, 184, 166), new Color(34, 197, 94), new Color(239, 68, 68)};
    static final Color NAVY = new Color(15, 23, 42), BLUE = new Color(37, 99, 235),
            BG = new Color(241, 245, 249), TEXT = new Color(30, 41, 59), MUTED = new Color(100, 116, 139);
    static final Font F = new Font("Segoe UI", Font.PLAIN, 14), FB = new Font("Segoe UI", Font.BOLD, 14);
    static final String FILE = "candidates.csv";

    static class Candidate { int id; String name, email, skills, role, stage, date; }

    // ---------- reusable styled components ----------
    static class Btn extends JButton {
        Color base;
        Btn(String t, Color c) {
            super(t); base = c;
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setForeground(Color.WHITE); setFont(FB); setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 8, 9, 8));
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(!isEnabled() ? new Color(203, 213, 225) : getModel().isRollover() ? base.brighter() : base);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class Card extends JPanel {
        Color accent;
        Card(Color accent) { this.accent = accent; setOpaque(false); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 18)); g2.fillRoundRect(2, 3, getWidth() - 4, getHeight() - 4, 16, 16);
            g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 16, 16);
            if (accent != null) { g2.setColor(accent); g2.fillRoundRect(0, 0, 7, getHeight() - 3, 7, 7); }
            g2.dispose();
        }
    }

    static JTextField field() {
        JTextField t = new JTextField();
        t.setFont(F);
        t.setBorder(new CompoundBorder(new LineBorder(new Color(203, 213, 225), 1, true), new EmptyBorder(7, 10, 7, 10)));
        return t;
    }

    static JLabel label(String s) {
        JLabel l = new JLabel(s); l.setFont(new Font("Segoe UI", Font.BOLD, 12)); l.setForeground(MUTED); return l;
    }

    // ---------- state ----------
    List<Candidate> list = new ArrayList<>();
    DefaultTableModel model;
    JTable table;
    JPanel dash = new JPanel(new BorderLayout(0, 18));
    CardLayout cards = new CardLayout();
    JPanel pages = new JPanel(cards);
    JTextField nameF = field(), emailF = field(), skillsF = field(), roleF = field(), searchF = field();
    JComboBox<String> stageBox = new JComboBox<>(STAGES), filterBox = new JComboBox<>();
    Btn navCand, navDash, updBtn;

    public ATS() {
        setTitle("TalentTrack - Applicant Tracking System");
        setSize(1150, 700);
        setMinimumSize(new Dimension(1000, 620));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        load();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.add(buildHeader(), BorderLayout.NORTH);

        pages.setBackground(BG);
        pages.setBorder(new EmptyBorder(12, 18, 12, 18));
        dash.setOpaque(false);
        pages.add(buildCandidatesPage(), "cand");
        pages.add(dash, "dash");
        root.add(pages, BorderLayout.CENTER);
        setContentPane(root);

        refreshTable();
        refreshDashboard();
    }

    // ---------- header ----------
    JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, NAVY, getWidth(), 0, new Color(30, 64, 175)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        h.setBorder(new EmptyBorder(10, 24, 10, 24));

        JPanel titles = new JPanel(new GridLayout(2, 1)); titles.setOpaque(false);
        JLabel t = new JLabel("TalentTrack ATS"); t.setFont(new Font("Segoe UI", Font.BOLD, 24)); t.setForeground(Color.WHITE);
        JLabel s = new JLabel("Manage candidates, track hiring stages and monitor recruitment"); s.setFont(F); s.setForeground(new Color(191, 219, 254));
        titles.add(t); titles.add(s);
        h.add(titles, BorderLayout.WEST);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6)); nav.setOpaque(false);
        navCand = new Btn("Candidates", BLUE);
        navDash = new Btn("Dashboard", new Color(255, 255, 255, 40));
        navCand.addActionListener(e -> showPage("cand"));
        navDash.addActionListener(e -> { refreshDashboard(); showPage("dash"); });
        nav.add(navCand); nav.add(navDash);
        h.add(nav, BorderLayout.EAST);
        return h;
    }

    void showPage(String p) {
        cards.show(pages, p);
        navCand.base = p.equals("cand") ? BLUE : new Color(255, 255, 255, 40);
        navDash.base = p.equals("dash") ? BLUE : new Color(255, 255, 255, 40);
        repaint();
    }

    // ---------- candidates page ----------
    JPanel buildCandidatesPage() {
        JPanel page = new JPanel(new BorderLayout(18, 14)); page.setOpaque(false);

        // search bar
        Card searchCard = new Card(null); searchCard.setLayout(new BorderLayout(10, 0));
        searchCard.setBorder(new EmptyBorder(8, 14, 8, 14));
        searchF.putClientProperty("JTextField.placeholderText", "Search by name, skill or job role...");
        filterBox.addItem("ALL STAGES"); for (String s : STAGES) filterBox.addItem(s);
        filterBox.setFont(F);
        Btn searchBtn = new Btn("Search", BLUE);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); right.setOpaque(false);
        right.add(new JLabel("Stage:")); right.add(filterBox); right.add(searchBtn);
        searchCard.add(searchF, BorderLayout.CENTER); searchCard.add(right, BorderLayout.EAST);
        page.add(searchCard, BorderLayout.NORTH);
        searchBtn.addActionListener(e -> refreshTable());
        searchF.addActionListener(e -> refreshTable());
        filterBox.addActionListener(e -> refreshTable());

        // form
        Card formCard = new Card(BLUE); formCard.setLayout(new BorderLayout(0, 10));
        formCard.setBorder(new EmptyBorder(14, 20, 14, 14));
        formCard.setPreferredSize(new Dimension(330, 0));
        JPanel form = new JPanel(); form.setOpaque(false); form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        JLabel ft = new JLabel("Candidate Details"); ft.setFont(new Font("Segoe UI", Font.BOLD, 17)); ft.setForeground(TEXT);
        ft.setAlignmentX(0f);
        JLabel hint = new JLabel("Select a row in the table to edit it."); hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hint.setForeground(MUTED); hint.setAlignmentX(0f);
        form.add(ft); form.add(Box.createVerticalStrut(2)); form.add(hint); form.add(Box.createVerticalStrut(10));
        stageBox.setFont(F);
        addRow(form, "FULL NAME", nameF); addRow(form, "EMAIL", emailF);
        addRow(form, "SKILLS (e.g. Java, SQL)", skillsF); addRow(form, "JOB ROLE", roleF);
        addRow(form, "HIRING STAGE", stageBox);
        JScrollPane formScroll = new JScrollPane(form, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setBorder(null); formScroll.setOpaque(false); formScroll.getViewport().setOpaque(false);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        formCard.add(formScroll, BorderLayout.CENTER);

        Btn add = new Btn("Add", new Color(34, 197, 94));
        updBtn = new Btn("Update", BLUE);
        Btn del = new Btn("Delete", new Color(239, 68, 68)), next = new Btn("Next Stage", new Color(139, 92, 246)),
                clr = new Btn("Clear", new Color(100, 116, 139));
        JPanel row1 = new JPanel(new GridLayout(1, 3, 8, 0)); row1.setOpaque(false);
        row1.add(add); row1.add(updBtn); row1.add(del);
        JPanel row2 = new JPanel(new GridLayout(1, 2, 8, 0)); row2.setOpaque(false);
        row2.add(next); row2.add(clr);
        JPanel btns = new JPanel(new GridLayout(2, 1, 0, 8)); btns.setOpaque(false);
        btns.add(row1); btns.add(row2);
        formCard.add(btns, BorderLayout.SOUTH);
        page.add(formCard, BorderLayout.WEST);
        updBtn.setEnabled(false);

        add.addActionListener(e -> addCandidate());
        updBtn.addActionListener(e -> updateCandidate());
        del.addActionListener(e -> deleteCandidate());
        next.addActionListener(e -> nextStage());
        clr.addActionListener(e -> clearForm());

        // table
        model = new DefaultTableModel(new String[]{"ID", "Name", "Email", "Skills", "Job Role", "Stage", "Applied On"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setFont(F); table.setRowHeight(38); table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(219, 234, 254)); table.setSelectionForeground(TEXT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setMaxWidth(50);

        JTableHeader th = table.getTableHeader();
        th.setPreferredSize(new Dimension(0, 40));
        th.setDefaultRenderer((tb, v, sel, foc, r, c) -> {
            JLabel l = new JLabel("  " + v); l.setOpaque(true); l.setBackground(NAVY);
            l.setForeground(Color.WHITE); l.setFont(FB); return l;
        });

        DefaultTableCellRenderer base = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setBorder(new EmptyBorder(0, 10, 0, 6));
                if (!sel) setBackground(r % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                return this;
            }
        };
        for (int i = 0; i < 7; i++) if (i != 5) table.getColumnModel().getColumn(i).setCellRenderer(base);
        table.getColumnModel().getColumn(5).setCellRenderer((t, v, sel, foc, r, c) -> {
            JLabel l = new JLabel(String.valueOf(v), SwingConstants.CENTER);
            l.setOpaque(true); l.setFont(new Font("Segoe UI", Font.BOLD, 12)); l.setForeground(Color.WHITE);
            l.setBackground(STAGE_COLORS[Math.max(0, Arrays.asList(STAGES).indexOf(String.valueOf(v)))]);
            JPanel w = new JPanel(new GridBagLayout());
            w.setBackground(sel ? new Color(219, 234, 254) : (r % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));
            l.setBorder(new EmptyBorder(4, 12, 4, 12));
            w.add(l);
            return w;
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new LineBorder(new Color(226, 232, 240)));
        sp.getViewport().setBackground(Color.WHITE);
        page.add(sp, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            int r = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && r != -1) {
                r = table.convertRowIndexToModel(r);
                nameF.setText(model.getValueAt(r, 1).toString());
                emailF.setText(model.getValueAt(r, 2).toString());
                skillsF.setText(model.getValueAt(r, 3).toString());
                roleF.setText(model.getValueAt(r, 4).toString());
                stageBox.setSelectedItem(model.getValueAt(r, 5).toString());
                updBtn.setEnabled(true);
            }
        });
        return page;
    }

    void addRow(JPanel p, String name, JComponent c) {
        JLabel l = label(name); l.setAlignmentX(0f); c.setAlignmentX(0f);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34)); c.setPreferredSize(new Dimension(200, 34));
        p.add(l); p.add(Box.createVerticalStrut(4)); p.add(c); p.add(Box.createVerticalStrut(8));
    }

    // ---------- dashboard ----------
    void refreshDashboard() {
        dash.removeAll();
        int total = list.size(), hired = count("HIRED"), rejected = count("REJECTED");
        int active = total - hired - rejected;

        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0)); stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(0, 100));
        stats.add(stat("Total Candidates", total, BLUE));
        stats.add(stat("In Pipeline", active, new Color(245, 158, 11)));
        stats.add(stat("Hired", hired, new Color(34, 197, 94)));
        stats.add(stat("Rejected", rejected, new Color(239, 68, 68)));
        dash.add(stats, BorderLayout.NORTH);

        JPanel mid = new JPanel(new GridLayout(1, 2, 16, 0)); mid.setOpaque(false);

        Card chartCard = new Card(null) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 17)); g2.setColor(TEXT);
                g2.drawString("Candidates by Stage", 22, 34);
                int max = 1; for (String s : STAGES) max = Math.max(max, count(s));
                int y = 62, rowH = (getHeight() - 80) / STAGES.length, barMax = getWidth() - 220;
                for (int i = 0; i < STAGES.length; i++) {
                    int c = count(STAGES[i]);
                    g2.setFont(F); g2.setColor(MUTED); g2.drawString(STAGES[i], 22, y + rowH / 2 + 2);
                    g2.setColor(new Color(241, 245, 249)); g2.fillRoundRect(120, y, barMax, rowH - 12, 10, 10);
                    g2.setColor(STAGE_COLORS[i]); g2.fillRoundRect(120, y, Math.max(c * barMax / max, c > 0 ? 10 : 0), rowH - 12, 10, 10);
                    g2.setFont(FB); g2.setColor(TEXT); g2.drawString(String.valueOf(c), 130 + barMax, y + rowH / 2 + 2);
                    y += rowH;
                }
            }
        };
        mid.add(chartCard);

        Card recent = new Card(null); recent.setLayout(new BorderLayout(0, 10));
        recent.setBorder(new EmptyBorder(16, 22, 16, 18));
        JLabel rt = new JLabel("Recent Applicants"); rt.setFont(new Font("Segoe UI", Font.BOLD, 17)); rt.setForeground(TEXT);
        recent.add(rt, BorderLayout.NORTH);
        JPanel rows = new JPanel(new GridLayout(5, 1, 0, 6)); rows.setOpaque(false);
        int shown = 0;
        for (int i = list.size() - 1; i >= 0 && shown < 5; i--, shown++) {
            Candidate c = list.get(i);
            JPanel r = new JPanel(new BorderLayout()); r.setOpaque(false);
            r.setBorder(new MatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));
            JLabel n = new JLabel("<html><b>" + c.name + "</b><br><span style='color:#64748b'>" + c.role + "  |  " + c.date + "</span></html>");
            n.setFont(F);
            JLabel st = new JLabel(c.stage); st.setFont(FB);
            st.setForeground(STAGE_COLORS[Math.max(0, Arrays.asList(STAGES).indexOf(c.stage))]);
            r.add(n, BorderLayout.CENTER); r.add(st, BorderLayout.EAST);
            rows.add(r);
        }
        if (list.isEmpty()) { JLabel e = new JLabel("No candidates yet. Add your first one!"); e.setForeground(MUTED); rows.add(e); }
        recent.add(rows, BorderLayout.CENTER);
        mid.add(recent);

        dash.add(mid, BorderLayout.CENTER);
        dash.revalidate(); dash.repaint();
    }

    JPanel stat(String title, int value, Color color) {
        Card c = new Card(color); c.setLayout(new GridLayout(2, 1));
        c.setBorder(new EmptyBorder(14, 24, 10, 10));
        JLabel t = new JLabel(title); t.setFont(F); t.setForeground(MUTED);
        JLabel v = new JLabel(String.valueOf(value)); v.setFont(new Font("Segoe UI", Font.BOLD, 34)); v.setForeground(color);
        c.add(t); c.add(v);
        return c;
    }

    // ---------- logic ----------
    void addCandidate() {
        if (!valid(-1)) return;
        Candidate c = new Candidate();
        c.id = list.stream().mapToInt(x -> x.id).max().orElse(0) + 1;
        fill(c); c.date = LocalDate.now().toString();
        list.add(c);
        saveAndRefresh(); clearForm();
        info("Candidate added successfully.");
    }

    void updateCandidate() {
        Candidate c = selected(); if (c == null || !valid(c.id)) return;
        fill(c); saveAndRefresh(); clearForm();
        info("Candidate updated successfully.");
    }

    void deleteCandidate() {
        Candidate c = selected(); if (c == null) return;
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " + c.name + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            list.remove(c); saveAndRefresh(); clearForm();
            info("Candidate deleted successfully.");
        }
    }

    void nextStage() {
        Candidate c = selected(); if (c == null) return;
        int i = Arrays.asList(STAGES).indexOf(c.stage);
        if (i >= 4) { warn("This candidate has already reached a final stage (" + c.stage + ")."); return; }
        c.stage = STAGES[i + 1];
        saveAndRefresh(); clearForm();
        info(c.name + " moved to " + c.stage + ".");
    }

    Candidate selected() {
        int r = table.getSelectedRow();
        if (r == -1) { warn("Please select a candidate from the table first."); return null; }
        int id = (int) model.getValueAt(table.convertRowIndexToModel(r), 0);
        for (Candidate c : list) if (c.id == id) return c;
        return null;
    }

    void fill(Candidate c) {
        c.name = clean(nameF.getText()); c.email = clean(emailF.getText());
        c.skills = clean(skillsF.getText()); c.role = clean(roleF.getText());
        c.stage = stageBox.getSelectedItem().toString();
    }

    boolean valid(int ignoreId) {
        if (nameF.getText().isBlank() || emailF.getText().isBlank() || skillsF.getText().isBlank() || roleF.getText().isBlank()) {
            warn("Please fill in all the fields."); return false;
        }
        if (!emailF.getText().trim().matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            warn("Please enter a valid email address."); return false;
        }
        for (Candidate c : list)
            if (c.id != ignoreId && c.email.equalsIgnoreCase(emailF.getText().trim())) {
                warn("A candidate with this email already exists."); return false;
            }
        return true;
    }

    int count(String stage) { int n = 0; for (Candidate c : list) if (c.stage.equals(stage)) n++; return n; }

    void refreshTable() {
        model.setRowCount(0);
        String s = searchF.getText().trim().toLowerCase(), st = filterBox.getSelectedItem().toString();
        for (Candidate c : list) {
            boolean t = s.isEmpty() || c.name.toLowerCase().contains(s) || c.skills.toLowerCase().contains(s) || c.role.toLowerCase().contains(s);
            if (t && (st.equals("ALL STAGES") || c.stage.equals(st)))
                model.addRow(new Object[]{c.id, c.name, c.email, c.skills, c.role, c.stage, c.date});
        }
    }

    void saveAndRefresh() { save(); refreshTable(); refreshDashboard(); }

    void clearForm() {
        nameF.setText(""); emailF.setText(""); skillsF.setText(""); roleF.setText("");
        stageBox.setSelectedIndex(0); table.clearSelection(); updBtn.setEnabled(false);
    }

    void info(String m) { JOptionPane.showMessageDialog(this, m, "Success", JOptionPane.INFORMATION_MESSAGE); }
    void warn(String m) { JOptionPane.showMessageDialog(this, m, "Attention", JOptionPane.WARNING_MESSAGE); }
    static String clean(String s) { return s.trim().replace(",", " "); }

    // ---------- file storage ----------
    void load() {
        File f = new File(FILE); if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length == 7) {
                    Candidate c = new Candidate();
                    c.id = Integer.parseInt(p[0]); c.name = p[1]; c.email = p[2]; c.skills = p[3];
                    c.role = p[4]; c.stage = p[5]; c.date = p[6];
                    list.add(c);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    void save() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            for (Candidate c : list)
                pw.println(c.id + "," + c.name + "," + c.email + "," + c.skills + "," + c.role + "," + c.stage + "," + c.date);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new ATS().setVisible(true));
    }
}