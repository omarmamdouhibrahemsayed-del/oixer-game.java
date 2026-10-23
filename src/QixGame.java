import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class QixGame extends JFrame {
    public QixGame() {
        setTitle("Qix 3D Style Game - Java Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        GamePanel panel = new GamePanel();
        add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QixGame());
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private final int WIDTH = 800;
    private final int HEIGHT = 650;
    private final int GRID_SIZE = 20;
    private final int COLS = WIDTH / GRID_SIZE;
    private final int ROWS = HEIGHT / GRID_SIZE;

    private Timer timer;
    private boolean[][] claimed; // true = المنطقة المحتلة (البرتقالي)، false = منطقة الرمال

    // إحداثيات اللاعب
    private int playerCol, playerRow;
    private boolean isDrawing = false;
    private ArrayList<Point> currentPath = new ArrayList<>();

    // الكرات المتحركة (الأعداء)
    private ArrayList<Ball> balls = new ArrayList<>();

    // احصائيات اللعبة
    private int score = 7500;
    private int lives = 3;
    private double claimedPercentage = 0.0;

    // أزرار التحكم
    private boolean left, right, up, down, space;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(240, 180, 120));
        setFocusable(true);
        addKeyListener(this);

        initGame();

        timer = new Timer(16, this); // ~60 إطار في الثانية
        timer.start();
    }

    private void initGame() {
        claimed = new boolean[COLS][ROWS];
        // تهيئة الحدود الخارجية كأماكن محتلة آمنة
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                if (c < 3 || c >= COLS - 3 || r < 3 || r >= ROWS - 3) {
                    claimed[c][r] = true;
                } else {
                    claimed[c][r] = false;
                }
            }
        }

        playerCol = COLS / 2;
        playerRow = ROWS - 4;

        // إنشاء الكرات الملونه المرتدة
        balls.clear();
        Random rand = new Random();
        for (int i = 0; i < 5; i++) {
            balls.add(new Ball(rand.nextInt(WIDTH - 200) + 100, rand.nextInt(HEIGHT - 200) + 100));
        }

        updatePercentage();
    }

    private void updatePercentage() {
        int totalPlayable = 0;
        int claimedCount = 0;
        for (int c = 3; c < COLS - 3; c++) {
            for (int r = 3; r < ROWS - 3; r++) {
                totalPlayable++;
                if (claimed[c][r]) {
                    claimedCount++;
                }
            }
        }
        if (totalPlayable > 0) {
            claimedPercentage = (double) claimedCount / totalPlayable * 100.0;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int newCol = playerCol;
        int newRow = playerRow;

        if (left) newCol--;
        if (right) newCol++;
        if (up) newRow--;
        if (down) newRow++;

        if ((newCol != playerCol || newRow != playerRow) && newCol >= 0 && newCol < COLS && newRow >= 0 && newRow < ROWS) {
            playerCol = newCol;
            playerRow = newRow;

            if (space) {
                if (!claimed[playerCol][playerRow]) {
                    isDrawing = true;
                    currentPath.add(new Point(playerCol, playerRow));
                } else if (isDrawing) {
                    // إغلاق المنطقة بنجاح وحفظها
                    for (Point p : currentPath) {
                        claimed[p.x][p.y] = true;
                    }
                    fillUnclaimedArea();
                    currentPath.clear();
                    isDrawing = false;
                    score += 500;
                    updatePercentage();
                }
            } else {
                if (isDrawing && claimed[playerCol][playerRow]) {
                    // العودة لمنطقة آمنة أثناء الرسم
                    for (Point p : currentPath) {
                        claimed[p.x][p.y] = true;
                    }
                    fillUnclaimedArea();
                    currentPath.clear();
                    isDrawing = false;
                    score += 300;
                    updatePercentage();
                }
            }
        }

        // تحديث حركة الكرات واكتشاف الاصطدام بالخط المرسوم
        for (Ball ball : balls) {
            ball.update(claimed, GRID_SIZE, WIDTH, HEIGHT);

            if (isDrawing) {
                for (Point p : currentPath) {
                    if (Math.abs(ball.x - (p.x * GRID_SIZE + GRID_SIZE/2)) < GRID_SIZE &&
                            Math.abs(ball.y - (p.y * GRID_SIZE + GRID_SIZE/2)) < GRID_SIZE) {
                        // اصطدام! خسارة حياة وإعادة تعيين الخط
                        lives--;
                        currentPath.clear();
                        isDrawing = false;
                        playerCol = COLS / 2;
                        playerRow = ROWS - 4;
                        if (lives <= 0) {
                            initGame();
                            lives = 3;
                            score = 7500;
                        }
                        break;
                    }
                }
            }
        }

        repaint();
    }

    private void fillUnclaimedArea() {
        boolean[][] visited = new boolean[COLS][ROWS];
        for (Ball ball : balls) {
            int bc = (int)(ball.x / GRID_SIZE);
            int br = (int)(ball.y / GRID_SIZE);
            if (bc >= 0 && bc < COLS && br >= 0 && br < ROWS) {
                floodFill(bc, br, visited);
            }
        }

        for (int c = 3; c < COLS - 3; c++) {
            for (int r = 3; r < ROWS - 3; r++) {
                if (!claimed[c][r] && !visited[c][r]) {
                    claimed[c][r] = true;
                }
            }
        }
    }

    private void floodFill(int c, int r, boolean[][] visited) {
        if (c < 0 || c >= COLS || r < 0 || r >= ROWS) return;
        if (claimed[c][r] || visited[c][r]) return;

        visited[c][r] = true;
        floodFill(c + 1, r, visited);
        floodFill(c - 1, r, visited);
        floodFill(c, r + 1, visited);
        floodFill(c, r - 1, visited);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) left = true;
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) right = true;
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) up = true;
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) down = true;
        if (key == KeyEvent.VK_SPACE) space = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) left = false;
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) right = false;
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) up = false;
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) down = false;
        if (key == KeyEvent.VK_SPACE) space = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // خلفية الرمال
        g2d.setColor(new Color(210, 160, 110));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // رسم المناطق المحتلة (البرتقالي)
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                if (claimed[c][r]) {
                    g2d.setColor(new Color(230, 110, 20));
                    g2d.fillRect(c * GRID_SIZE, r * GRID_SIZE, GRID_SIZE, GRID_SIZE);
                    g2d.setColor(new Color(250, 140, 50));
                    g2d.drawRect(c * GRID_SIZE, r * GRID_SIZE, GRID_SIZE, GRID_SIZE);
                }
            }
        }

        // رسم الخط الأصفر أثناء الرسم
        g2d.setColor(new Color(240, 220, 50));
        for (Point p : currentPath) {
            g2d.fillRect(p.x * GRID_SIZE, p.y * GRID_SIZE, GRID_SIZE, GRID_SIZE);
            g2d.setColor(new Color(200, 180, 30));
            g2d.drawRect(p.x * GRID_SIZE, p.y * GRID_SIZE, GRID_SIZE, GRID_SIZE);
        }

        // رسم الكرات المتحركة
        for (Ball ball : balls) {
            ball.draw(g2d);
        }

        // رسم شخصية اللاعب (مروحة / مروحية صغيرة)
        int px = playerCol * GRID_SIZE;
        int py = playerRow * GRID_SIZE;
        g2d.setColor(new Color(0, 130, 150));
        g2d.fillOval(px + 2, py + 2, GRID_SIZE - 4, GRID_SIZE - 4);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(px + 2, py, GRID_SIZE - 4, 4);

        // واجهة المستخدم العلوية (Score & UI Bar)
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, WIDTH, 50);

        // النقاط
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("⭐ " + score, 20, 32);

        // شريط النسبة المئوية
        g2d.setColor(Color.WHITE);
        g2d.fillRect(250, 15, 200, 20);
        g2d.setColor(new Color(50, 200, 50));
        g2d.fillRect(250, 15, (int)(200 * (claimedPercentage / 100.0)), 20);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(250, 15, 200, 20);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString(String.format("%.1f%%", claimedPercentage), 330, 30);

        // القلوب (الأرواح)
        g2d.setColor(Color.RED);
        for (int i = 0; i < lives; i++) {
            g2d.fillOval(550 + i * 35, 18, 15, 15);
        }
    }
}

class Ball {
    double x, y;
    double vx, vy;
    Color color;

    public Ball(double x, double y) {
        this.x = x;
        this.y = y;
        Random r = new Random();
        this.vx = (r.nextDouble() - 0.5) * 6;
        this.vy = (r.nextDouble() - 0.5) * 6;
        if (Math.abs(vx) < 2) vx = 2;
        if (Math.abs(vy) < 2) vy = 2;

        Color[] colors = {Color.RED, Color.MAGENTA, Color.CYAN, Color.GREEN, Color.YELLOW};
        this.color = colors[r.nextInt(colors.length)];
    }

    public void update(boolean[][] claimed, int gridSize, int width, int height) {
        x += vx;
        y += vy;

        int cols = width / gridSize;
        int rows = height / gridSize;

        int c = (int)(x / gridSize);
        int r = (int)(y / gridSize);

        if (x <= gridSize * 3 || x >= width - gridSize * 3 || (c >= 0 && c < cols && r >= 0 && r < rows && claimed[c][r])) {
            vx = -vx;
            x += vx * 2;
        }
        if (y <= gridSize * 3 || y >= height - gridSize * 3 || (c >= 0 && c < cols && r >= 0 && r < rows && claimed[c][r])) {
            vy = -vy;
            y += vy * 2;
        }
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.fillOval((int)(x - 10), (int)(y - 10), 20, 20);
        g2d.setColor(Color.BLACK);
        g2d.drawOval((int)(x - 10), (int)(y - 10), 20, 20);
    }
}