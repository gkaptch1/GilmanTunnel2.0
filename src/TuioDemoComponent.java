/*
 TUIO Java GUI Demo
 Copyright (c) 2005-2014 Martin Kaltenbrunner <martin@tuio.org>
 
 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files
 (the "Software"), to deal in the Software without restriction,
 including without limitation the rights to use, copy, modify, merge,
 publish, distribute, sublicense, and/or sell copies of the Software,
 and to permit persons to whom the Software is furnished to do so,
 subject to the following conditions:
 
 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.
 
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import TUIO.TuioBlob;
import TUIO.TuioCursor;
import TUIO.TuioListener;
import TUIO.TuioObject;
import TUIO.TuioTime;

public class TuioDemoComponent extends JComponent implements TuioListener,
        Runnable {

    // Private vars from the TUIO interface

    /**
     * Default UID
     */
    private static final long serialVersionUID = 1L;
    // private Hashtable<Long, TuioDemoObject> objectList = new Hashtable<Long,
    // TuioDemoObject>();
    // private Hashtable<Long, Point> objectPointList = new Hashtable<Long,
    // Point>();
    private Hashtable<Long, TuioCursor> cursorList = new Hashtable<Long, TuioCursor>();
    private Map<Long, Point> cursorPointList = new TreeMap<Long, Point>();
    private Map<Long, Point> tempCursorPointList = new TreeMap<Long, Point>();
    // private Hashtable<Long, TuioDemoBlob> blobList = new Hashtable<Long,
    // TuioDemoBlob>();
    // private Hashtable<Long, Point> blobPointList = new Hashtable<Long,
    // Point>();

    public static final int finger_size = 15;
    public static final int object_size = 60;
    public static final int table_size = 760;

    public static int width, height;
    public boolean verbose = false;

    // Private vars for the frog animation
    // collisions constant
    final int HORIZONTAL_COLLIDE = 1;
    final int VERTICAL_COLLIDE = 2;
    final int INIT_COLLIDE = 4;

    // constant factors that we can adjust for better animations
    final int OUT_FACTOR = 1000;
    final int IN_FACTOR = 15;
    final int FROG_FACTOR_ONE = 3; // square root frog movement speed
    final int FROG_FACTOR_TWO = 3; // constant frog movement speed
    final int FROG_FACTOR_THREE = 5; // sensitivity for landing

    // flags for getVelocity()
    final int AWAY = 1;
    final int BACK = 2;
    final int FROG = 3;
    final int NOT_JUMPING = -99;

    // index of mouse, non-zero indexes are for multipoints simulations
    final int MOUSE = 0;

    Image offscreen;
    Image background;

    // num parameters, feel free to change it
    final int numPic = 12;
    final int numBigLotus = 15;
    final int numFrog = 3; // strictly numFrog < numBigLotus
    final int numObject = 130;
    final int numPeople = 3;

    // radius constants
    final int radius = 500; // radius for lotus
    final int frogRadius = 300; // radius for triggering frogs to jump

    // for checking whether the cursor is in the applet
    boolean mouseInScreen = false;

    // width and height of the window
    final int WIDTH = 1960;
    final int HEIGHT = 1250;

    // random
    Random rnd = new Random();

    // lists
    BufferedImage[] pics = new BufferedImage[numPic]; // the set of pics
    Frog[] frog = new Frog[numFrog]; // the set of frogs
    Point[] list = new Point[numObject]; // the set of objects

    // speed of strawberries
    final int simulationSpeed = 10;

    Thread myThread;

    public void init() {

        offscreen = createImage(WIDTH, HEIGHT);

        try {
            pics[0] = ImageIO
                    .read(new File(
                            "/Users/Gabe/Documents/workspace/RealGilamTunnel/strawberry.gif"));
            pics[1] = ImageIO
                    .read(new File(
                            "/Users/Gabe/Documents/workspace/RealGilamTunnel/apple.jpg"));
            pics[2] = ImageIO
                    .read(new File(
                            "/Users/Gabe/Documents/workspace/RealGilamTunnel/banana.jpg"));
            pics[3] = ImageIO
                    .read(new File(
                            "/Users/Gabe/Documents/workspace/RealGilamTunnel/watermelon.jpg"));
            pics[4] = ImageIO
                    .read(new File(
                            "/Users/Gabe/Documents/workspace/RealGilamTunnel/orange.jpg"));
            pics[5] = ImageIO
                    .read(new File(
                            "/Users/Gabe/Documents/workspace/RealGilamTunnel/warning.jpg"));
            pics[6] = ImageIO
                    .read(new File(
                            "/Users/Gabe/Documents/workspace/RealGilamTunnel/good.jpg"));
            pics[7] = ImageIO
                    .read(new File(
                            "/Users/Gabe/Documents/workspace/RealGilamTunnel/lotus1.gif"));
            pics[8] = ImageIO
                    .read(new File(
                            "/Users/Gabe/Documents/workspace/RealGilamTunnel/lotus2.jpg"));
            pics[9] = ImageIO
                    .read(new File(
                            "/Users/Gabe/Documents/workspace/RealGilamTunnel/flower.jpg"));
            pics[10] = ImageIO
                    .read(new File(
                            "/Users/Gabe/Documents/workspace/RealGilamTunnel/frog.gif"));
            pics[11] = ImageIO
                    .read(new File(
                            "/Users/Gabe/Documents/workspace/RealGilamTunnel/fish.jpg"));
            background = ImageIO
                    .read(new File(
                            "/Users/Gabe/Documents/workspace/RealGilamTunnel/background.jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < numObject; i++) {
            int picWidth = rnd.nextInt(40) + 60;
            if (i < numBigLotus) {
                picWidth = 150;
            }
            int picHeight = picWidth;
            int a = rnd.nextInt(WIDTH - picWidth);
            int b = rnd.nextInt(HEIGHT - picHeight);
            boolean repeat = false;
            Point newPoint = new Point(a, b);
            newPoint.setImage(pics[7].getScaledInstance(picWidth, picHeight, 0));
            for (int j = 0; j < i; j++) {
                if (checkCollide(list[j], newPoint) == 4) {
                    repeat = true;
                    break;
                }
            }
            if (!repeat) {
                this.list[i] = newPoint;
                if (i < this.numFrog) {
                    this.frog[i] = new Frog(0, 0, i);
                    this.frog[i].setImage(pics[10]);
                }
            } else {
                i--;
            }
        }

        myThread = new Thread(this);
        myThread.start();
    }

    public int checkCollide(Point p1, Point p2) {
        int rt = 0;
        if (p1 == null || p2 == null || p1.img == null || p2.img == null) {
            return 0;
        }
        int w1 = p1.img.getWidth(this);
        int h1 = p1.img.getHeight(this);
        int w2 = p2.img.getWidth(this);
        int h2 = p2.img.getHeight(this);
        Point prev1 = p1.getLastPoint();
        Point prev2 = p2.getLastPoint();
        if (p1.x <= p2.x + w2 && p1.x + w1 >= p2.x && p1.y <= p2.y + h2
                && p1.y + h1 >= p2.y) {
            if (prev1.x > prev2.x + w2 || prev1.x + w1 < prev2.x) {
                rt += HORIZONTAL_COLLIDE;
            }
            if (prev1.y > prev2.y + h2 || prev1.y + h1 < prev2.y) {
                rt += VERTICAL_COLLIDE;
            }
            if (rt == 0) {
                return INIT_COLLIDE;
            }
        }
        return rt;
    }

    public void setSize(int w, int h) {
        super.setSize(w, h);
        width = w;
        height = h;
    }

    public void addTuioObject(TuioObject tobj) {
        // TuioDemoObject demo = new TuioDemoObject(tobj);
        // objectList.put(tobj.getSessionID(), demo);
        // objectPointList.put(tobj.getSessionID(), new Point(tobj.getX() *
        // WIDTH,
        // tobj.getY() * HEIGHT, pics[1]));

        if (verbose)
            System.out.println("add obj " + tobj.getSymbolID() + " ("
                    + tobj.getSessionID() + ") " + tobj.getX() + " "
                    + tobj.getY() + " " + tobj.getAngle());
    }

    public void updateTuioObject(TuioObject tobj) {

        // TuioDemoObject demo = (TuioDemoObject) objectList.get(tobj
        // .getSessionID());
        // demo.update(tobj);
        // objectPointList.put(tobj.getSessionID(), new Point(tobj.getX() *
        // WIDTH,
        // tobj.getY() * HEIGHT, pics[1]));

        if (verbose)
            System.out.println("set obj " + tobj.getSymbolID() + " ("
                    + tobj.getSessionID() + ") " + tobj.getX() + " "
                    + tobj.getY() + " " + tobj.getAngle() + " "
                    + tobj.getMotionSpeed() + " " + tobj.getRotationSpeed()
                    + " " + tobj.getMotionAccel() + " "
                    + tobj.getRotationAccel());
    }

    public void removeTuioObject(TuioObject tobj) {
        // objectList.remove(tobj.getSessionID());
        // objectPointList.remove(tobj.getSessionID());

        if (verbose)
            System.out.println("del obj " + tobj.getSymbolID() + " ("
                    + tobj.getSessionID() + ")");
    }

    public void addTuioCursor(TuioCursor tcur) {

        if (!cursorList.containsKey(tcur.getSessionID())) {
            cursorList.put(tcur.getSessionID(), tcur);
            tempCursorPointList.put(tcur.getSessionID(),
                    new Point((1 - tcur.getX()) * WIDTH, tcur.getY() * HEIGHT,
                            pics[1]));
            // repaint();
        }

        if (verbose)
            System.out.println("add cur " + tcur.getCursorID() + " ("
                    + tcur.getSessionID() + ") " + tcur.getX() + " "
                    + tcur.getY());
    }

    public void updateTuioCursor(TuioCursor tcur) {

        tempCursorPointList.put(tcur.getSessionID(), new Point(
                (1 - tcur.getX()) * WIDTH, tcur.getY() * HEIGHT, pics[1]));
        // repaint();

        if (verbose)
            System.out.println("set cur " + tcur.getCursorID() + " ("
                    + tcur.getSessionID() + ") " + tcur.getX() + " "
                    + tcur.getY() + " " + tcur.getMotionSpeed() + " "
                    + tcur.getMotionAccel());
    }

    public void removeTuioCursor(TuioCursor tcur) {

        cursorList.remove(tcur.getSessionID());
        // deleteCursorPointList.remove(tcur.getSessionID());
        // repaint();

        if (verbose)
            System.out.println("del cur " + tcur.getCursorID() + " ("
                    + tcur.getSessionID() + ")");
    }

    public void addTuioBlob(TuioBlob tblb) {
        // TuioDemoBlob demo = new TuioDemoBlob(tblb);
        // blobList.put(tblb.getSessionID(), demo);
        // blobPointList.put(tblb.getSessionID(), new Point((1 - tblb.getX())
        // * WIDTH, tblb.getY() * HEIGHT, pics[1]));

        if (verbose)
            System.out.println("add blb " + tblb.getBlobID() + " ("
                    + tblb.getSessionID() + ") " + tblb.getX() + " "
                    + tblb.getY() + " " + tblb.getAngle());
    }

    public void updateTuioBlob(TuioBlob tblb) {

        // TuioDemoBlob demo = (TuioDemoBlob) blobList.get(tblb.getSessionID());
        // demo.update(tblb);
        // blobPointList.put(tblb.getSessionID(), new Point((1 - tblb.getX())
        // * WIDTH, tblb.getY() * HEIGHT, pics[1]));

        if (verbose)
            System.out.println("set blb " + tblb.getBlobID() + " ("
                    + tblb.getSessionID() + ") " + tblb.getX() + " "
                    + tblb.getY() + " " + tblb.getAngle() + " "
                    + tblb.getMotionSpeed() + " " + tblb.getRotationSpeed()
                    + " " + tblb.getMotionAccel() + " "
                    + tblb.getRotationAccel());
    }

    public void removeTuioBlob(TuioBlob tblb) {
        // blobList.remove(tblb.getSessionID());
        // blobPointList.remove(tblb.getSessionID());

        if (verbose)
            System.out.println("del blb " + tblb.getBlobID() + " ("
                    + tblb.getSessionID() + ")");
    }

    public void refresh(TuioTime frameTime) {
        // repaint();
    }

    public void paint(Graphics g) {
        g.drawImage(offscreen, 0, 0, this);
    }

    public void update(Graphics g) {
        paint(g);

        /*
         * 
         * Graphics2D g2 = (Graphics2D)g;
         * g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
         * RenderingHints.VALUE_ANTIALIAS_ON);
         * g2.setRenderingHint(RenderingHints.KEY_RENDERING,
         * RenderingHints.VALUE_RENDER_QUALITY);
         * 
         * g2.setColor(Color.white); g2.fillRect(0,0,width,height);
         * 
         * int w = (int)Math.round(width-scale*finger_size/2.0f); int h =
         * (int)Math.round(height-scale*finger_size/2.0f);
         * 
         * Enumeration<TuioCursor> cursors = cursorList.elements(); while
         * (cursors.hasMoreElements()) { TuioCursor tcur =
         * cursors.nextElement(); if (tcur==null) continue; ArrayList<TuioPoint>
         * path = tcur.getPath(); TuioPoint current_point = path.get(0); if
         * (current_point!=null) { // draw the cursor path
         * g2.setPaint(Color.blue); for (int i=0;i<path.size();i++) { TuioPoint
         * next_point = path.get(i); g2.drawLine(current_point.getScreenX(w),
         * current_point.getScreenY(h), next_point.getScreenX(w),
         * next_point.getScreenY(h)); current_point = next_point; } }
         * 
         * // draw the finger tip g2.setPaint(Color.lightGray); int s =
         * (int)(scale*finger_size);
         * g2.fillOval(current_point.getScreenX(w-s/2),
         * current_point.getScreenY(h-s/2),s,s); g2.setPaint(Color.black);
         * g2.drawString
         * (tcur.getCursorID()+"",current_point.getScreenX(w),current_point
         * .getScreenY(h)); }
         * 
         * // draw the objects Enumeration<TuioDemoObject> objects =
         * objectList.elements(); while (objects.hasMoreElements()) {
         * TuioDemoObject tobj = objects.nextElement(); if (tobj!=null)
         * tobj.paint(g2, width,height); }
         */
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

    /**
     * Gets the velocity of a point to another point
     * 
     * @param
     * @param
     * @param flag
     *            1 is when the point(object) is moving away from the mouse 2 is
     *            when the point(object) is moving back to its original position
     *            3 is when the a frog is involved
     * @param the
     *            velocity in the form of a Point
     */
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

    // Private classes for the animation
    private class Point { // the object class, also sometimes used to pass x y
                          // corrdinates
        public double x, y, directionX, directionY, speedX, speedY, initX,
                initY, degrees;
        public double prevX, prevY; // x y positions of the point in the
                                    // previous frame
        public Image img;

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

        public Point getLastPoint() {
            return new Point(this.prevX, this.prevY);
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public Point getRelativeCenter(Point p) {
            double tempX = this.x
                    + (this.img.getWidth(TuioDemoComponent.this) - p.img
                            .getWidth(TuioDemoComponent.this)) / 2;
            double tempY = this.y
                    + (this.img.getHeight(TuioDemoComponent.this) - p.img
                            .getHeight(TuioDemoComponent.this)) / 2;
            return new Point(tempX, tempY);
        }
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

    public void run() {

        while (true) {
            if (offscreen == null) {
                offscreen = createImage(WIDTH, HEIGHT);
            }

            Graphics2D g = (Graphics2D) offscreen.getGraphics();
            g.setColor(Color.cyan);
            g.fillRect(0, 0, WIDTH, HEIGHT);

            for (int i = 0; i < numObject; i++) {
                if (list[i] != null)
                    list[i].update();
            }

            for (Point p : cursorPointList.values()) {
                g.drawImage(pics[4], (int) p.getX(), (int) p.getY(), this);
            }

            // Checking for collisions??
            for (int i = 0; i < numObject; i++) {
                ArrayList<Integer> collide = new ArrayList<Integer>();
                for (int b = 0; b < numObject; b++) {
                    if (i != b && checkCollide(list[i], list[b]) != 0) {
                        collide.add(checkCollide(list[i], list[b]));
                    }
                }
                if (list[i].y >= HEIGHT - list[i].img.getHeight(this)) {
                    list[i].y = HEIGHT - list[i].img.getHeight(this);
                }
                if (list[i].y <= 0) {
                    list[i].y = 0;
                }
                if (list[i].x >= WIDTH - list[i].img.getWidth(this)) {
                    list[i].x = WIDTH - list[i].img.getWidth(this);
                }
                if (list[i].x <= 0) {
                    list[i].x = 0;
                }

                list[i].speedX = 0;
                list[i].speedY = 0;
                boolean check = true;

                for (Point p : cursorPointList.values()) {

                    double distance = getDistance(list[i], p);

                    if (distance < radius - radius / 10) {
                        check = false;
                        Point v = this.getVelocity(list[i], p, AWAY);

                        list[i].speedX += v.x;
                        list[i].speedY += v.y;

                    } else if (distance < radius) {
                        check = false;
                        list[i].speedX += 0;
                        list[i].speedY += 0;
                    } else {
                        double x = (list[i].initX - list[i].x);
                        double y = (list[i].initY - list[i].y);
                        if (x != 0 && y != 0 /* && p == numPeople - 1 */
                                && check) {
                            Point init = new Point(list[i].initX, list[i].initY);
                            Point v = this.getVelocity(init, list[i], BACK);
                            list[i].speedX += v.x;
                            list[i].speedY += v.y;
                        } else {
                            list[i].speedX += 0;
                            list[i].speedY += 0;
                        }
                    }
                }

                g.drawImage(list[i].img, (int) list[i].x, (int) list[i].y, this);
            }

            for (int i = 0; i < numFrog; i++) {
                if (this.frog[i] == null) {
                    continue;
                }

                this.frog[i].update();
                if (!this.frog[i].jump) {
                    Point c = this.list[this.frog[i].destination]
                            .getRelativeCenter((Point) this.frog[i]);
                    this.frog[i].x = c.x;
                    this.frog[i].y = c.y;
                } else {
                    Point v = this.getVelocity(
                            this.list[this.frog[i].destination]
                                    .getRelativeCenter(this.frog[i]),
                            this.frog[i], FROG);
                    this.frog[i].speedX = v.x;
                    this.frog[i].speedY = v.y;
                    double distance = this.getDistance(
                            this.list[this.frog[i].destination]
                                    .getRelativeCenter(this.frog[i]),
                            this.frog[i]);
                    if (distance <= FROG_FACTOR_THREE) {
                        this.frog[i].land();
                    }
                }
                for (Point p : cursorPointList.values()) {
                    double distance = this.getDistance(p, this.frog[i]);
                    if (!this.frog[i].jump && distance < this.frogRadius) {
                        int destination;
                        boolean taken = false;
                        int prev = this.frog[i].destination;
                        do {
                            taken = false;
                            destination = rnd.nextInt(numBigLotus);
                            for (int j = 0; j < numFrog; j++) {
                                if (j != i
                                        && this.frog[j].destination == destination) {
                                    taken = true;
                                    break;
                                }
                            }
                        } while (destination == prev || taken);
                        frog[i].startJump(destination);
                    }
                }
                g.drawImage(this.rotate(this.frog[i]), (int) this.frog[i].x,
                        (int) this.frog[i].y, this);
            }
            repaint();
            cursorPointList = new TreeMap<Long, Point>(tempCursorPointList);
            tempCursorPointList.clear();
            delay(50);
        }
    }

    /**
     * Delay the thread.
     * 
     * @param time
     *            the time to delay the thread, in milliseconds
     */
    public void delay(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
