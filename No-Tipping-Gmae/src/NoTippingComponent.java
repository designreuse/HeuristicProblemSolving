// NoTippingComponent
//
// version 1.X
//
// Tyler Neylon, 2002
//

import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

class Weight2 {
    public Weight2(int w, int whose, int place, int p) {
        this.w = w;
        this.whose = whose;
        this.place = place;
        position = p;

        do_draw = true;
    }

    public int w;
    public int whose;
    public int place;
    public boolean do_draw;
    public int position;
}

class Move {
    public Move(int w_index, int who, int p) {
        this.w_index = w_index;
        this.who = who;
        position = p;
    }

    public int w_index, who, position;
}

public class NoTippingComponent
        extends Component
        implements MouseListener,
        MouseMotionListener, ActionListener, ItemListener {

    private int width, height, meter, ss_width, horizon, ss_height, f_height;
    private Vector weights;
    private int weight_selected, selected_x, selected_y;
    private int whose_turn;
    private int left_torque, right_torque;
    private boolean game_over;
    private Stack moves;
    private int phase, num_on_grass, who_lost;
    private BufferedImage image;
    private HelpFrame help_frame;
    private int most_recent_position = 0;
    private int most_recent_weight = 0;

    private Rectangle computeRectangle(Weight2 w) {
        int h = w.w*2 + 17;
        if (w.place == 0) {
            if (w.whose == 0) {
                return new Rectangle(20*w.w+10, 15 + (8-w.w)*2, 12, h);
            } else {
                return new Rectangle(width-20*(8-w.w)-10, 15 + (8-w.w)*2, 12, h);
            }
        } else if (w.place == 1) {
            return new Rectangle(screen_x(w.position) - 6,
                    horizon-ss_height-f_height-h,
                    12, h);
        } else {
            // it's on the grass (removed)
            return new Rectangle(20*w.position+10, height-40+(8-w.w)*2, 12, h);
        }
    }

    public void actionPerformed(ActionEvent e) {
        System.out.println("actionPerformed(" + e.getActionCommand() + ")");
        if (e.getActionCommand().equals("Connect")) {
            System.out.println("hi");
            System.out.println(red);
            System.out.println(blue);
            if (red != null && blue != null) {
                try {
                    startGame();
                } catch (Exception ev) {
                    System.err.println(ev.getMessage());
                }
            }
            return;
        }
        if (e.getActionCommand().equals("Undo")) {

            System.out.println("Undo");
            if (moves.size() == 0) return;
            Move m = (Move)moves.pop();
            if (phase == 1) {
                num_on_grass -- ;
            }
            if (moves.size() == 13) phase = 0;
            whose_turn = 1 - whose_turn;
            Weight2 w = (Weight2)weights.get(m.w_index);
            if (m.position == -1) {
                w.place = 0;
            } else {
                w.place = 1;
                w.position = m.position;
            }

            if (game_over) {
                game_over = false;
                int i, n = weights.size() - 1; // less one to avoid the invible center of gravity
                for (i=0; i<n; i++) {
                    w = (Weight2)weights.get(i);
                    w.do_draw = true;
                }
            }

            update(getGraphics());
            return;

            // repaint();

        } else if (e.getActionCommand().equals("Restart")) {
            most_recent_position = 0;
            most_recent_weight = 0;
            begin();
            update(getGraphics());
            return;
        } else if (e.getActionCommand().equals("Help")) {
            help_frame.setVisible(true);
        }

        String s = e.getActionCommand();
        if (phase == 0) {
            // look for w,p format
            int i = s.indexOf(',');
            if (i == -1) {
                System.out.println("Syntax Error");
                return;
            }
            String s1 = s.substring(0, i);
            String s2 = s.substring(i+1, s.length());
            int wt = new Integer(s1).intValue();
            int p = new Integer(s2).intValue();
            int n = weights.size() - 1;
            // check that the destination is free
            for (i=0; i<n; i++) {
                Weight2 w = (Weight2)weights.get(i);
                if (w.position == p && w.place == 1) {
                    return;
                }
            }
            for (i=0; i<n; i++) {
                Weight2 w = (Weight2)weights.get(i);
                if (wt == w.w && w.whose == whose_turn && w.place == 0) {
                    w.place = 1;
                    w.position = p;
                    moves.push(new Move(i, whose_turn, -1));
                    if (moves.size() == 14) phase++;
                    whose_turn = 1 - whose_turn;
                    update(getGraphics());
                    // repaint();
                    return;
                }
            }
        } else {
            // look for p format
            int p = new Integer(s).intValue();

            // find the moving weight
            int i, n = weights.size() - 1;
            for (i=0; i<n; i++) {
                Weight2 w = (Weight2)weights.get(i);
                if (w.position == p && w.place == 1) {
                    w.place = 2;
                    moves.push(new Move(i, whose_turn, w.position));
                    w.position = num_on_grass;
                    num_on_grass++;
                    whose_turn = 1 - whose_turn;
                    update(getGraphics());
                    // repaint();
                    return;
                }
            }
        }
    }

    public Dimension getPreferredSize() {
        System.out.println("getPreferredSize()");
        return new Dimension(500, 300);
    }

    public void setSize(Dimension d) {
        System.out.println("setSize(Dimension)");
        setSize(d.width, d.height);
    }

    public void setSize(int width, int height) {
        System.out.println("setSize(int, int)");
        this.width = width;
        this.height = height;
        super.setSize(width, height);
    }

    public void setBounds(Rectangle r) {
        System.out.println("setBounds(Rectangle)");
        setBounds(r.x, r.y, r.width, r.height);
    }

    public void setBounds(int x, int y, int width, int height) {
        System.out.println("setBounds(" + x + ", " +
                y + ", " + width + ", " + height + ")");

        if (this.width == width && this.height == height) return;

        this.width = width;
        this.height = height;

        super.setBounds(x, y, width, height);

        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    public NoTippingComponent() {

        System.out.println("NoTippingComponent() v1.4" );

        addMouseListener(this);
        addMouseMotionListener(this);

        help_frame = new HelpFrame();
        help_frame.setVisible(false);

        begin();
    }

    public void update(Graphics g) {
        System.out.println("update(Graphics()");

        paint(g);
    }

    private void begin() {

        weights = new Vector();
        weight_selected = -1;
        whose_turn = 0;
        game_over = false;
        moves = new Stack();
        phase = 0;
        num_on_grass = 0;

        int i;
        for (i=1; i<=15; i++) {
            Weight2 w = new Weight2(i, 0, 0, 0);
            weights.add(w);
            w = new Weight2(i, 1, 0, 0);
            weights.add(w);
        }
        weights.add(new Weight2(3, 2, 1, -4));
        Weight2 w = new Weight2(3, 2, 1, 0);
        w.do_draw = false;
        weights.add(w);


    }


    public void repaint() {
        System.out.println("repaint()");
        // super.repaint();
    }

    public void paint(Graphics real_g) {
        System.out.println("paint(Graphics)");

        horizon = height*4/5;
        ss_height = height/16;
        f_height = height/10;
        ss_width = width - 100 + 16;
        meter = (ss_width - 16) / 50;

        // find the torques
        int n = weights.size();
        int i;
        left_torque = 0;
        right_torque = 0;
        for (i=0; i<n; i++) {
            Weight2 w = (Weight2)weights.get(i);
            if (w.place == 1) {
                left_torque -= (w.position - (-3)) * w.w;
                right_torque -= (w.position - (-1)) * w.w;
            }
        }
        if (left_torque > 0 || right_torque < 0) {
            if (!game_over) {
                who_lost = 1 - whose_turn;
                for (i=0; i<n; i++) {
                    Weight2 w = (Weight2)weights.get(i);
                    if (w.place == 0) {
                        w.do_draw = false;
                    }
                }
            }
            game_over = true;
        }

        Graphics g = image.createGraphics();

        // draw the sky
        g.setColor(new Color(140, 150, 200));
        g.drawRect(0, 0, width, height);
        g.fillRect(0, 0, width, height);

        // draw some clouds
        g.setColor(new Color(250, 240, 230));
        g.fillOval(width - 100, 20, 50, 30);
        g.fillOval(width - 80, 30, 50, 30);
        g.fillOval(width - 90, 25, 40, 36);
        g.fillOval(width - 150, 15, 60, 40);
        g.fillOval(width - 160, 25, 50, 35);
        g.fillOval(width - 130, 30, 55, 35);
        g.fillOval(width - 50, 10, 40, 30);
        g.fillOval(width - 65, 15, 45, 35);
        g.fillOval(width - 130, 10, 80, 30);

        // draw the grass
        g.setColor(new Color(0, 255, 0));
        g.drawRect(0, horizon, width-1, height-horizon);
        g.setColor(new Color(0, 255, 0));
        g.fillRect(0, horizon, width-1, height-horizon);

        // draw the fulcra
        g.setColor(new Color(180, 120, 0));
        for (i = 0; i < 10; i++) {
            g.drawLine(screen_x(-3)-i, horizon, screen_x(-3), horizon-f_height);
            g.drawLine(screen_x(-3), horizon-f_height, screen_x(-3)+i, horizon);

            g.drawLine(screen_x(-1)-i, horizon, screen_x(-1), horizon-f_height);
            g.drawLine(screen_x(-1), horizon-f_height, screen_x(-1)+i, horizon);
        }

        // draw the torques
        g.setColor(new Color(0, 0, 0));
        Font f = g.getFont();
        String left_s = Integer.toString(left_torque);
        String right_s = Integer.toString(right_torque);
        Graphics2D g2 = (Graphics2D) g;
        FontRenderContext frc = g2.getFontRenderContext();

        int s_width = (int)(f.getStringBounds(left_s, frc).getWidth());
        int s_height = (int)(f.getStringBounds(left_s, frc).getHeight());
//        g.drawString(left_s, screen_x(-3)-s_width/2, horizon+s_height+3);

        s_width = (int)(f.getStringBounds(right_s, frc).getWidth());
        s_height = (int)(f.getStringBounds(right_s, frc).getHeight());
//        g.drawString(right_s, screen_x(-1)-s_width/2, horizon+s_height+3);

        if (game_over) {
            Font font = new Font("SansSerif", Font.BOLD, 30);
            Font old_f = g.getFont();
            g.setFont(font);
            g.drawString("Bummer, dude", 30, 100);

            if (who_lost == 0) {
                // red lost
                g.drawString("Red lost :(", 30, 140);
            } else {
                // blue lost
                g.drawString("Blue lost :(", 30, 140);
            }

            g.setFont(old_f);

            double theta = 0;
            int rotate_x = 0;
            if (left_torque > 0) {
                theta = -Math.atan((double)f_height/(double)(screen_x(-3)-screen_x(-10)+8));
                rotate_x = screen_x(-3);
            }
            if (right_torque < 0) {
                theta = Math.atan((double)f_height/(double)(screen_x(10)-screen_x(-1)+8));
                rotate_x = screen_x(-1);
            }
            System.out.println("theta is " + theta);
            System.out.println("taking atan of " + ((double)f_height/(double)(screen_x(10)-screen_x(-1)+8)));
            g2.rotate(theta, rotate_x, horizon-f_height);

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        // draw the see-saw
        g2.setColor(new Color(230, 235, 240));
        g2.fillRect(42, horizon-ss_height-f_height, ss_width, ss_height);
        g2.setColor(new Color(60, 20, 0));
        for (i=-25; i<=25; i++) {
            g2.drawLine(screen_x(i), horizon-ss_height-f_height,
                    screen_x(i), horizon-f_height-ss_height*3/4);
            if ((i%2) == 0) {
                String s = new String(Integer.toString(i));
                //FontMetrics fm = new FontMetrics(g.getFont());
                f = g.getFont();
                int font_width = (int)(f.getStringBounds(s, g2.getFontRenderContext()).getWidth());
                g2.drawString(Integer.toString(i), screen_x(i)-font_width/2, horizon-f_height-1);
            }
        }

        // draw the weights
        n = weights.size();
        for (i=0; i<n; i++) {
            Color c;
            Weight2 w = (Weight2)weights.get(i);
            switch(w.whose) {
                case 0:
                    c = new Color(200, 0, 0);
                    break;
                case 1:
                    c = new Color(0, 0, 200);
                    break;
                default:
                    c = new Color(0, 180, 0);
                    break;
            }
            g2.setColor(c);
            Rectangle r = computeRectangle(w);
            if (w.do_draw && (!game_over || w.place == 1)) {
                g2.fillRect(r.x, r.y, r.width, r.height);
                g2.setColor(new Color(255, 255, 255));
                g2.drawString(Integer.toString(w.w), r.x + 2, r.y + r.height - 2);
            }
        }

        if (weight_selected >= 0) {
            Weight2 w = (Weight2)weights.get(weight_selected);
            Color c;
            switch(w.whose) {
                case 0:
                    c = new Color(200, 0, 0);
                    break;
                case 1:
                    c = new Color(0, 0, 200);
                    break;
                default:
                    c = new Color(0, 180, 0);
                    break;
            }
            g2.setColor(c);
            Rectangle r = new Rectangle(selected_x, selected_y, 12, w.w*2+17);
            g2.fillRect(r.x, r.y, r.width, r.height);
            g2.setColor(new Color(255, 255, 255));
            g2.drawString(Integer.toString(w.w), r.x+2, r.y+r.height-2);
        }

        // draw the status string
        if (!game_over) {
            Color c;
            String s;
            if (whose_turn == 0) {
                // red's turn
                c = new Color(150, 0, 0);
                s = new String("It's Red's turn");
            } else {
                // blue's turn
                c = new Color(0, 0, 150);
                s = new String("It's Blue's turn");
            }
            g.setColor(c);
            g.drawString(s, 30, 70);

            c = new Color(0, 0, 0);
            g.setColor(c);
            if (phase == 0) {
                s = new String("Adding phase");
            } else {
                s = new String("Removing phase");
            }
            g.drawString(s, 30, 90);
        }

        // render the image to the screen

        real_g.drawImage(image, 0, 0, null);

    }

    public void mouseDragged(MouseEvent e) {

        System.out.println("mouseDragged(MouseEvent)");
        int x = e.getX();
        int y = e.getY();
        if (x < 0) x = 0;
        if (x >= width) x = width-1;
        if (y < 0) y = 0;
        if (y >= height) y = height-1;


        if (weight_selected < 0) return;

        Weight2 w = (Weight2)weights.get(weight_selected);
        int h = w.w*2+17;
        selected_x = x-6;
        selected_y = y-h/2;
        update(getGraphics());
        // repaint();
    }

    public void mouseMoved(MouseEvent e) {}


    // MouseListener stuff

    public void mouseClicked(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {

        System.out.println("mousePressed(MouseEvent)");

        // check if it was pressed on a weight
        int n = weights.size();
        int i;
        for (i=0; i<n; i++) {
            Weight2 w = (Weight2)weights.get(i);
            Rectangle r = computeRectangle(w);
            if (r.contains(e.getPoint())) {
                if (w.whose != whose_turn && phase == 0) continue;
                System.out.println("weight hit");
                weight_selected = i;
                w.do_draw = false;
                selected_x = r.x;
                selected_y = r.y;
                update(getGraphics());
                // repaint();
                break;
            }
        }

    }

    public void mouseReleased(MouseEvent e) {

        System.out.println("mouseReleased(MouseEvent)");

        if (weight_selected < 0) return;
        Weight2 w = (Weight2)weights.get(weight_selected);
        w.do_draw = true;
        Point p = e.getPoint();
        int top = horizon-ss_height-f_height-40;
		/*
		Rectangle ss_rect = new Rectangle(40, top, ss_width+4, 40);
		if (!ss_rect.contains(p)) return;
		*/

        Rectangle grass_rect = new Rectangle(0, horizon, width, height-horizon);
        if (grass_rect.contains(p)) {
            // see if we can remove this piece

            // remove the piece
            w.place = 2;
            moves.push(new Move(weight_selected, whose_turn, w.position));
            w.position = num_on_grass;
            num_on_grass++;
            whose_turn = 1 - whose_turn;
            weight_selected = -1;
            update(getGraphics());

            // repaint();
            return;

        }

        int i;
        for (i=-25; i<=25; i++) {
            Rectangle r = new Rectangle(screen_x(i)-meter/2, top, meter, 50);
            if (r.contains(p)) {
                // check that there isn't already a weight here
                int j, n = weights.size() - 1;
                for (j=0; j<n; j++) {
                    Weight2 w2 = (Weight2)weights.get(j);
                    if ((w2.place == 1) && (w2.position == i)) {
                        weight_selected = -1;
                        update(getGraphics());
                        //repaint();
                        return;
                    }
                }

                // drop the weight here
                w.place = 1;
                w.position = i;
                moves.push(new Move(weight_selected, whose_turn, -1));
                if (moves.size() == 30) phase++;
                whose_turn = 1 - whose_turn;
                break;
            }
        }
        weight_selected = -1;
        update(getGraphics());
        // repaint();

    }

    private int screen_x(int meters) {
        return 42 + ss_width/2 + meters * meter;
    }

    private Socket red, blue;
    private PrintWriter redOut = null;
    private PrintWriter blueOut = null;
    private BufferedReader redIn = null;
    private BufferedReader blueIn = null;
    private long redTime = 0, blueTime = 0;
    private final long timeAllowed = 5 * 60 * 1000;// 5 minutes

    /**
     * Whenever a choice is made in the choice box, a new socket is opened and the game is
     * restarted
     * @param e
     */
    public void itemStateChanged(ItemEvent e) {
        String playerType = ((Choice)e.getItemSelectable()).getName();
        String playerName = (String)e.getItem();
        connectRed(playerType, playerName);
        connectBlue(playerType, playerName);
    }

    private void connectBlue(String playerType, String playerName) {
        if (playerType.equals("blue")) {
            try {
                if (blue != null) {
                    closeBlue();
                }
                openBlue(playerName);
            } catch (Exception ev) {
                System.out.println(ev.getMessage());
            }
        }
    }

    private void openBlue(String playerName) throws IOException {
        blue = new Socket(Player.getHostname(), Player.getPort(playerName));
        blueOut = new PrintWriter(blue.getOutputStream(), true);
        blueIn = new BufferedReader(new InputStreamReader(blue.getInputStream()));
    }

    private void connectRed(String playerType, String playerName) {
        if (playerType.equals("red")) {
            try {
                if (red != null) {
                    closeRed();
                }
                openRed(playerName);
            } catch (Exception ev) {
                System.out.println(ev.getMessage());
            }
        }
    }

    private void openRed(String playerName) throws IOException {
        red = new Socket(Player.getHostname(), Player.getPort(playerName));
        redOut = new PrintWriter(red.getOutputStream(), true);
        redIn = new BufferedReader(new InputStreamReader(red.getInputStream()));
    }

    private void closeBlue() throws IOException {
        blueIn.close();
        blueOut.close();
        blue.close();
    }

    private void closeRed() throws IOException {
        redIn.close();
        redOut.close();
        red.close();
    }

    private void startGame() throws Exception {
        System.out.println("Start Game");
        actionPerformed(new ActionEvent(this, (int) System.currentTimeMillis(), "Restart"));
        redTime = 0;
        blueTime = 0;
        long moveTime;
        while (true) {// Keep playing until one of them loses
            long t1 = sendState();
            long t2 = readMove();
            moveTime = t2 - t1;
            if (color(whose_turn).equals("Red"))
                blueTime += moveTime;
            else
                redTime += moveTime;
            if (redTime > timeAllowed) {// If red loses
                who_lost = 0;
                game_over = true;
            }
            if (blueTime > timeAllowed) {// If blue loses
                who_lost = 1;
                game_over = true;
            }
            if (game_over) {
                closeRed();
                closeBlue();
                break;
            }
        }
    }

    /**
     * Returns the time at which the state was sent
     * @return
     */
    private long sendState() {
        Weight2 w;
        if (phase == 0)
            send("ADDING");
        else
            send("REMOVING");
//        for (int i=0; i<weights.size(); i++) {
//            w = (Weight2)weights.get(i);
//            if (w.do_draw)
//                send(this.most_recent_position + " " + this.most_recent_weight);
//        }
        send(this.most_recent_position + " " + this.most_recent_weight);
        send("STATE END");
        return System.currentTimeMillis();
    }

    /**
     * Returns the time at which the move was returned
     * @return
     */
    private long readMove() throws Exception {
        String move = read();
        long timeRead = System.currentTimeMillis();
        int position = Integer.MAX_VALUE, weight = -1;
        StringTokenizer st = new StringTokenizer(move);
        position = Integer.parseInt(st.nextToken());
        weight = Integer.parseInt(st.nextToken());
        this.most_recent_position = position;
        this.most_recent_weight = weight;
        Weight2 w = getWeight(weight, (phase==1)?position:0);
        if (w == null)// No weight exists
            return Integer.MAX_VALUE;// Make this player lose
        Rectangle source = computeRectangle(w);
        // Generate a mouse click on the weight
        mousePressed(new MouseEvent(this, MouseEvent.MOUSE_PRESSED,
                (int)System.currentTimeMillis(), 1040, (int)source.getCenterX(),
                (int)source.getCenterY(), 1, false, MouseEvent.BUTTON1));
        int destX, destY;
        if (phase==0) {
            destX = screen_x(position);
            destY = horizon-ss_height-f_height-15;
        } else {
            destX = screen_x(position);
            destY = horizon+15;
        }
        // Drag the mouse to the position specified and release it
        generateMouseMovements((int)source.getCenterX(), (int)source.getCenterY(),
                destX, destY);
        if (game_over) {
            return Integer.MAX_VALUE;
        }
        return timeRead;
    }

    private void generateMouseMovements(int x1, int y1, int x2, int y2) throws Exception {
        int t = 1500;// Time to be taken
        double cx = x1, cy = y1;
        double dx = x2-x1, dy = y2 - y1, d12y = (y1 + y2) / 2;
        double vx = dx / t, vy = dy / t;
        while (true) {// Move the block down half the way
            Thread.sleep(1);
            if (!Player.noDelay()) {
                if (Math.abs(cy - d12y) > 3)
                    cy += vy * 30;
                else
                    cy = d12y;
            } else
                cy = d12y;
            mouseDragged(new MouseEvent(this, MouseEvent.MOUSE_DRAGGED,
                    (int)System.currentTimeMillis(), 1040,
                    (int)cx, (int)cy, 1, false, MouseEvent.NOBUTTON));
            if (cy == d12y)
                break;
        }
        while (true) {// Move across above the drop off point
            Thread.sleep(1);
            if (!Player.noDelay()) {
                if (Math.abs(cx - x2) > 3)
                    cx += vx * 30;
                else
                    cx = x2;
            } else
                cx = x2;
            mouseDragged(new MouseEvent(this, MouseEvent.MOUSE_DRAGGED,
                    (int)System.currentTimeMillis(), 1040,
                    (int)cx, (int)cy, 1, false, MouseEvent.NOBUTTON));
            if (cx == x2)
                break;
        }
        while (true) {// Drop it down
            Thread.sleep(1);
            if (!Player.noDelay()) {
                if (Math.abs(cy - y2) > 3)
                    cy += vy * 30;
                else
                    cy = y2;
            } else
                cy = y2;
            mouseDragged(new MouseEvent(this, MouseEvent.MOUSE_DRAGGED,
                    (int)System.currentTimeMillis(), 1040,
                    (int)cx, (int)cy, 1, false, MouseEvent.NOBUTTON));
            if (cy == y2)
                break;
        }
        mouseReleased(new MouseEvent(this, MouseEvent.MOUSE_RELEASED,
                (int)System.currentTimeMillis(), 16, (int)cx, (int)cy, 1, false, MouseEvent.BUTTON1));
    }

    private Weight2 getWeight(int weight, int position) {
        Weight2 w;
        for (int i=0; i<weights.size(); i++) {
            w = (Weight2)weights.get(i);
            if ((phase != 0 && w.w == weight && w.position == position && w.place == 1 && w.do_draw) || (phase == 0 && whose_turn == w.whose && w.w == weight && w.place == 0 && w.do_draw)) {
                return w;
            }
        }
        return null;
    }

    private String color(int whose) {
        switch (whose) {
            case 0:
                return "Red";
            case 1:
                return "Blue";
            case 2:
                return "Green";
        }
        return "";
    }

    private void send(String command) {
        if (whose_turn == 0)
            redOut.println(command);
        else
            blueOut.println(command);
    }

    private String read() {
        try {
            if (whose_turn == 0)
                return redIn.readLine();
            else
                return blueIn.readLine();
        } catch (IOException io) {
            System.err.println(io.getMessage());
        }
        return null;
    }
}
