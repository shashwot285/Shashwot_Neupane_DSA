import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

// Question 5a: Tourist Spot Optimizer — GUI with Heuristic-Based Itinerary Planner
// Uses a Greedy heuristic to generate a near-optimal travel path.
// Also runs brute-force on small datasets to compare accuracy vs performance.
// GUI: Swing (JFrame, JTabbedPane, Canvas map, JTable comparison).
public class TouristOptimizerApp extends JFrame {

    // ── Data ──────────────────────────────────────────────────────────────────
    static class Spot {
        String name; double lat, lon; int fee; String open, close;
        String[] tags; double visitHrs;
        Spot(String n, double la, double lo, int f, String o, String c, String[] t, double v) {
            name=n; lat=la; lon=lo; fee=f; open=o; close=c; tags=t; visitHrs=v;
        }
    }

    static final Spot[] SPOTS = {
        new Spot("Pashupatinath",  27.7104,85.3488,100,"06:00","18:00",new String[]{"culture","religious"},1.5),
        new Spot("Swayambhunath",  27.7149,85.2906,200,"07:00","17:00",new String[]{"culture","heritage"}, 1.5),
        new Spot("Garden of Dreams",27.7125,85.3170,150,"09:00","21:00",new String[]{"nature","relaxation"},1.0),
        new Spot("Chandragiri Hills",27.6616,85.2458,700,"09:00","17:00",new String[]{"nature","adventure"},3.0),
        new Spot("Durbar Square",  27.7048,85.3076,100,"10:00","17:00",new String[]{"culture","heritage"}, 2.0),
        new Spot("Boudhanath",     27.7215,85.3621,400,"06:00","20:00",new String[]{"culture","religious"},1.5),
        new Spot("Nagarkot",       27.7150,85.5200,  0,"05:00","19:00",new String[]{"nature","adventure"}, 2.0),
    };
    static final String[] ALL_TAGS = {"adventure","culture","heritage","nature","relaxation","religious"};

    // ── GUI fields ────────────────────────────────────────────────────────────
    private JTextField budgetF, timeF, startF;
    private Map<String,JCheckBox> tagCBs = new LinkedHashMap<>();
    private JTextArea resultArea, compareArea;
    private MapCanvas mapCanvas;
    private JLabel statusBar;
    private List<Spot> lastRoute = new ArrayList<>();

    public TouristOptimizerApp() {
        super("Tourist Spot Optimizer — Kathmandu");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(980, 760);
        setLocationRelativeTo(null);
        buildUI();
        setVisible(true);
    }

    void buildUI() {
        setLayout(new BorderLayout(4,4));

        // Header
        JLabel hdr = new JLabel("  Tourist Spot Optimizer — Nepal", SwingConstants.LEFT);
        hdr.setFont(new Font("SansSerif",Font.BOLD,16));
        hdr.setOpaque(true); hdr.setBackground(new Color(44,62,80)); hdr.setForeground(Color.WHITE);
        hdr.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // Input panel
        JPanel inp = new JPanel(new GridBagLayout());
        inp.setBorder(BorderFactory.createTitledBorder("Your Preferences"));
        inp.setBackground(new Color(240,244,248));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4,6,4,6); g.anchor = GridBagConstraints.WEST;

        g.gridx=0; g.gridy=0; inp.add(new JLabel("Budget (Rs.):"), g);
        g.gridx=1; budgetF = new JTextField("1500",8); inp.add(budgetF, g);
        g.gridx=2; inp.add(new JLabel("Time (hrs):"), g);
        g.gridx=3; timeF = new JTextField("8",5); inp.add(timeF, g);
        g.gridx=4; inp.add(new JLabel("Start Hour:"), g);
        g.gridx=5; startF = new JTextField("9",4); inp.add(startF, g);

        g.gridx=0; g.gridy=1; inp.add(new JLabel("Interests:"), g);
        for (int i=0; i<ALL_TAGS.length; i++) {
            JCheckBox cb = new JCheckBox(cap(ALL_TAGS[i]), true);
            cb.setBackground(new Color(240,244,248));
            tagCBs.put(ALL_TAGS[i], cb);
            g.gridx=i+1; inp.add(cb, g);
        }

        // Buttons
        JPanel btnP = new JPanel(new FlowLayout(FlowLayout.CENTER,10,6));
        btnP.setBackground(new Color(240,244,248));
        JButton findBtn = btn("Find Itinerary (Greedy)",  new Color(39,174,96));
        JButton cmpBtn  = btn("Brute-Force Compare",       new Color(41,128,185));
        JButton clrBtn  = btn("Clear",                     new Color(149,165,166));
        findBtn.addActionListener(e -> runGreedy());
        cmpBtn .addActionListener(e -> runCompare());
        clrBtn .addActionListener(e -> clear());
        btnP.add(findBtn); btnP.add(cmpBtn); btnP.add(clrBtn);

        JPanel top = new JPanel(new BorderLayout());
        top.add(hdr, BorderLayout.NORTH);
        top.add(inp, BorderLayout.CENTER);
        top.add(btnP, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        resultArea  = area(); tabs.addTab("Itinerary",   new JScrollPane(resultArea));
        mapCanvas   = new MapCanvas(); tabs.addTab("Map View", mapCanvas);
        compareArea = area(); tabs.addTab("Comparison",  new JScrollPane(compareArea));
        add(tabs, BorderLayout.CENTER);

        statusBar = new JLabel("  Ready.");
        statusBar.setOpaque(true); statusBar.setBackground(new Color(44,62,80));
        statusBar.setForeground(new Color(220,220,220));
        statusBar.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
        add(statusBar, BorderLayout.SOUTH);
    }

    JTextArea area() {
        JTextArea a = new JTextArea();
        a.setFont(new Font("Monospaced",Font.PLAIN,11));
        a.setEditable(false); return a;
    }
    JButton btn(String t, Color bg) {
        JButton b = new JButton(t);
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif",Font.BOLD,11));
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    String cap(String s) { return s.isEmpty()?s:Character.toUpperCase(s.charAt(0))+s.substring(1); }

    // ── Inputs ────────────────────────────────────────────────────────────────
    int[] inputs() {
        try {
            int b=Integer.parseInt(budgetF.getText().trim());
            int t=Integer.parseInt(timeF.getText().trim());
            int s=Integer.parseInt(startF.getText().trim());
            if(b<=0||t<=0||s<0||s>23) throw new NumberFormatException();
            return new int[]{b,t,s};
        } catch(NumberFormatException e) {
            JOptionPane.showMessageDialog(this,"Invalid inputs. Budget>0, Time>0, Hour 0-23.");
            return null;
        }
    }
    Set<String> interests() {
        Set<String> s=new HashSet<>();
        for(Map.Entry<String,JCheckBox> e:tagCBs.entrySet()) if(e.getValue().isSelected()) s.add(e.getKey());
        return s;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    static double dist(Spot a, Spot b) {
        double dx=a.lat-b.lat, dy=a.lon-b.lon; return Math.sqrt(dx*dx+dy*dy);
    }
    static double travel(Spot a, Spot b) { return dist(a,b)*1.5; }
    static int score(Spot s, Set<String> interests) {
        int n=0; for(String t:s.tags) if(interests.contains(t)) n++; return n;
    }
    static List<Spot> filter(int budget, Set<String> interests) {
        List<Spot> r=new ArrayList<>();
        for(Spot s:SPOTS) {
            if(s.fee>budget) continue;
            if(!interests.isEmpty()&&score(s,interests)==0) continue;
            r.add(s);
        }
        return r;
    }

    // ── Greedy Heuristic ──────────────────────────────────────────────────────
    // At each step: pick unvisited spot with highest (interestScore*10 - travel*5)
    // that fits within remaining time and budget.
    static class Visit { Spot spot; double arrive; String reason; }

    static List<Visit> greedy(List<Spot> cands, int budget, int time, Set<String> interests, int start) {
        List<Visit> visited = new ArrayList<>();
        List<Spot>  avail   = new ArrayList<>(cands);
        avail.sort((a,b)->{ int d=score(b,interests)-score(a,interests); return d!=0?d:a.fee-b.fee; });
        double remB=budget, remT=time;
        Spot cur=null; double curH=start;

        while (!avail.isEmpty() && remT>0) {
            Spot best=null; double bestScore=Double.NEGATIVE_INFINITY, bestTravel=0;
            for(Spot s:avail) {
                double tr=cur==null?0:travel(cur,s);
                if(tr+s.visitHrs>remT||s.fee>remB) continue;
                double sc=score(s,interests)*10.0-tr*5;
                if(sc>bestScore){ bestScore=sc; best=s; bestTravel=tr; }
            }
            if(best==null) break;
            curH+=bestTravel;
            Visit v=new Visit();
            v.spot=best; v.arrive=curH;
            v.reason="Tags: "+Arrays.toString(best.tags)+", fee: Rs."+best.fee+", travel: "+String.format("%.2f",bestTravel)+"h";
            visited.add(v);
            remB-=best.fee; remT-=(bestTravel+best.visitHrs); curH+=best.visitHrs;
            cur=best; avail.remove(best);
        }
        return visited;
    }

    // ── Brute-Force (≤6 spots, checks all permutations) ──────────────────────
    static List<Spot> bruteForce(List<Spot> cands, int budget, int time, Set<String> interests, int start) {
        List<Spot> small=cands.subList(0,Math.min(cands.size(),6));
        List<Spot> bestR=new ArrayList<>();
        int bestSc=-1;
        for(List<Spot> perm:perms(small)) {
            List<Spot> vis=new ArrayList<>();
            double rb=budget,rt=time; Spot c=null; int sc=0;
            for(Spot s:perm){
                double tr=c==null?0:travel(c,s);
                if(tr+s.visitHrs>rt||s.fee>rb) continue;
                vis.add(s); rb-=s.fee; rt-=(tr+s.visitHrs); sc+=score(s,interests); c=s;
            }
            if(sc>bestSc){ bestSc=sc; bestR=vis; }
        }
        return bestR;
    }

    static List<List<Spot>> perms(List<Spot> l) {
        List<List<Spot>> r=new ArrayList<>();
        if(l.size()<=1){ r.add(new ArrayList<>(l)); return r; }
        for(int i=0;i<l.size();i++){
            Spot cur=l.get(i); List<Spot> rest=new ArrayList<>(l); rest.remove(i);
            for(List<Spot> p:perms(rest)){ p.add(0,cur); r.add(p); }
        }
        return r;
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    void runGreedy() {
        int[] inp=inputs(); if(inp==null) return;
        Set<String> ints=interests();
        List<Spot> cands=filter(inp[0],ints);
        if(cands.isEmpty()){ JOptionPane.showMessageDialog(this,"No spots match. Adjust budget/interests."); return; }

        List<Visit> visits=greedy(cands,inp[0],inp[1],ints,inp[2]);
        lastRoute=new ArrayList<>();
        for(Visit v:visits) lastRoute.add(v.spot);

        StringBuilder sb=new StringBuilder("=".repeat(65)+"\n  GREEDY ITINERARY\n"+"=".repeat(65)+"\n\n");
        int tc=0;
        for(int i=0;i<visits.size();i++){
            Visit v=visits.get(i);
            int h=(int)v.arrive, m=(int)((v.arrive%1)*60);
            sb.append(String.format("  Stop %d: %s%n    Arrive: %02d:%02d | Duration: %.1fh | Fee: Rs.%d%n    Decision: %s%n%n",
                i+1,v.spot.name,h,m,v.spot.visitHrs,v.spot.fee,v.reason));
            tc+=v.spot.fee;
        }
        sb.append("-".repeat(65)).append(String.format("%n  Total spots: %d | Total cost: Rs.%d%n",visits.size(),tc));
        resultArea.setText(sb.toString());
        mapCanvas.setRoute(lastRoute);
        statusBar.setText("  Greedy: "+visits.size()+" spots | Cost: Rs."+tc);
    }

    void runCompare() {
        int[] inp=inputs(); if(inp==null) return;
        Set<String> ints=interests();
        List<Spot> cands=filter(inp[0],ints);
        if(cands.isEmpty()){ JOptionPane.showMessageDialog(this,"No spots match."); return; }

        List<Visit> gv=greedy(cands,inp[0],inp[1],ints,inp[2]);
        List<Spot>  bv=bruteForce(cands,inp[0],inp[1],ints,inp[2]);

        int gSpots=gv.size(), gCost=gv.stream().mapToInt(v->v.spot.fee).sum();
        int gScore=gv.stream().mapToInt(v->score(v.spot,ints)).sum();
        int bSpots=bv.size(), bCost=bv.stream().mapToInt(s->s.fee).sum();
        int bScore=bv.stream().mapToInt(s->score(s,ints)).sum();

        StringBuilder sb=new StringBuilder("=".repeat(65)+"\n  GREEDY vs BRUTE-FORCE\n"+"=".repeat(65)+"\n\n");
        sb.append(String.format("  Dataset: %d spots (brute-force capped at 6)%n%n",Math.min(cands.size(),6)));
        sb.append(String.format("  %-26s %10s %14s%n","Metric","Greedy","Brute-Force"));
        sb.append("  "+"-".repeat(52)+"\n");
        sb.append(String.format("  %-26s %10d %14d%n","Spots Visited",gSpots,bSpots));
        sb.append(String.format("  %-26s %10d %14d%n","Total Cost (Rs.)",gCost,bCost));
        sb.append(String.format("  %-26s %10d %14d%n","Interest Score",gScore,bScore));
        sb.append("\n  Discussion:\n");
        sb.append("  Greedy  O(n^2): fast, scalable, near-optimal.\n");
        sb.append("  Brute   O(n!):  exact but infeasible beyond ~10 spots.\n");
        sb.append("  7 spots = 5040 checks, 10 spots = 3,628,800 checks.\n");
        compareArea.setText(sb.toString());

        // Switch to comparison tab
        Container c = getContentPane();
        for(Component comp:c.getComponents()) if(comp instanceof JTabbedPane tp){ tp.setSelectedIndex(2); break; }
    }

    void clear() { resultArea.setText(""); compareArea.setText(""); mapCanvas.setRoute(new ArrayList<>()); statusBar.setText("  Cleared."); }

    // ── Map Canvas ────────────────────────────────────────────────────────────
    static class MapCanvas extends JPanel {
        List<Spot> route = new ArrayList<>();
        void setRoute(List<Spot> r) { route=r; repaint(); }

        @Override
        protected void paintComponent(Graphics g2) {
            super.paintComponent(g2);
            Graphics2D g=(Graphics2D)g2;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(214,234,248)); g.fillRect(0,0,getWidth(),getHeight());

            double minLat=99,maxLat=-99,minLon=999,maxLon=-999;
            for(Spot s:SPOTS){ minLat=Math.min(minLat,s.lat); maxLat=Math.max(maxLat,s.lat); minLon=Math.min(minLon,s.lon); maxLon=Math.max(maxLon,s.lon); }
            int M=45,W=getWidth()-2*M,H=getHeight()-2*M;
            double latR=maxLat-minLat, lonR=maxLon-minLon;

            // All spots (grey)
            for(Spot s:SPOTS){
                int cx=(int)(M+(s.lon-minLon)/Math.max(lonR,0.001)*W);
                int cy=(int)(getHeight()-M-(s.lat-minLat)/Math.max(latR,0.001)*H);
                g.setColor(new Color(189,195,199)); g.fillOval(cx-6,cy-6,12,12);
                g.setColor(Color.DARK_GRAY); g.setFont(new Font("Arial",Font.PLAIN,9));
                g.drawString(s.name.length()>13?s.name.substring(0,13):s.name,cx+8,cy+4);
            }
            if(route.isEmpty()) return;

            int[] rx=new int[route.size()], ry=new int[route.size()];
            for(int i=0;i<route.size();i++){
                rx[i]=(int)(M+(route.get(i).lon-minLon)/Math.max(lonR,0.001)*W);
                ry[i]=(int)(getHeight()-M-(route.get(i).lat-minLat)/Math.max(latR,0.001)*H);
            }
            // Route line
            g.setColor(new Color(39,174,96)); g.setStroke(new BasicStroke(2f));
            for(int i=0;i<route.size()-1;i++) g.drawLine(rx[i],ry[i],rx[i+1],ry[i+1]);
            // Visited nodes
            for(int i=0;i<route.size();i++){
                g.setColor(new Color(231,76,60)); g.fillOval(rx[i]-9,ry[i]-9,18,18);
                g.setColor(Color.WHITE); g.setFont(new Font("Arial",Font.BOLD,10));
                g.drawString(String.valueOf(i+1),rx[i]-4,ry[i]+4);
            }
            g.setColor(Color.DARK_GRAY); g.setFont(new Font("Arial",Font.PLAIN,9));
            g.drawString("Red=visited (numbered)  Grey=not visited  Green=route",8,getHeight()-6);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TouristOptimizerApp::new);
    }
}