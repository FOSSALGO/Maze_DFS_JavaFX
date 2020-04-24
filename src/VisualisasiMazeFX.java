
import java.awt.Point;
import java.util.ArrayList;
import java.util.Stack;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class VisualisasiMazeFX extends Application {

    //Atribut kelas	
    private Labirin labirin = new GeneratorMaze(5, 5).getLabirin();
    private ArrayList<History> history = null;

    private ProgressBox[][] progressBox = null;
    private Point[] lintasan = null;
    double T = 2;
    int t, head = 0;
    boolean isSet = false;
    double size;
    Color color1 = Color.rgb(243, 156, 18, 0.9);
    Color color2 = Color.RED;

    @Override
    public void start(Stage stage) {
        inisialisasiUI(stage);
    }

    private void inisialisasiUI(Stage stage) {

        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, Color.WHITESMOKE);

        Canvas canvas = new Canvas(500, 500);
        root.setCenter(canvas);

        FlowPane flowPane = new FlowPane(Orientation.HORIZONTAL, 6, 2);
        flowPane.setPadding(new Insets(4));
        flowPane.setAlignment(Pos.BASELINE_CENTER);

        Label lbl1 = new Label("_Baris:");
        Label lbl2 = new Label("_Kolom:");
        TextField field1 = new TextField();
        TextField field2 = new TextField();
        field1.setText("5");
        field2.setText("5");
        field1.setPrefColumnCount(4);
        field2.setPrefColumnCount(4);
        lbl1.setLabelFor(field1);
        lbl1.setMnemonicParsing(true);
        lbl2.setLabelFor(field2);
        lbl2.setMnemonicParsing(true);

        Button tombolGenerate = new Button("_Generate");
        tombolGenerate.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int baris = Integer.parseInt(field1.getText().trim());
                int kolom = Integer.parseInt(field2.getText().trim());
                labirin = new GeneratorMaze(baris, kolom).getLabirin();
                history = null;
                progressBox = null;
                lintasan = null;
            }
        });

        Button tombolDFS = new Button("Solusi _DFS");
        tombolDFS.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (labirin != null) {
                    initializeProgressBox(labirin.getGrid().length, labirin.getGrid()[0].length);
                    isSet = false;
                    DFS dfs = new DFS(labirin);
                    history = dfs.getHistory();
                    lintasan = dfs.getLintasan();
                    t = 0;
                    head = 0;
                }
            }
        });

        flowPane.getChildren().addAll(lbl1, field1, lbl2, field2, tombolGenerate, tombolDFS);
        root.setTop(flowPane);

        new AnimationTimer() {
            @Override
            public void handle(long currentNanoTime) {
                update();
                draw(canvas);
            }
        }.start();

        stage.setTitle("LABIRIN");
        stage.setScene(scene);
        stage.show();
    }

    private void draw(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();
        gc.clearRect(0, 0, width, height);
        //Menggambar objek di atas canvas
        if (this.labirin != null) {
            int[][] grid = this.labirin.getGrid();
            //gambar gridnya
            //inisialisasi titik awal
            double s1 = height / (double) grid.length;
            double s2 = width / (double) grid[0].length;
            double size = Math.min(s1, s2);
            this.size = size;
            double s3 = size * grid.length;
            double s4 = size * grid[0].length;
            int gxo = (int) ((width - s4) / 2.0);
            int gyo = (int) ((height - s3) / 2.0);
            setSizeOfProgressBox(size, gxo, gyo);
            //buat warna latar Gray
            gc.setFill(Color.SILVER);
            gc.fillRect(gxo, gyo, s4, s3);

            //gambar cell-cell grid
            gc.beginPath();
            gc.setLineWidth(0.15);
            gc.setStroke(Color.rgb(236, 240, 241));
            for (int i = 0; i <= grid.length; i++) {
                int yt = (int) (gyo + i * size);
                gc.moveTo(gxo, yt);
                gc.lineTo(gxo + s4, yt);
            }
            for (int j = 0; j <= grid[0].length; j++) {
                int xt = (int) (gxo + j * size);
                gc.moveTo(xt, gyo);
                gc.lineTo(xt, gyo + s3);
            }
            gc.stroke();
            //gambar dinding penghalang
            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j < grid[i].length; j++) {
                    if (grid[i][j] == -1) {
                        gc.setFill(Color.rgb(52, 152, 219));
                        gc.fillRect(gxo + j * size, gyo + i * size, size, size);
                    }
                }
            }
            //gambar sel awal dan akhir
            gc.setFill(color1);
            Point start = labirin.getStart();
            Point end = labirin.getEnd();
            gc.fillRect(gxo + start.y * size, gyo + start.x * size, size, size);
            gc.fillRect(gxo + end.y * size, gyo + end.x * size, size, size);

            //gambar progressBox
            if (progressBox != null) {
                for (int i = 0; i < progressBox.length; i++) {
                    for (int j = 0; j < progressBox[i].length; j++) {
                        progressBox[i][j].draw(gc);
                    }
                }
            }

            //gambar lintasan terbaik (Shortest Path)
            if (lintasan != null && lintasan.length > 1 && head >= history.size()) {
                for (int i = 1; i < lintasan.length; i++) {
                    double x0 = gxo + size * lintasan[i - 1].y;
                    double y0 = gyo + size * lintasan[i - 1].x;
                    double x1 = gxo + size * lintasan[i].y;
                    double y1 = gyo + size * lintasan[i].x;

                    double setengahSize = size / 2.0;
                    double cx0 = x0 + setengahSize;
                    double cy0 = y0 + setengahSize;
                    double cx1 = x1 + setengahSize;
                    double cy1 = y1 + setengahSize;

                    //gc.setStroke(Color.rgb(52, 152, 219));
                    gc.setStroke(Color.WHITE);
                    gc.setLineWidth(size / 10.0);
                    gc.strokeLine(cx0, cy0, cx1, cy1);
                }
            }

        }
    }

    private void initializeProgressBox(int nBaris, int nKolom) {
        this.progressBox = new ProgressBox[nBaris][nKolom];
        for (int i = 0; i < this.progressBox.length; i++) {
            for (int j = 0; j < this.progressBox[i].length; j++) {
                this.progressBox[i][j] = new ProgressBox(i, j, T, color1);
            }
        }
    }

    private void setSizeOfProgressBox(double size, double gxo, double gyo) {
        if (!this.isSet && this.progressBox != null) {
            for (int i = 0; i < this.progressBox.length; i++) {
                for (int j = 0; j < this.progressBox[i].length; j++) {
                    this.progressBox[i][j].setSize(size, gxo, gyo);
                }
            }
        }
    }

    private void setSpeedOfProgressBox(double speed) {
        if (!this.isSet && this.progressBox != null) {
            for (int i = 0; i < this.progressBox.length; i++) {
                for (int j = 0; j < this.progressBox[i].length; j++) {
                    this.progressBox[i][j].setSpeed(speed);
                }
            }
        }
    }

    private void update() {
        if (T > 0) {
            t++;
            if (t > T && history != null && !history.isEmpty() && head < history.size() && progressBox != null) {
                t = 0;
                History h = history.get(head);
                Point ori = h.getOriginal();
                Point des = h.getDestination();
                Gerakan gr = h.getGerakan();
                Arah ar = h.getArah();
                if (gr == Gerakan.MAJU) {
                    int I = des.x, J = des.y;
                    System.out.println("TRACE[" + (I) + "," + (J) + "]");
                    progressBox[I][J].aktifkan(gr, ar);
                } else if (gr == Gerakan.MUNDUR) {
                    int I = ori.x, J = ori.y;
                    progressBox[I][J].aktifkan(gr, ar);
                }
                head++;
            }
        }

    }

    //main method
    public static void main(String[] args) {
        launch(args);
    }

    //private class & enum
    private enum Arah {
        UTARA, TIMUR, SELATAN, BARAT, NULL;

        public static Arah getArah(Point original, Point destination) {
            Arah arah = Arah.NULL;
            if (original.x == destination.x) {
                if (original.y > destination.y) {
                    arah = Arah.BARAT;
                } else if (original.y < destination.y) {
                    arah = Arah.TIMUR;
                }
            } else if (original.y == destination.y) {
                if (original.x < destination.x) {
                    arah = Arah.SELATAN;
                } else if (original.x > destination.x) {
                    arah = Arah.UTARA;
                }
            }
            return arah;
        }
    }

    private class Cloning {

        public int[][] clone(int[][] A) {
            int[][] result = null;
            if (A != null) {
                result = new int[A.length][A[0].length];
                for (int i = 0; i < A.length; i++) {
                    for (int j = 0; j < A[0].length; j++) {
                        result[i][j] = A[i][j];
                    }
                }
            }
            return result;
        }
    }

    private class DFS {

        //INPUT
        private Labirin labirin = null;

        //OUTPUT
        private Point[] lintasan = null;
        private ArrayList<History> history = null;

        public Point[] getLintasan() {
            return lintasan;
        }

        public ArrayList<History> getHistory() {
            return history;
        }

        public DFS(Labirin labirin) {
            super();
            this.labirin = labirin;
            this.run();
        }

        public void run() {
            if (this.labirin != null) {
                this.history = new ArrayList<History>();

                int[][] grid = new Cloning().clone(this.labirin.getGrid());
                Point start = this.labirin.getStart();
                Point end = this.labirin.getEnd();
                Stack<Point> path = new Stack<Point>();
                path.push(start);

                //Set Step 1
                grid[start.x][start.y] = 1;

                Arah arah = Arah.UTARA;
                Boolean next = true;
                while (next) {
                    Point center = path.peek();
                    int[] status = this.getStatusSelTetangga(center, grid);
                    int nextSel = -1;

                    //Menentukan arah berdasarkan prioritas aturan produksi: DEPAN | KANAN | KIRI | BELAKANG
                    if (status[arah.ordinal()] == 1) {
                        nextSel = arah.ordinal();//operasi MAJU
                    } else {
                        int kanan = (arah.ordinal() + 1) % 4;
                        while (kanan != arah.ordinal()) {
                            if (status[kanan] == 1) {
                                nextSel = kanan;
                                arah = hadapKanan(arah);//operasi Hadap Kanan
                            } else {
                                kanan = (kanan + 1) % 4;
                            }
                        }
                    }

                    //SET TRANSISI / MELANGKAH
                    if (nextSel != -1) {//ditemukan sel baru
                        Point selBaru = getTetangga(center, nextSel);
                        grid[selBaru.x][selBaru.y] = grid[center.x][center.y] + 1;;
                        path.push(selBaru);
                        this.history.add(new History(center, selBaru, Gerakan.MAJU, Arah.getArah(center, selBaru)));
                        if (selBaru.x == end.x && selBaru.y == end.y) {
                            //proses berhenti
                            next = false;
                        }
                    } else {//tidak ditemukan sel baru
                        path.pop();
                        Point prevSel = path.peek();
                        this.history.add(new History(center, prevSel, Gerakan.MUNDUR, Arah.getArah(center, prevSel)));
                        if (path.isEmpty()) {
                            next = false;
                        }
                    }

                }
                //set lintasan
                this.lintasan = new Point[path.size()];
                for (int i = 0; i < lintasan.length; i++) {
                    lintasan[i] = path.get(i);
                }
                cetakArray(lintasan);
                cetakArray(grid);
            }
        }

        private int[] getStatusSelTetangga(Point selPusat, int[][] grid) {
            int x = selPusat.x;
            int y = selPusat.y;
            int[] status = new int[4];
            //UTARA
            int xn = x - 1;
            int yn = y;
            if (xn >= 0 && xn < grid.length && yn >= 0 && yn < grid[0].length && grid[xn][yn] == 0) {
                status[0] = 1;
            }
            //TIMUR
            int xe = x;
            int ye = y + 1;
            if (xe >= 0 && xe < grid.length && ye >= 0 && ye < grid[0].length && grid[xe][ye] == 0) {
                status[1] = 1;
            }

            //SELATAN
            int xs = x + 1;
            int ys = y;
            if (xs >= 0 && xs < grid.length && ys >= 0 && ys < grid[0].length && grid[xs][ys] == 0) {
                status[2] = 1;
            }

            //BARAT
            int xw = x;
            int yw = y - 1;
            if (xw >= 0 && xw < grid.length && yw >= 0 && yw < grid[0].length && grid[xw][yw] == 0) {
                status[3] = 1;
            }
            return status;
        }

        private Point getTetangga(Point center, int nextNode) {
            if (nextNode == 0) {
                return new Point(center.x - 1, center.y);
            } else if (nextNode == 1) {
                return new Point(center.x, center.y + 1);
            } else if (nextNode == 2) {
                return new Point(center.x + 1, center.y);
            } else if (nextNode == 3) {
                return new Point(center.x, center.y - 1);
            } else {
                return null;
            }
        }

        private Arah hadapKanan(Arah oldArah) {
            Arah newArah = Arah.NULL;
            if (oldArah == Arah.UTARA) {
                newArah = Arah.TIMUR;
            } else if (oldArah == Arah.TIMUR) {
                newArah = Arah.SELATAN;
            } else if (oldArah == Arah.SELATAN) {
                newArah = Arah.BARAT;
            } else if (oldArah == Arah.BARAT) {
                newArah = Arah.UTARA;
            }
            return newArah;
        }

        public void cetakArray(Point[] arrPoint) {
            for (int i = 0; i < arrPoint.length; i++) {
                System.out.print("[" + arrPoint[i].x + ", " + arrPoint[i].y + "] ");
            }
            System.out.println();
        }

        public void cetakArray(int[][] grid) {
            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j < grid[i].length; j++) {
                    System.out.print(grid[i][j] + " ");
                }
                System.out.println();
            }
        }

    }

    private class GeneratorMaze {

        //atribut kelas
        private Labirin labirin = null;

        public Labirin getLabirin() {
            return labirin;
        }

        public void setLabirin(Labirin labirin) {
            this.labirin = labirin;
        }

        public GeneratorMaze(int baris, int kolom) {
            super();
            this.generate(baris, kolom);
        }

        private Labirin generate(int baris, int kolom) {
            int[][] grid = new int[2 * baris + 1][2 * kolom + 1];

            //INISIALISASI grid
            for (int i = 0; i < grid.length; i++) {
                if (i % 2 == 0) {
                    for (int j = 0; j < grid[i].length; j++) {
                        grid[i][j] = -1;
                    }
                } else {
                    for (int j = 0; j < grid[i].length; j += 2) {
                        grid[i][j] = -1;
                    }
                }
            }

            //TENTUKAN TITIK AWAL DAN TITIK AKHIR SECARA RANDOM
            int iAwal, iAkhir;
            iAwal = randomBetweenInt(1, grid.length - 2);
            while (iAwal % 2 == 0) {
                iAwal = randomBetweenInt(1, grid.length - 2);
            }
            iAkhir = randomBetweenInt(1, grid.length - 2);
            while (iAkhir % 2 == 0) {
                iAkhir = randomBetweenInt(1, grid.length - 2);
            }
            //tandai titik awal dan akhir dengan mengubah nilai elemen gridnya menjadi = 1
            //tandai titik awal
            grid[iAwal][0] = 1;
            //node[iAwal][1]=1;
            Point titikAwal = new Point(iAwal, 0);
            //tandai titik akhir
            grid[iAkhir][grid[iAkhir].length - 1] = 1;
            grid[iAkhir][grid[iAkhir].length - 2] = 1;
            Point titikAkhir = new Point(iAkhir, grid[iAkhir].length - 1);

            //MEMULAI PROSES PEMBANGKITAN MAZE
            //proses akan dimulai dari titik akhir (* ini opsional, boleh juga dari titik awal)
            //proses pembangkitan maze akan dilakukan menggunakan algoritma Backtracking
            //lintasan/path Backtracking akan disimpan ke dalam struktur data stack
            Stack<Point> path = new Stack<Point>();
            path.push(new Point(iAkhir, grid[iAkhir].length - 2));
            while (!path.isEmpty()) {
                //melakukan operasi peek untuk memanggil titik teratas di stack path
                //titik ini teratas ini kemudian ditetapkan sebagai center
                Point center = path.peek();
                int cx = center.x;
                int cy = center.y;

                //mengidentifikasi cell-cell tetangga
                ArrayList<Point> tetangga = this.getTetangga(cx, cy, grid);

                //jika titik center tidak memiliki tetangga maka keluarkan titik center dari stack path			
                if (tetangga.isEmpty()) {
                    path.pop();
                } else {
                    //jika titik center memiliki tetangga, maka lakukan operasi random untuk memilih tetangga yg akan menjadi titik center berikutnya
                    int index = randomBetweenInt(0, tetangga.size() - 1);
                    Point destination = tetangga.get(index);
                    //update grid untuk tetangga yang baru terpilih
                    grid[destination.x][destination.y] = 1;
                    //hapus pagar pembatas
                    if (destination.x == cx) {
                        if (destination.y < cy) {
                            grid[cx][cy - 1] = 1;
                        } else if (destination.y > cy) {
                            grid[cx][cy + 1] = 1;
                        }
                    } else if (destination.y == cy) {
                        if (destination.x < cx) {
                            grid[cx - 1][cy] = 1;
                        } else if (destination.x > cx) {
                            grid[cx + 1][cy] = 1;
                        }
                    }
                    //update path
                    path.push(destination);
                }
            }

            //replace nilai grid (ganti nilai 1 menjadi 0)
            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j < grid[i].length; j++) {
                    if (grid[i][j] == 1) {
                        grid[i][j] = 0;
                    }
                }
            }

            if (grid != null && titikAwal != null && titikAkhir != null) {
                this.labirin = new Labirin(grid, titikAwal, titikAkhir);
            }

            return this.labirin;
        }

        private ArrayList<Point> getTetangga(int cx, int cy, int[][] grid) {
            int nBaris = grid.length;
            int nKolom = grid[0].length;
            ArrayList<Point> tetangga = new ArrayList<Point>();
            //Nort
            int nx = cx - 2;
            int ny = cy;
            Point pn = new Point(nx, ny);
            if (isValid(pn, nBaris, nKolom) && valueAt(pn, grid) == 0) {
                tetangga.add(pn);
            }

            //East
            int ex = cx;
            int ey = cy + 2;
            Point pe = new Point(ex, ey);
            if (isValid(pe, nBaris, nKolom) && valueAt(pe, grid) == 0) {
                tetangga.add(pe);
            }

            //South
            int sx = cx + 2;
            int sy = cy;
            Point ps = new Point(sx, sy);
            if (isValid(ps, nBaris, nKolom) && valueAt(ps, grid) == 0) {
                tetangga.add(ps);
            }

            //West
            int wx = cx;
            int wy = cy - 2;
            Point pw = new Point(wx, wy);
            if (isValid(pw, nBaris, nKolom) && valueAt(pw, grid) == 0) {
                tetangga.add(pw);
            }
            return tetangga;
        }

        private boolean isValid(Point p, int nBaris, int nKolom) {
            boolean valid = false;
            if (p.x > 0 && p.y > 0 && p.x < nBaris - 1 && p.y < nKolom - 1) {
                valid = true;
            }
            return valid;
        }

        private int valueAt(Point p, int[][] grid) {
            if (isValid(p, grid.length, grid[0].length)) {
                return grid[p.x][p.y];
            } else {
                return -1;
            }
        }

        private int randomBetweenInt(int min, int max) {
            return (int) (min + Math.random() * (max - min + 1));
        }

    }

    private enum Gerakan {
        DIAM, MAJU, MUNDUR;
    }

    private class History {

        private Point original = null;
        private Point destination = null;
        private Gerakan gerakan = Gerakan.DIAM;
        private Arah arah = Arah.NULL;

        public History(Point original, Point destination, Gerakan gerakan, Arah arah) {
            super();
            this.original = original;
            this.destination = destination;
            this.gerakan = gerakan;
            this.arah = arah;
        }

        public Point getOriginal() {
            return original;
        }

        public void setOriginal(Point original) {
            this.original = original;
        }

        public Point getDestination() {
            return destination;
        }

        public void setDestination(Point destination) {
            this.destination = destination;
        }

        public Gerakan getGerakan() {
            return gerakan;
        }

        public void setGerakan(Gerakan gerakan) {
            this.gerakan = gerakan;
        }

        public Arah getArah() {
            return arah;
        }

        public void setArah(Arah arah) {
            this.arah = arah;
        }

    }

    private class Labirin {

        private int[][] grid = null;
        private Point start = null;
        private Point end = null;

        public Labirin(int[][] grid, Point startCell, Point endCell) {
            super();
            this.grid = grid;
            this.start = startCell;
            this.end = endCell;
        }

        public int[][] getGrid() {
            return grid;
        }

        public Point getStart() {
            return start;
        }

        public Point getEnd() {
            return end;
        }

        public void setStart(Point start) {
            this.start = start;
        }

        public void setEnd(Point end) {
            this.end = end;
        }

        public int getValueAt(Point at) {
            int value = -1;
            if (at != null && this.grid != null) {
                try {
                    value = this.grid[at.x][at.y];
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return value;
        }

        public boolean setValueAt(int value, Point at) {
            boolean result = false;
            if (at != null && this.grid != null) {
                try {
                    this.grid[at.x][at.y] = value;
                    result = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return result;
        }

        public Labirin clone() {
            Labirin l = null;
            if (this.grid != null && this.start != null && this.end != null) {
                int[][] n = this.grid.clone();
                Point s = new Point(this.start.x, this.start.y);
                Point e = new Point(this.end.x, this.end.y);
                l = new Labirin(n, s, e);
            }
            return l;
        }

        public void tampil() {
            if (grid != null) {
                for (int i = 0; i < grid.length; i++) {
                    for (int j = 0; j < grid[i].length; j++) {
                        if (i % 2 == 0) {
                            if (j % 2 == 0) {
                                System.out.print("+");
                            } else {
                                if (grid[i][j] == -1) {
                                    System.out.print("---");
                                } else {
                                    System.out.print("   ");
                                }
                            }
                        } else {
                            if (j % 2 == 0) {
                                if (grid[i][j] == -1) {
                                    System.out.print("|");
                                } else {
                                    System.out.print(" ");
                                }
                            } else {
                                System.out.print("   ");
                            }
                        }
                    }
                    System.out.println();
                }
            }
        }
    }

    private class ProgressBox {

        double size, size1, t, T, gxo, gyo, I, J;
        Color color;
        Gerakan gerakan = Gerakan.DIAM;
        Arah arah = Arah.NULL;

        public ProgressBox(int I, int J, double size, double gxo, double gyo, double T, Color color) {
            super();
            this.size = size;
            this.T = T;
            this.color = color;
            this.I = I;
            this.J = J;
            this.gxo = gxo;
            this.gyo = gyo;
        }

        public ProgressBox(int I, int J, double T, Color color) {
            super();
            this.T = T;
            this.color = color;
            this.I = I;
            this.J = J;
        }

        public void setSpeed(double speed) {
            T = speed;
        }

        public void setSize(double size, double gxo, double gyo) {
            this.size = size;
            this.gxo = gxo;
            this.gyo = gyo;
        }

        public void aktifkan(Gerakan gerakan, Arah arah) {
            this.gerakan = gerakan;
            this.arah = arah;
            t = 0;
            size1 = 0;
        }

        private void update() {
            if (gerakan != Gerakan.DIAM && t < T) {
                t++;
                size1 = size * (t / T);
                if (size1 > size) {
                    size1 = size;
                }
            }
        }

        public void draw(GraphicsContext gc) {
            if (gerakan != Gerakan.DIAM) {
                gc.setFill(color);
                double xo = gxo + J * size, yo = gyo + I * size, sizeX = 0, sizeY = 0;
                if (gerakan == Gerakan.MAJU) {
                    if (arah == Arah.UTARA) {
                        yo += (size - size1);
                        sizeX = size;
                        sizeY = size1;
                    } else if (arah == Arah.TIMUR) {
                        sizeX = size1;
                        sizeY = size;
                    } else if (arah == Arah.SELATAN) {
                        sizeX = size;
                        sizeY = size1;
                    } else if (arah == Arah.BARAT) {
                        xo += (size - size1);
                        sizeX = size1;
                        sizeY = size;
                    }
                } else if (gerakan == Gerakan.MUNDUR) {
                    if (arah == Arah.UTARA) {
                        sizeX = size;
                        sizeY = size - size1;
                    } else if (arah == Arah.TIMUR) {
                        xo += (size1);
                        sizeX = size - size1;
                        sizeY = size;
                    } else if (arah == Arah.SELATAN) {
                        yo += (size1);
                        sizeX = size;
                        sizeY = size - size1;
                    } else if (arah == Arah.BARAT) {
                        sizeX = size - size1;
                        sizeY = size;
                    }
                }
                gc.fillRect(xo, yo, sizeX, sizeY);
                update();
            }
        }

    }

}
