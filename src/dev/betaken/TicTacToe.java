package dev.betaken;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.util.concurrent.ThreadLocalRandom;

public class TicTacToe extends Canvas implements Runnable {
    private final JFrame frame;

    public static final String TITLE = "Tic-Tac-Toes";
    public static final byte CELLS = 3;
    public static final int CELL_SIZE = 128;
    public static final int WH = CELL_SIZE * CELLS;

    private Thread thread;
    public volatile boolean running = false;

    public byte[][] BOARD = new byte[CELLS][CELLS];
    public boolean turn = ThreadLocalRandom.current().nextBoolean();
    public byte win;

    public TicTacToe() {
        this.frame = new JFrame(TITLE);
        initFrame();
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (win != 0) return;
                byte col = (byte) (e.getX() / CELL_SIZE);
                byte row = (byte) (e.getY() / CELL_SIZE);
                if (BOARD[col][row] != 0) return;
                BOARD[col][row] = (byte) (turn ? 2 : 1);
                turn = !turn;
                win = check();
                if (win == 0 && isDraw()) win = 3;
            }
        });

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_R) {
                    BOARD = new byte[CELLS][CELLS];
                    turn = ThreadLocalRandom.current().nextBoolean();
                    win = 0;
                }
            }
        });
    }

    private void initFrame() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        this.setPreferredSize(new Dimension(WH, WH));
        frame.add(this);

        frame.pack();
        frame.setLocationRelativeTo(null);
        this.start();
        setFocusable(true);
        frame.setVisible(true);
        requestFocusInWindow();
    }

    public synchronized void start() {
        if (running) return;

        running = true;
        this.thread = new Thread(this);
        thread.start();
    }

    public synchronized void stop() {
        if (!running) return;

        running = false;
        try {
            this.thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000D/60D;

        int ticks = 0;
        int frames = 0;

        long lastTimer = System.currentTimeMillis();
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;
            boolean shouldRender = false;

            while (delta >= 1) {
                ticks++;
                tick();

                delta -= 1;
                shouldRender = true;
            }

            try {
                Thread.sleep(2);
            } catch (InterruptedException ignored) {}

            if (shouldRender) {
                frames++;
                render();
            }

            if (System.currentTimeMillis() - lastTimer >= 1000) {
                frame.setTitle(TITLE + " | TPS: " + ticks + ", FPS: " + frames);
                lastTimer += 1000;
                ticks = 0;
                frames = 0;
            }
        }

        stop();
    }

    private void tick() {}

    public static final Color TEXT_COLOR = new Color(80, 80, 80);
    public static final Font FONT = new Font("Arial", Font.PLAIN, 40);
    private void render() {
        BufferStrategy bs = this.getBufferStrategy();
        if (bs == null) {
            this.createBufferStrategy(3);
            return;
        }

        Graphics2D g = (Graphics2D) bs.getDrawGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        int i = 0;
        for (int x = 0; x < CELLS; x++) {
            for (int y = 0; y < CELLS; y++) {
                g.setStroke(new BasicStroke(3));

                i++;
                g.setColor(i % 2 == 0 ? Color.WHITE : Color.GRAY);
                g.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                g.setColor(Color.BLACK);
                g.drawRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);

                byte b = BOARD[x][y];
                g.setStroke(new BasicStroke(6));
                if (b == 1) {
                    g.setColor(Color.RED);
                    g.drawLine(x * CELL_SIZE + 16, y * CELL_SIZE + 16,
                            x * CELL_SIZE + 112, y * CELL_SIZE + 112);
                    g.drawLine(x * CELL_SIZE + 112, y * CELL_SIZE + 16,
                            x * CELL_SIZE + 16, y * CELL_SIZE + 112);
                } else if (b == 2) {
                    g.setColor(Color.BLUE);
                    g.drawOval(x * CELL_SIZE + 16, y * CELL_SIZE + 16,
                            CELL_SIZE - 32, CELL_SIZE - 32);
                }
            }
        }

        g.setFont(FONT);
        String text = (turn ? "BLUE" : "RED") + "'S TURN";
        if (win != 0) {
            if (win == 3) text = "DRAW";
            else text = (win == 2 ? "BLUE" : "RED") + " WINS";
        }

        FontMetrics fm = g.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(text)) / 2;
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

        g.setColor(TEXT_COLOR);
        g.drawString(text, x, y);
        g.dispose();
        bs.show();
    }

    private byte check() {
        // Horizontal
        for (int x = 0; x < CELLS; x++) {
            byte first = BOARD[x][0];
            if (first == 0) continue;

            boolean win = true;
            for (int y = 1; y < CELLS; y++) {
                if (first != BOARD[x][y]) {
                    win = false;
                    break;
                }
            }

            if (win) return first;
        }

        // Vertical
        for (int y = 0; y < CELLS; y++) {
            byte first = BOARD[0][y];
            if (first == 0) continue;

            boolean win = true;
            for (int x = 1; x < CELLS; x++) {
                if (first != BOARD[x][y]) {
                    win = false;
                    break;
                }
            }

            if (win) return first;
        }

        // Diagonal 1
        if (BOARD[0][0] != 0 &&
                BOARD[0][0] == BOARD[1][1] &&
                BOARD[1][1] == BOARD[2][2]) {

            return BOARD[0][0];
        }

        // Diagonal 2
        if (BOARD[0][2] != 0 &&
                BOARD[0][2] == BOARD[1][1] &&
                BOARD[1][1] == BOARD[2][0]) {

            return BOARD[0][2];
        }

        return 0;
    }

    private boolean isDraw() {
        for (int x = 0; x < CELLS; x++) {
            for (int y = 0; y < CELLS; y++) {
                if (BOARD[x][y] == 0) return false;
            }
        }

        return true;
    }
}