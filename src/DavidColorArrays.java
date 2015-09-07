import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import javax.swing.JComponent;

import TUIO.TuioBlob;
import TUIO.TuioCursor;
import TUIO.TuioListener;
import TUIO.TuioObject;
import TUIO.TuioTime;

public class DavidColorArrays extends JComponent implements TuioListener,
        Runnable {

    private static final long serialVersionUID = 1L;
    // A bunch on unimportant stuff
    private Hashtable<Long, TuioDemoObject> objectList = new Hashtable<Long, TuioDemoObject>();
    private Hashtable<Long, Point> objectPointList = new Hashtable<Long, Point>();
    private Hashtable<Long, TuioCursor> cursorList = new Hashtable<Long, TuioCursor>();
    private Hashtable<Long, TuioDemoBlob> blobList = new Hashtable<Long, TuioDemoBlob>();
    private Hashtable<Long, Point> blobPointList = new Hashtable<Long, Point>();

    // The important list. Itterable Data Structure that contains the list of
    // all centeres of all the
    private Map<Long, Point> cursorPointList = Collections
            .synchronizedMap(new TreeMap<Long, Point>());

    // the temporary list that holds updates from each frame downtime
    private Map<Long, Point> tempCursorPointList = Collections
            .synchronizedMap(new TreeMap<Long, Point>());

    // width and height of the display window
    final int WIDTH = 1950;
    final int HEIGHT = 1250;

    // Thread that the entire thing will run on
    Thread myThread;

    // Debug for the Kinect
    boolean verbose = false;

    // collisions constant
    final int HORIZONTAL_COLLIDE = 1;
    final int VERTICAL_COLLIDE = 2;
    final int INIT_COLLIDE = 4;

    // initial position of mouse
    final int NOWHERE = 9999;

    // constant factors that we can adjust for better animations
    final int OUT_FACTOR = 5000;
    final int IN_FACTOR = 10;
    final int FROG_FACTOR_ONE = 3; // square root frog movement speed
    final int FROG_FACTOR_TWO = 3; // constant frog movement speed
    final int FROG_FACTOR_THREE = 5; // sensitivity for landing

    final int BOXDIM = 200;
    final int WAVEWIDTH = 200;

    // flags for getVelocity()
    final int AWAY = 1;
    final int BACK = 2;
    final int FROG = 3;
    final int NOT_JUMPING = -99;

    final int JHULOC = 153;
    final int ECELOC = 338;

    // index of mouse, non-zero indexes are for multipoints simulations
    final int MOUSE = 0;
    Point fish;

    Image offscreen;
    Image background;

    int max = -2;

    final int picHeight = 10;
    final int picWidth = 10;

    // num parameters, feel free to change it
    final int numPic = 12;
    final int numBigLotus = 684;
    final int numFrog = 0; // strictly numFrog < numBigLotus
    final int numObject = 975;
    final int numPeople = 1;
    final int numFrame = 28;
    final int numTrigger = 8;

    // radius constants
    final int radius = 200; // radius for lotus
    final int frogRadius = 300; // radius for triggering frogs to jump

    // for checking whether the cursor is in the applet
    boolean mouseInScreen = false;

    // random
    Random rnd = new Random();

    boolean[] wave;
    double[] waveCount;
    Point[] trigger;
    Color[] color;
    int[] letters = new int[6];

    int timer = 0;

    // lists
    // BufferedImage[] pics = new BufferedImage[numPic]; // the set of pics
    BufferedImage[] frames = new BufferedImage[numFrame]; // the set of frames
    Frog[] frog = new Frog[numFrog]; // the set of frogs
    Point[] list = new Point[numObject]; // the set of objects

    LinkedList<Point> position = new LinkedList<Point>();

    // speed of strawberries
    final int simulationSpeed = 10;

    public void init() {

        offscreen = createImage(WIDTH, HEIGHT);

        trigger = new Point[numTrigger];
        trigger[0] = new Point(100, 400);
        trigger[1] = new Point(250, 100);
        trigger[2] = new Point(1100, 200);
        trigger[3] = new Point(1600, 300);
        trigger[4] = new Point(1400, 50);
        trigger[5] = new Point(1200, 600);
        trigger[6] = new Point(450, 650);
        trigger[7] = new Point(700, 200);
        this.wave = new boolean[numTrigger];
        this.waveCount = new double[numTrigger];
        for (int i = 0; i < numTrigger; i++) {
            this.wave[i] = false;
            this.waveCount[i] = 0;
        }
        this.color = new Color[numTrigger];
        color[0] = new Color(255, 0, 0);
        color[1] = new Color(255, 127, 0);
        color[2] = new Color(255, 255, 0);
        color[3] = new Color(0, 255, 0);
        color[4] = new Color(0, 0, 255);
        color[5] = new Color(75, 0, 130);
        color[6] = new Color(238, 130, 238);
        color[7] = new Color(0, 255, 255);

        int count = 0;
        for (int i = 0; i < (WIDTH / 50); i++) {
            for (int j = 0; j < (HEIGHT / 50); j++) {
                list[i * (HEIGHT / 50) + j] = new Point(i * 50, j * 50);
                list[i * (HEIGHT / 50) + j].letter = (char) rnd.nextInt(26);
                list[i * (HEIGHT / 50) + j].letter += 'A';
            }
        }

        list[JHULOC].letter = 'J';
        list[JHULOC + (HEIGHT / 50)].letter = 'H';
        list[JHULOC + (HEIGHT / 50) * 2].letter = 'U';
        list[ECELOC].letter = 'E';
        list[ECELOC + (HEIGHT / 50)].letter = 'C';
        list[ECELOC + (HEIGHT / 50) * 2].letter = 'E';

        letters[0] = JHULOC;
        letters[1] = JHULOC + (HEIGHT / 50);
        letters[2] = JHULOC + (HEIGHT / 50) * 2;
        letters[3] = ECELOC;
        letters[4] = ECELOC + (HEIGHT / 50);
        letters[5] = ECELOC + (HEIGHT / 50) * 2;

        Thread myThread = new Thread(this);
        myThread.start();
    }

    public void run() {

        while (true) {
            if (offscreen == null) {
                offscreen = createImage(WIDTH, HEIGHT);
            }
            Graphics2D g = (Graphics2D) offscreen.getGraphics();
            // g.drawImage(background, 0, 0, this);
            g.setColor(Color.black);
            g.fillRect(0, 0, WIDTH, HEIGHT);
            // g.drawImage(this.frames[this.timer % 28], 0, 0, this);
            for (int i = 0; i < numObject; i++) {
                this.list[i].update();
            }

            for (int i = 0; i < numObject; i++) {

                if (list[i].y >= HEIGHT - 10) {
                    list[i].y = HEIGHT - 10;
                }
                if (list[i].y < 0) {
                    list[i].y = 1;
                }
                if (list[i].x >= WIDTH - 10) {
                    list[i].x = WIDTH - 10;
                }
                if (list[i].x < 0) {
                    list[i].x = 1;
                }
                boolean first = true;

                this.list[i].speedX = 0;
                this.list[i].speedY = 0;
                boolean check = true;

                for (Point p : cursorPointList.values()) {

                    double distance = this.getDistance(this.list[i], p);

                    if (distance < this.radius - this.radius / 10) {
                        check = false;
                        Point v = this.getVelocity(this.list[i], p, AWAY);

                        this.list[i].speedX += v.x;
                        this.list[i].speedY += v.y;

                    } else if (distance < this.radius) {
                        check = false;
                        this.list[i].speedX += 0;
                        this.list[i].speedY += 0;
                    } else {
                        double x = (this.list[i].initX - this.list[i].x);
                        double y = (this.list[i].initY - this.list[i].y);
                        if (x != 0 && y != 0
                                && /* p == numPeople - 1 && */check) {
                            Point init = new Point(this.list[i].initX,
                                    this.list[i].initY);
                            Point v = this
                                    .getVelocity(init, this.list[i], BACK);
                            this.list[i].speedX += v.x;
                            this.list[i].speedY += v.y;
                        } else {
                            this.list[i].speedX += 0;
                            this.list[i].speedY += 0;
                        }
                    }
                }
                int alpha = 0;

                int red = 255;
                int green = 255;
                int blue = 255;

                Point closestPoint = findClosestCursor(list[i]);

                if (closestPoint == null) {
                    alpha = 0;
                } else {
                    alpha = (int) (255.0 * (1.0 / this.getDistance(
                            this.list[i], closestPoint) * 150));
                }
                if (alpha > 255 || alpha < 0) {
                    alpha = 0;
                }
                boolean special = false;
                int triggerCount = 1;
                for (int j = 0; j < numTrigger; j++) {
                    if (wave[j]) {
                        waveCount[j] += 0.05;
                        Point t = new Point(trigger[j].x + BOXDIM / 2,
                                trigger[j].y + BOXDIM / 2);
                        if (waveCount[j] > 1000) {
                            waveCount[j] = 0;
                            wave[j] = false;
                        } else if (this.getDistance(list[i], t) > (waveCount[j])
                                && this.getDistance(this.list[i], t) < (waveCount[j] + WAVEWIDTH)) {
                            Color c = this.color[j];
                            red += c.getRed();
                            green += c.getGreen();
                            blue += c.getBlue();
                            triggerCount++;
                            for (int s = 0; s < letters.length; s++) {
                                if (i == letters[s]) {
                                    special = true;
                                }
                            }
                        }
                    } else {
                        for (Point p : cursorPointList.values()) {
                            if (!wave[j]
                                    && this.inBox(p, this.trigger[j], BOXDIM,
                                            BOXDIM)) {
                                wave[j] = true;
                            }
                        }
                    }
                }
                boolean checkT = false;
                if (triggerCount > 1) {
                    triggerCount--;
                    red -= 255;
                    green -= 255;
                    blue -= 255;
                    checkT = true;
                }
                red /= triggerCount;
                green /= triggerCount;
                blue /= triggerCount;
                if (alpha > max) {
                    max = alpha;
                }
                if (checkT) {
                    Color c = new Color(red, green, blue, 255);
                    g.setColor(c);
                } else {
                    Color c = new Color(red, green, blue, alpha);
                    g.setColor(c);
                }
                // g.drawImage(list[i].img, (int) list[i].x, (int) list[i].y,
                // this);
                Point draw = list[i].getDrawPlace();
                // g.fillRect((int) draw.x, (int) draw.y, 10, 10);
                // c = new Color((int) (red * 0.50), (int) (green * 0.50), (int)
                // (blue * 0.50), 255);
                // g.setColor(c);
                g.setStroke(new BasicStroke(2));
                if (special) {
                    g.setColor(Color.white);
                    g.setFont(new Font("TimesRoman", Font.PLAIN, 40));
                    // Color c2 = new Color(red / 2, green / 2, blue / 2, 255);
                    // g.setColor(c2);
                    g.drawString(list[i].letter + "", (int) draw.x + 2,
                            (int) draw.y - 2);
                    g.setColor(new Color(255, 255, 255, 123));
                    g.setFont(new Font("TimesRoman", Font.PLAIN, 46));
                    // g.setColor(c);
                } else {
                    g.setFont(new Font("TimesRoman", Font.PLAIN, 20));
                }
                g.drawString(list[i].letter + "", (int) draw.x, (int) draw.y);
                // g.drawRect((int) draw.x, (int) draw.y, 10, 10);
            }
            for (int i = 0; i < numTrigger; i++) {
                for (int j = 0; j < 10; j++) {
                    Color c = new Color(color[i].getRed(), color[i].getGreen(),
                            color[i].getBlue(), 10);
                    g.setColor(c);
                    g.fillOval((int) this.trigger[i].x + j * 10,
                            (int) this.trigger[i].y + j * 10, BOXDIM - j * 10
                                    * 2, BOXDIM - j * 10 * 2);
                }
            }

            for (Point p : cursorPointList.values()) {
                g.drawImage(p.img, (int) p.x, (int) p.y, this);
            }
            if (position.size() > 30) {
                position.remove();
            }
            // TODO FIX THIS
            /*
             * Point last = (Point) cursorPointList.values().toArray()[0];
             * position.add(last); for (int i = 1; i < position.size(); i++) {
             * Color wakeC = new Color(247, 255, 124, i * 6); g.setColor(wakeC);
             * if (!(position.get(i - 1).x == position.get(i).x && position
             * .get(i - 1).y == position.get(i).y)) { // g.drawOval((int)
             * this.position.get(i).x - // (this.position.size() - i), (int)
             * this.position.get(i).y // - (this.position.size() - i), 70 - i *
             * 2, 70 - i * 2); g.drawOval( (int) this.position.get(i).x -
             * (this.position.size() - i), (int) this.position.get(i).y -
             * (this.position.size() - i), (900 - (int) Math.pow(i, 2)) / 20,
             * (900 - (int) Math.pow(i, 2)) / 20);
             * 
             * } }
             * 
             * System.out.println(position.size());
             */
            this.timer++;
            repaint();
            cursorPointList = new TreeMap<Long, Point>(tempCursorPointList);
            tempCursorPointList.clear();
            delay(50);
        }
    }

    // Callback for new TuioObject
    public void addTuioObject(TuioObject tobj) {
        TuioDemoObject demo = new TuioDemoObject(tobj);
        objectList.put(tobj.getSessionID(), demo);
        objectPointList.put(tobj.getSessionID(), new Point(tobj.getX() * WIDTH,
                tobj.getY() * HEIGHT, null));

        if (verbose)
            System.out.println("add obj " + tobj.getSymbolID() + " ("
                    + tobj.getSessionID() + ") " + tobj.getX() + " "
                    + tobj.getY() + " " + tobj.getAngle());
    }

    // Callback for movement of a TuioObject
    public void updateTuioObject(TuioObject tobj) {

        TuioDemoObject demo = (TuioDemoObject) objectList.get(tobj
                .getSessionID());
        demo.update(tobj);
        objectPointList.put(tobj.getSessionID(), new Point(tobj.getX() * WIDTH,
                tobj.getY() * HEIGHT, null));

        if (verbose)
            System.out.println("set obj " + tobj.getSymbolID() + " ("
                    + tobj.getSessionID() + ") " + tobj.getX() + " "
                    + tobj.getY() + " " + tobj.getAngle() + " "
                    + tobj.getMotionSpeed() + " " + tobj.getRotationSpeed()
                    + " " + tobj.getMotionAccel() + " "
                    + tobj.getRotationAccel());
    }

    // Calback for removal of a TuioObject
    public void removeTuioObject(TuioObject tobj) {
        objectList.remove(tobj.getSessionID());
        objectPointList.remove(tobj.getSessionID());

        if (verbose)
            System.out.println("del obj " + tobj.getSymbolID() + " ("
                    + tobj.getSessionID() + ")");
    }

    // Callback for new TuioCursor
    // THIS IS THE ONE THAT MATTERS!!!!
    public void addTuioCursor(TuioCursor tcur) {

        if (!cursorList.containsKey(tcur.getSessionID())) {
            cursorList.put(tcur.getSessionID(), tcur);
            tempCursorPointList.put(tcur.getSessionID(),
                    new Point((1 - tcur.getX()) * WIDTH, tcur.getY() * HEIGHT,
                            null));
        }

        if (verbose)
            System.out.println("add cur " + tcur.getCursorID() + " ("
                    + tcur.getSessionID() + ") " + tcur.getX() + " "
                    + tcur.getY());
    }

    // Callback for movement of a TuioCursor
    // THIS IS THE ONE THAT MATTERS!!!!
    public void updateTuioCursor(TuioCursor tcur) {

        tempCursorPointList.put(tcur.getSessionID(), new Point(
                (1 - tcur.getX()) * WIDTH, tcur.getY() * HEIGHT, null));

        if (verbose)
            System.out.println("set cur " + tcur.getCursorID() + " ("
                    + tcur.getSessionID() + ") " + tcur.getX() + " "
                    + tcur.getY() + " " + tcur.getMotionSpeed() + " "
                    + tcur.getMotionAccel());
    }

    // Callback for movement of a TuioCursor
    // THIS IS THE ONE THAT MATTERS!!!!
    public void removeTuioCursor(TuioCursor tcur) {

        cursorList.remove(tcur.getSessionID());
        // cursorPointList.remove(tcur.getSessionID());

        if (verbose)
            System.out.println("del cur " + tcur.getCursorID() + " ("
                    + tcur.getSessionID() + ")");
    }

    // Callback for new TuioBlob
    public void addTuioBlob(TuioBlob tblb) {
        TuioDemoBlob demo = new TuioDemoBlob(tblb);
        blobList.put(tblb.getSessionID(), demo);
        blobPointList.put(tblb.getSessionID(), new Point((1 - tblb.getX())
                * WIDTH, tblb.getY() * HEIGHT, null));

        if (verbose)
            System.out.println("add blb " + tblb.getBlobID() + " ("
                    + tblb.getSessionID() + ") " + tblb.getX() + " "
                    + tblb.getY() + " " + tblb.getAngle());
    }

    // Callback for the movement of the TuioBlob
    public void updateTuioBlob(TuioBlob tblb) {

        TuioDemoBlob demo = (TuioDemoBlob) blobList.get(tblb.getSessionID());
        demo.update(tblb);
        blobPointList.put(tblb.getSessionID(), new Point((1 - tblb.getX())
                * WIDTH, tblb.getY() * HEIGHT, null));

        if (verbose)
            System.out.println("set blb " + tblb.getBlobID() + " ("
                    + tblb.getSessionID() + ") " + tblb.getX() + " "
                    + tblb.getY() + " " + tblb.getAngle() + " "
                    + tblb.getMotionSpeed() + " " + tblb.getRotationSpeed()
                    + " " + tblb.getMotionAccel() + " "
                    + tblb.getRotationAccel());
    }

    // Callback for the removal of a TuioBlob
    public void removeTuioBlob(TuioBlob tblb) {
        blobList.remove(tblb.getSessionID());
        blobPointList.remove(tblb.getSessionID());

        if (verbose)
            System.out.println("del blb " + tblb.getBlobID() + " ("
                    + tblb.getSessionID() + ")");
    }

    // Async refresh call from tuio. We do nothing on this event
    public void refresh(TuioTime frameTime) {
    }

    // Actually draws the image
    public void paint(Graphics g) {
        g.drawImage(offscreen, 0, 0, this);
    }

    // Callback for the system. Magic just happens here.
    public void update(Graphics g) {
        paint(g);
    }

    // Wrapper for sleep
    public void delay(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Private classes for the animation
    // I'm not particularly attaached to this. You can change the way points
    // work if you really want to
    private class Point { // the object class, also sometimes used to pass x y
                          // corrdinates
        public double x, y, directionX, directionY, speedX, speedY, initX,
                initY, degrees;
        public double prevX, prevY; // x y positions of the point in the
                                    // previous frame
        public Image img;

        public char letter;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
            this.prevX = this.x;
            this.prevY = this.y;
            this.directionX = 1;
            this.directionY = 1;
            this.speedX = 0;
            this.speedY = 0;
            this.initX = x;
            this.initY = y;
            this.degrees = rnd.nextInt(360);
        }

        public Point(double x, double y, Image i) {
            this.x = x;
            this.y = y;
            this.prevX = this.x;
            this.prevY = this.y;
            this.directionX = 1;
            this.directionY = 1;
            this.speedX = 0;
            this.speedY = 0;
            this.initX = x;
            this.degrees = rnd.nextInt(360);
            this.img = i;
        }

        public void update() {
            this.prevX = this.x;
            this.prevY = this.y;
            this.x = this.x + speedX * directionX;
            this.y = this.y + speedY * directionY;
        }

        public void setImage(Image i) {
            this.img = i;
        }

        public void changeDirectionX() {
            this.directionX *= -1;
        }

        public void changeDirectionY() {
            this.directionY *= -1;
        }

        public Point getLastPoint() {
            return new Point(this.prevX, this.prevY);
        }

        public Point getDrawPlace() {
            double tempX = this.x - picWidth / 2.0;
            double tempY = this.y - picHeight / 2.0;
            return new Point(tempX, tempY);
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }

    /**
     * Converts an Image to a BufferedImage
     * 
     * @param img
     *            the image to be converted
     * @return the BufferedImage instance of img
     */
    public BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        BufferedImage bimage = new BufferedImage(img.getWidth(null),
                img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();
        return bimage;
    }

    /**
     * Gets distance between two points
     * 
     * @param a
     *            a point
     * @param b
     *            the other point
     * @return the distance between two points in double
     */
    public double getDistance(Point a, Point b) {
        return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }

    public boolean inBox(Point p, Point box, int x, int y) {
        if (p.x < box.x || p.x > box.x + x || p.y < box.y || p.y > box.y + y) {
            return false;
        }
        return true;
    }

    private class Frog extends Point {
        int home, destination;
        boolean jump, directionSet;

        public Frog(double x, double y, int home) {
            super(x, y);
            this.home = home;
            this.destination = home;
            this.jump = false;
            this.directionSet = false;
        }

        public void startJump(int destination) {
            this.jump = true;
            this.destination = destination;
        }

        public void land() {
            this.jump = false;
            this.directionSet = false;
        }
    }

    public Point getVelocity(Point to, Point from, int flag) {
        double distance = getDistance(to, from);
        double x = (to.x - from.x);
        double y = (to.y - from.y);
        double magnitude;
        if (flag == AWAY) {
            magnitude = OUT_FACTOR / distance;
        } else if (flag == BACK) {
            magnitude = distance / IN_FACTOR;
        } else {
            magnitude = Math.sqrt(distance) * FROG_FACTOR_ONE + FROG_FACTOR_TWO;
        }
        if (flag == FROG) {
            Frog f = (Frog) from;
            if (!f.directionSet) {
                f.directionSet = true;
                double tempY = -y;
                double degrees = Math.toDegrees(Math.atan(tempY / x));
                if (x >= 0 && tempY >= 0) {
                    // System.out.println("quadrant 1");
                } else if (x <= 0 && tempY >= 0) {
                    // System.out.println("before conversion " + degrees);
                    degrees = 180 + degrees;
                    // System.out.println("after convresion " + degrees);
                    // System.out.println("quadrant 2");
                } else if (x <= 0 && tempY <= 0) {
                    degrees = 180 + degrees;
                    // System.out.println("quadrant 3");
                } else if (x >= 0 && tempY <= 0) {
                    degrees = 360 + degrees;
                    // System.out.println("quadrant 4");
                }
                from.degrees = degrees;
            }
        }
        double k = Math.sqrt(Math.pow(magnitude, 2)
                / ((Math.pow(x, 2)) + (Math.pow(y, 2))));
        Point v = new Point(k * x, k * y);
        return v;
    }

    /**
     * Rotates the image of a object, by degrees given by itself
     * 
     * @param p
     *            the object to retoate
     * @return the instance of the rotated Image
     */
    public Image rotate(Point p) {
        double convert = (360 - p.degrees + 90) % 360;
        double rotationRequired = Math.toRadians(convert);
        double locationX = p.img.getWidth(this) / 2;
        double locationY = p.img.getHeight(this) / 2;
        AffineTransform tx = AffineTransform.getRotateInstance(
                rotationRequired, locationX, locationY);
        AffineTransformOp op = new AffineTransformOp(tx,
                AffineTransformOp.TYPE_BILINEAR);
        return op.filter(this.toBufferedImage(p.img), null);
    }

    public Point findClosestCursor(Point inputPoint) {
        Point toReturn = null;
        double closestDistance = Double.MAX_VALUE;
        for (Point p : cursorPointList.values()) {
            if (getDistance(inputPoint, p) < closestDistance) {
                closestDistance = getDistance(inputPoint, p);
                toReturn = p;
            }
        }
        return toReturn;
    }

}