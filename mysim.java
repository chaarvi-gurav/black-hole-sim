import javax.swing.*;
import java.awt.*;

public class mysim extends JPanel
{
    static final double RS = 2.0;      // Schwarzschild radius (world units)
    static final double SCALE = 20;    // pixels per world unit

    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        int cx = getWidth()/2;
        int cy = getHeight()/2;
        int r = (int)(RS * SCALE);
        g.setColor(Color.BLACK);
        g.fillOval(cx-r, cy-r, 2*r, 2*r);

    }

    public static void main(String[] args)
    {
        JFrame f = new JFrame("sim");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new mysim());
        f.setSize(900, 900);
        f.setVisible(true);
    }
}