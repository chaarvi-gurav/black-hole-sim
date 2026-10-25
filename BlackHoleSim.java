import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

// black hole accretion disk using the Paczynski-Wiita potential
// click = drop a probe, space = pause, r = reset
public class BlackHoleSim extends JPanel implements ActionListener {

    // G = M = c = 1
    static final double GM = 1.0;
    static final double RS = 2.0;
    static final double ISCO = 3 * RS;
    static final double KILL_R = RS * 1.1;
    static final double DT = 0.1;
    static final int SUBSTEPS = 5;
    static final double DRAG = 0.0005;

    static final int SIZE = 900;
    static final double SCALE = 14.0;
    static final int NUM_PARTICLES = 2500;

    static Random rand = new Random();

    static class Particle {
        double x, y, vx, vy;
        double oldX = Double.NaN, oldY;
        boolean probe;
    }

    ArrayList<Particle> parts = new ArrayList<>();
    BufferedImage trail;
    Timer timer = new Timer(16, this);
    boolean paused = false;
    int swallowed = 0;

    public BlackHoleSim() {
        setPreferredSize(new Dimension(SIZE, SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);
        reset();

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                double wx = (e.getX() - getWidth() / 2.0) / SCALE;
                double wy = (e.getY() - getHeight() / 2.0) / SCALE;
                addProbe(wx, wy);
            }
        });

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) paused = !paused;
                if (e.getKeyCode() == KeyEvent.VK_R) reset();
            }
        });

        timer.start();
    }

    void reset() {
        parts.clear();
        swallowed = 0;
        trail = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < NUM_PARTICLES; i++) {
            parts.add(newDiskParticle(false));
        }
    }

    double orbitSpeed(double r) {
        return Math.sqrt(GM * r) / (r - RS);
    }

    Particle newDiskParticle(boolean outerOnly) {
        Particle p = new Particle();
        double r;
        if (outerOnly) {
            r = 20 + rand.nextDouble() * 4;
        } else {
            r = ISCO + 0.5 + Math.pow(rand.nextDouble(), 1.4) * 18;
        }

        double a = rand.nextDouble() * 2 * Math.PI;
        p.x = r * Math.cos(a);
        p.y = r * Math.sin(a);

        // little bit of random scatter so it isn't perfectly circular
        double v = orbitSpeed(r) * (1 + rand.nextGaussian() * 0.03);
        p.vx = -v * Math.sin(a);
        p.vy = v * Math.cos(a);
        return p;
    }

    void addProbe(double x, double y) {
        double r = Math.hypot(x, y);
        if (r <= KILL_R) return;

        Particle p = new Particle();
        p.probe = true;
        p.x = x;
        p.y = y;

        // slower than circular so it falls in
        double v = orbitSpeed(r) * 0.7;
        p.vx = -v * y / r;
        p.vy = v * x / r;
        parts.add(p);
    }

    double[] accel(double x, double y) {
        double r = Math.hypot(x, y);
        double d = r - RS;
        if (d <= 0) d = 0.001; // avoid dividing by 0
        double a = GM / (d * d);
        return new double[] { -a * x / r, -a * y / r };
    }

    // leapfrog step
    void step(Particle p) {
        double[] a = accel(p.x, p.y);
        p.vx += a[0] * DT / 2;
        p.vy += a[1] * DT / 2;

        p.x += p.vx * DT;
        p.y += p.vy * DT;

        a = accel(p.x, p.y);
        p.vx += a[0] * DT / 2;
        p.vy += a[1] * DT / 2;

        if (!p.probe) {
            p.vx *= 1 - DRAG;
            p.vy *= 1 - DRAG;
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (!paused) {
            for (int i = 0; i < parts.size(); i++) {
                Particle p = parts.get(i);

                for (int s = 0; s < SUBSTEPS; s++) {
                    step(p);
                    if (Math.hypot(p.x, p.y) < KILL_R) break;
                }

                double r = Math.hypot(p.x, p.y);
                if (r < KILL_R || r > 60) {
                    if (r < KILL_R) swallowed++;

                    if (p.probe) {
                        parts.remove(i);
                        i--;
                    } else {
                        parts.set(i, newDiskParticle(true));
                    }
                }
            }
        }
        repaint();
    }

    // hot/blue near the center, cooler/red further out
    Color diskColor(double r) {
        double t = (r - ISCO) / 18.0;
        t = Math.max(0, Math.min(1, t));
        int red, green, blue;

        if (t < 0.5) {
            double u = t * 2;
            red = (int) (200 + 55 * u);
            green = (int) (220 - 50 * u);
            blue = (int) (255 - 195 * u);
        } else {
            double u = (t - 0.5) * 2;
            red = (int) (255 - 55 * u);
            green = (int) (170 - 130 * u);
            blue = (int) (60 - 40 * u);
        }
        return new Color(red, green, blue, 230);
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        // fade old trails a bit each frame
        Graphics2D tg = trail.createGraphics();
        if (!paused) {
            tg.setColor(new Color(0, 0, 0, 45));
            tg.fillRect(0, 0, SIZE, SIZE);
        }

        for (Particle p : parts) {
            double sx = SIZE / 2.0 + p.x * SCALE;
            double sy = SIZE / 2.0 + p.y * SCALE;

            if (!Double.isNaN(p.oldX) && !paused) {
                if (p.probe) {
                    tg.setColor(new Color(120, 255, 160));
                } else {
                    tg.setColor(diskColor(Math.hypot(p.x, p.y)));
                }
                tg.drawLine((int) p.oldX, (int) p.oldY, (int) sx, (int) sy);
            }
            p.oldX = sx;
            p.oldY = sy;
        }
        tg.dispose();

        g2.drawImage(trail, cx - SIZE / 2, cy - SIZE / 2, null);

        // glow around the horizon
        double hr = KILL_R * SCALE;
        float gr = (float) (hr * 2.2);
        RadialGradientPaint glow = new RadialGradientPaint(
            new Point(cx, cy), gr,
            new float[] { 0.45f, 0.6f, 1.0f },
            new Color[] {
                new Color(255, 190, 90, 150),
                new Color(255, 140, 40, 60),
                new Color(255, 120, 30, 0)
            });
        g2.setPaint(glow);
        g2.fill(new Ellipse2D.Double(cx - gr, cy - gr, gr * 2, gr * 2));

        // the black hole itself
        g2.setColor(Color.BLACK);
        g2.fill(new Ellipse2D.Double(cx - hr, cy - hr, hr * 2, hr * 2));
        g2.setColor(new Color(255, 200, 120, 200));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new Ellipse2D.Double(cx - hr, cy - hr, hr * 2, hr * 2));

        // text overlay
        g2.setColor(new Color(200, 200, 200));
        g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g2.drawString("Click: Drop Probe    Space: Pause    R: Reset", 12, 20);
        g2.drawString("Rs = " + RS + "    ISCO = " + ISCO + "    Swallowed = " + swallowed
                + (paused ? "    [PAUSED]" : ""), 12, 38);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Black Hole Simulation");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(new BlackHoleSim());
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}